
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class LimitadorSolicitudes {

    // Ventanas independientes: las imágenes de una página no consumen
    // el mismo cupo que la navegación HTML.
    private static final int MAX_PAGINAS_POR_VENTANA = 120;
    private static final int MAX_IMAGENES_POR_VENTANA = 600;

    private static final long VENTANA_NANOS =
        TimeUnit.SECONDS.toNanos(60);

    private static final long INACTIVIDAD_NANOS =
        TimeUnit.MINUTES.toNanos(5);

    private static final int MAX_CLAVES_REGISTRADAS = 20_000;

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

    // Compatibilidad para llamadas existentes: limita solicitudes de páginas.
    public static Resultado comprobar(String ip) {
        return comprobar(ip, false);
    }

    /**
     * Comprueba el límite de navegación o el límite de recursos estáticos.
     * Las imágenes tienen un cupo separado para que una carga normal del
     * catálogo no agote el límite de las páginas.
     */
    public static synchronized Resultado comprobar(
        String ip,
        boolean recursoImagen
    ) {
        String direccion = (ip == null || ip.isBlank())
            ? "desconocida"
            : ip.trim();

        String clave = direccion + (recursoImagen ? "\u0000IMG" : "\u0000WEB");
        int limiteSolicitudes = recursoImagen
            ? MAX_IMAGENES_POR_VENTANA
            : MAX_PAGINAS_POR_VENTANA;

        long ahora = System.nanoTime();
        comprobaciones++;

        if (comprobaciones % 256 == 0 ||
            ahora - ultimaLimpiezaNanos >= TimeUnit.SECONDS.toNanos(30)) {
            limpiarVentanasInactivas(ahora);
        }

        VentanaIP ventana = ventanas.get(clave);

        if (ventana == null) {
            if (ventanas.size() >= MAX_CLAVES_REGISTRADAS) {
                limpiarVentanasInactivas(ahora);
                if (ventanas.size() >= MAX_CLAVES_REGISTRADAS) {
                    return new Resultado(false, 60);
                }
            }

            ventana = new VentanaIP();
            ventanas.put(clave, ventana);
        }

        ventana.ultimaActividadNanos = ahora;

        while (!ventana.marcasDeTiempo.isEmpty() &&
               ahora - ventana.marcasDeTiempo.peekFirst() >= VENTANA_NANOS) {
            ventana.marcasDeTiempo.removeFirst();
        }

        if (ventana.marcasDeTiempo.size() >= limiteSolicitudes) {
            long transcurrido = ahora - ventana.marcasDeTiempo.peekFirst();
            long restante = Math.max(1L, VENTANA_NANOS - transcurrido);
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
