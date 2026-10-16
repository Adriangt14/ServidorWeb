
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LimitadorSolicitudes {

    private static final int MAX_SOLICITUDES_POR_VENTANA = 120;

    private static final long VENTANA_NANOS =
        TimeUnit.SECONDS.toNanos(60);

    private static final long INACTIVIDAD_NANOS =
        TimeUnit.MINUTES.toNanos(5);

    private static final int MAX_IPS_REGISTRADAS = 10_000;

    private static final Map<String, VentanaIP> ventanas =
        new HashMap<>();

    private static long comprobaciones;
    private static long ultimaLimpiezaNanos = System.nanoTime();

    private LimitadorSolicitudes() {
    }

    public record Resultado(
        boolean permitido,
        long retryAfterSegundos
    ) {
    }

    public static synchronized Resultado comprobar(String ip) {

        String clave = (ip == null || ip.isBlank())
            ? "desconocida"
            : ip;

        long ahora = System.nanoTime();
        comprobaciones++;

        if (comprobaciones % 256 == 0 ||
            ahora - ultimaLimpiezaNanos >= TimeUnit.SECONDS.toNanos(30)) {

            limpiarVentanasInactivas(ahora);
        }

        VentanaIP ventana = ventanas.get(clave);

        if (ventana == null) {

            if (ventanas.size() >= MAX_IPS_REGISTRADAS) {
                limpiarVentanasInactivas(ahora);

                if (ventanas.size() >= MAX_IPS_REGISTRADAS) {
                    return new Resultado(false, 60);
                }
            }

            ventana = new VentanaIP();
            ventanas.put(clave, ventana);
        }

        ventana.ultimaActividadNanos = ahora;

        while (
            !ventana.marcasDeTiempo.isEmpty() &&
            ahora - ventana.marcasDeTiempo.peekFirst() >= VENTANA_NANOS
        ) {
            ventana.marcasDeTiempo.removeFirst();
        }

        if (ventana.marcasDeTiempo.size() >= MAX_SOLICITUDES_POR_VENTANA) {

            long transcurrido =
                ahora - ventana.marcasDeTiempo.peekFirst();

            long restante =
                Math.max(1L, VENTANA_NANOS - transcurrido);

            long segundos = Math.max(
                1L,
                (restante + TimeUnit.SECONDS.toNanos(1) - 1)
                    / TimeUnit.SECONDS.toNanos(1)
            );

            return new Resultado(false, segundos);
        }

        ventana.marcasDeTiempo.addLast(ahora);

        return new Resultado(true, 0);
    }

    private static void limpiarVentanasInactivas(long ahora) {

        Iterator<Map.Entry<String, VentanaIP>> iterador =
            ventanas.entrySet().iterator();

        while (iterador.hasNext()) {

            Map.Entry<String, VentanaIP> entrada =
                iterador.next();

            if (
                ahora - entrada.getValue().ultimaActividadNanos
                    >= INACTIVIDAD_NANOS
            ) {
                iterador.remove();
            }
        }

        ultimaLimpiezaNanos = ahora;
    }

    private static final class VentanaIP {

        private final Deque<Long> marcasDeTiempo =
            new ArrayDeque<>();

        private long ultimaActividadNanos =
            System.nanoTime();
    }
}
