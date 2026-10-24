import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class LimitadorSolicitudes {

    private static final int MAX_PAGINAS_POR_VENTANA = 120;
    private static final int MAX_IMAGENES_POR_VENTANA = 600;
    private static final int MAX_SOLICITUDES_TOTALES_POR_VENTANA = 900;
    private static final int MAX_IPS_REGISTRADAS = 10_000;

    public static final int UMBRAL_RECHAZOS_BLOQUEO = 60;
    public static final long VENTANA_RECHAZOS_SEGUNDOS = 60;
    public static final long DURACION_BLOQUEO_SEGUNDOS = 120;

    private static final long VENTANA_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final long INACTIVIDAD_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final long INTERVALO_LIMPIEZA_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long VENTANA_RECHAZOS_NANOS = TimeUnit.SECONDS.toNanos(VENTANA_RECHAZOS_SEGUNDOS);
    private static final long DURACION_BLOQUEO_NANOS = TimeUnit.SECONDS.toNanos(DURACION_BLOQUEO_SEGUNDOS);

    private static final ConcurrentHashMap<String, LimitesCliente> clientes =
        new ConcurrentHashMap<>();
    private static final Object BLOQUEO_NUEVAS_IPS = new Object();
    private static final AtomicLong BLOQUEOS_INICIADOS = new AtomicLong();
    private static volatile long ultimaLimpiezaNanos =
        System.nanoTime() - INTERVALO_LIMPIEZA_NANOS;

    private LimitadorSolicitudes() {
    }

    public record Resultado(
        boolean permitido,
        long retryAfterSegundos,
        boolean sinCapacidad,
        boolean ipBloqueada
    ) {
        public Resultado(boolean permitido, long retryAfterSegundos) {
            this(permitido, retryAfterSegundos, false, false);
        }

        public Resultado(boolean permitido, long retryAfterSegundos, boolean sinCapacidad) {
            this(permitido, retryAfterSegundos, sinCapacidad, false);
        }
    }

    public static Resultado comprobarGlobal(String ip) {
        String clave = normalizar(ip);
        LimitesCliente cliente = obtenerCliente(clave);
        if (cliente == null) {
            return new Resultado(false, 60, true);
        }

        long restante = cliente.rechazos.restanteNanos(System.nanoTime());
        if (restante > 0) {
            return new Resultado(false, aSegundos(restante), false, true);
        }

        Resultado resultado = cliente.global.consumir();
        if (!resultado.permitido()) {
            registrarRechazo(clave, cliente);
        }
        return resultado;
    }

    public static Resultado comprobar(String ip) {
        return comprobar(ip, false);
    }

    public static Resultado comprobar(String ip, boolean recursoImagen) {
        String clave = normalizar(ip);
        LimitesCliente cliente = obtenerCliente(clave);
        if (cliente == null) {
            return new Resultado(false, 60, true);
        }

        Resultado resultado = recursoImagen
            ? cliente.imagenes.consumir()
            : cliente.paginas.consumir();
        if (!resultado.permitido()) {
            registrarRechazo(clave, cliente);
        }
        return resultado;
    }

    public static int ipsBloqueadasAhora() {
        long ahora = System.nanoTime();
        int total = 0;
        for (LimitesCliente cliente : clientes.values()) {
            if (cliente.rechazos.restanteNanos(ahora) > 0) {
                total++;
            }
        }
        return total;
    }

    public static int ipsSeguidas() {
        return clientes.size();
    }

    static boolean esLoopback(String ip) {
        return ip.startsWith("127.")
            || ip.equals("::1")
            || ip.equals("0:0:0:0:0:0:0:1");
    }

    private static void registrarRechazo(String ip, LimitesCliente cliente) {
        if (esLoopback(ip)) {
            return;
        }

        boolean iniciado = cliente.rechazos.registrar(
            System.nanoTime(),
            UMBRAL_RECHAZOS_BLOQUEO,
            VENTANA_RECHAZOS_NANOS,
            DURACION_BLOQUEO_NANOS
        );

        if (iniciado) {
            Estadisticas.registrarEvento(
                Estadisticas.TipoEvento.BLOQUEO_TEMPORAL,
                ip,
                UMBRAL_RECHAZOS_BLOQUEO + " rechazos en " + VENTANA_RECHAZOS_SEGUNDOS +
                    " s: bloqueada " + (DURACION_BLOQUEO_SEGUNDOS / 60) + " min"
            );
            long total = BLOQUEOS_INICIADOS.incrementAndGet();
            if (total == 1 || total % 100 == 0) {
                System.out.println(
                    "[BLOQUEO] IPs bloqueadas temporalmente: " + total + " | última: " + ip
                );
            }
        }
    }

    private static String normalizar(String ipOriginal) {
        return (ipOriginal == null || ipOriginal.isBlank())
            ? "desconocida"
            : ipOriginal.trim();
    }

    private static long aSegundos(long nanos) {
        long segundo = TimeUnit.SECONDS.toNanos(1);
        return Math.max(1L, (nanos + segundo - 1L) / segundo);
    }

    private static LimitesCliente obtenerCliente(String ip) {
        while (true) {
            long ahora = System.nanoTime();
            LimitesCliente existente = clientes.get(ip);
            if (existente != null) {
                if (existente.tocar(ahora) && clientes.get(ip) == existente) {
                    return existente;
                }
                clientes.remove(ip, existente);
                continue;
            }

            synchronized (BLOQUEO_NUEVAS_IPS) {
                existente = clientes.get(ip);
                if (existente != null) {
                    if (existente.tocar(ahora)) {
                        return existente;
                    }
                    clientes.remove(ip, existente);
                }

                if (clientes.size() >= MAX_IPS_REGISTRADAS) {
                    if (ahora - ultimaLimpiezaNanos >= INTERVALO_LIMPIEZA_NANOS) {
                        limpiarInactivos(ahora);
                    }
                    if (clientes.size() >= MAX_IPS_REGISTRADAS) {
                        return null;
                    }
                }

                LimitesCliente nuevo = new LimitesCliente(ahora);
                clientes.put(ip, nuevo);
                return nuevo;
            }
        }
    }

    private static void limpiarInactivos(long ahora) {
        Iterator<Map.Entry<String, LimitesCliente>> iterador =
            clientes.entrySet().iterator();

        while (iterador.hasNext()) {
            Map.Entry<String, LimitesCliente> entrada = iterador.next();
            LimitesCliente cliente = entrada.getValue();
            if (cliente.rechazos.restanteNanos(ahora) > 0) {
                continue;
            }
            if (cliente.expirarSiInactivo(ahora, INACTIVIDAD_NANOS)) {
                clientes.remove(entrada.getKey(), cliente);
            }
        }
        ultimaLimpiezaNanos = ahora;
    }

    static final class Rechazos {
        private int cantidad;
        private long inicioVentana;
        private boolean bloqueado;
        private long bloqueadoHasta;

        synchronized boolean registrar(long ahora, int umbral, long ventana, long duracion) {
            if (restanteNanos(ahora) > 0) {
                return false;
            }

            if (cantidad == 0 || ahora - inicioVentana > ventana) {
                inicioVentana = ahora;
                cantidad = 0;
            }

            cantidad++;
            if (cantidad >= umbral) {
                bloqueado = true;
                bloqueadoHasta = ahora + duracion;
                cantidad = 0;
                return true;
            }
            return false;
        }

        synchronized long restanteNanos(long ahora) {
            if (!bloqueado) {
                return 0;
            }
            long restante = bloqueadoHasta - ahora;
            if (restante <= 0) {
                bloqueado = false;
                return 0;
            }
            return restante;
        }
    }

    private static final class LimitesCliente {
        private final LimitadorAtomico global = new LimitadorAtomico(
            MAX_SOLICITUDES_TOTALES_POR_VENTANA, VENTANA_NANOS);
        private final LimitadorAtomico paginas = new LimitadorAtomico(
            MAX_PAGINAS_POR_VENTANA, VENTANA_NANOS);
        private final LimitadorAtomico imagenes = new LimitadorAtomico(
            MAX_IMAGENES_POR_VENTANA, VENTANA_NANOS);
        private final Rechazos rechazos = new Rechazos();

        private final AtomicLong estadoActividad;

        private LimitesCliente(long ahora) {
            estadoActividad = new AtomicLong(ahora & ~1L);
        }

        private boolean tocar(long ahora) {
            long nuevoTiempo = ahora & ~1L;
            while (true) {
                long estado = estadoActividad.get();
                if ((estado & 1L) != 0L) {
                    return false;
                }

                long anterior = estado & ~1L;
                if (ahora - anterior <= 0L) {
                    return true;
                }

                if (estadoActividad.compareAndSet(estado, nuevoTiempo)) {
                    return true;
                }
            }
        }

        private boolean expirarSiInactivo(long ahora, long tiempoInactividad) {
            while (true) {
                long estado = estadoActividad.get();
                if ((estado & 1L) != 0L) {
                    return true;
                }

                long ultimaActividad = estado & ~1L;
                if (ahora - ultimaActividad < tiempoInactividad) {
                    return false;
                }

                if (estadoActividad.compareAndSet(estado, estado | 1L)) {
                    return true;
                }
            }
        }
    }

    private static final class LimitadorAtomico {
        private final long intervaloTokenNanos;
        private final long toleranciaRafagaNanos;
        private final AtomicLong llegadaTeoricaNanos = new AtomicLong(System.nanoTime());

        private LimitadorAtomico(int capacidad, long ventanaNanos) {
            this.intervaloTokenNanos = Math.max(
                1L,
                (long) Math.ceil(ventanaNanos / (double) capacidad)
            );
            this.toleranciaRafagaNanos = (capacidad - 1L) * intervaloTokenNanos;
        }

        private Resultado consumir() {
            while (true) {
                long ahora = System.nanoTime();
                long llegadaAnterior = llegadaTeoricaNanos.get();
                long instantePermitido = llegadaAnterior - toleranciaRafagaNanos;

                if (ahora - instantePermitido < 0L) {
                    long esperaNanos = instantePermitido - ahora;
                    return new Resultado(false, aSegundos(esperaNanos));
                }

                long base = Math.max(llegadaAnterior, ahora);
                long siguiente = base + intervaloTokenNanos;
                if (llegadaTeoricaNanos.compareAndSet(llegadaAnterior, siguiente)) {
                    return new Resultado(true, 0);
                }
            }
        }
    }
}