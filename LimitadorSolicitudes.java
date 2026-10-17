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

    private static final long VENTANA_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final long INACTIVIDAD_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final long INTERVALO_LIMPIEZA_NANOS = TimeUnit.SECONDS.toNanos(5);

    private static final ConcurrentHashMap<String, LimitesCliente> clientes =
        new ConcurrentHashMap<>();
    private static final Object BLOQUEO_NUEVAS_IPS = new Object();
    private static volatile long ultimaLimpiezaNanos =
        System.nanoTime() - INTERVALO_LIMPIEZA_NANOS;

    private LimitadorSolicitudes() {
    }

    public record Resultado(boolean permitido, long retryAfterSegundos) {
    }

    public static Resultado comprobarGlobal(String ip) {
        LimitesCliente cliente = obtenerCliente(ip);
        return cliente == null
            ? new Resultado(false, 60)
            : cliente.global.consumir();
    }

    public static Resultado comprobar(String ip) {
        return comprobar(ip, false);
    }

    public static Resultado comprobar(String ip, boolean recursoImagen) {
        LimitesCliente cliente = obtenerCliente(ip);
        if (cliente == null) {
            return new Resultado(false, 60);
        }
        return recursoImagen
            ? cliente.imagenes.consumir()
            : cliente.paginas.consumir();
    }

    private static LimitesCliente obtenerCliente(String ipOriginal) {
        String ip = (ipOriginal == null || ipOriginal.isBlank())
            ? "desconocida"
            : ipOriginal.trim();

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
            if (cliente.expirarSiInactivo(ahora, INACTIVIDAD_NANOS)) {
                clientes.remove(entrada.getKey(), cliente);
            }
        }
        ultimaLimpiezaNanos = ahora;
    }

    private static final class LimitesCliente {
        private final LimitadorAtomico global = new LimitadorAtomico(
            MAX_SOLICITUDES_TOTALES_POR_VENTANA, VENTANA_NANOS);
        private final LimitadorAtomico paginas = new LimitadorAtomico(
            MAX_PAGINAS_POR_VENTANA, VENTANA_NANOS);
        private final LimitadorAtomico imagenes = new LimitadorAtomico(
            MAX_IMAGENES_POR_VENTANA, VENTANA_NANOS);

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
                    long retry = Math.max(
                        1L,
                        (esperaNanos + TimeUnit.SECONDS.toNanos(1) - 1L)
                            / TimeUnit.SECONDS.toNanos(1)
                    );
                    return new Resultado(false, retry);
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