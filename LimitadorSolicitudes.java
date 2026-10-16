import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class LimitadorSolicitudes {

    private static final int MAX_PAGINAS_POR_VENTANA = 120;
    private static final int MAX_IMAGENES_POR_VENTANA = 600;
    private static final int MAX_SOLICITUDES_TOTALES_POR_VENTANA = 900;
    private static final int MAX_IPS_REGISTRADAS = 10_000;

    private static final long VENTANA_NANOS =
        TimeUnit.SECONDS.toNanos(60);
    private static final long INACTIVIDAD_NANOS =
        TimeUnit.MINUTES.toNanos(5);
    private static final long INTERVALO_LIMPIEZA_NANOS =
        TimeUnit.SECONDS.toNanos(5);

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
        if (cliente == null) {
            return new Resultado(false, 60);
        }
        return cliente.global.consumir();
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
                existente.tocar(ahora);
                if (clientes.get(ip) == existente) {
                    return existente;
                }
                continue;
            }

            synchronized (BLOQUEO_NUEVAS_IPS) {
                existente = clientes.get(ip);
                if (existente != null) {
                    existente.tocar(ahora);
                    return existente;
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

            synchronized (cliente) {
                if (ahora - cliente.ultimaActividadNanos >= INACTIVIDAD_NANOS) {
                    clientes.remove(entrada.getKey(), cliente);
                }
            }
        }
        ultimaLimpiezaNanos = ahora;
    }

    private static final class LimitesCliente {
        private final TokenBucket global = new TokenBucket(
            MAX_SOLICITUDES_TOTALES_POR_VENTANA, VENTANA_NANOS);
        private final TokenBucket paginas = new TokenBucket(
            MAX_PAGINAS_POR_VENTANA, VENTANA_NANOS);
        private final TokenBucket imagenes = new TokenBucket(
            MAX_IMAGENES_POR_VENTANA, VENTANA_NANOS);
        private volatile long ultimaActividadNanos;

        private LimitesCliente(long ahora) {
            ultimaActividadNanos = ahora;
        }

        private synchronized void tocar(long ahora) {
            ultimaActividadNanos = ahora;
        }
    }

    private static final class TokenBucket {
        private final double capacidad;
        private final double tokensPorNano;
        private double tokens;
        private long ultimoRefillNanos;

        private TokenBucket(int capacidad, long ventanaNanos) {
            this.capacidad = capacidad;
            this.tokensPorNano = capacidad / (double) ventanaNanos;
            this.tokens = capacidad;
            this.ultimoRefillNanos = System.nanoTime();
        }

        private synchronized Resultado consumir() {
            long ahora = System.nanoTime();
            long transcurrido = Math.max(0L, ahora - ultimoRefillNanos);
            tokens = Math.min(capacidad, tokens + transcurrido * tokensPorNano);
            ultimoRefillNanos = ahora;

            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return new Resultado(true, 0);
            }

            double nanosNecesarios = (1.0d - tokens) / tokensPorNano;
            long segundos = Math.max(
                1L,
                (long) Math.ceil(nanosNecesarios / TimeUnit.SECONDS.toNanos(1))
            );
            return new Resultado(false, segundos);
        }
    }
}
