import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Estadisticas {

    private static final AtomicInteger recibidas =
        new AtomicInteger();

    private static final AtomicInteger procesadas =
        new AtomicInteger();

    private static final AtomicInteger rechazadas =
        new AtomicInteger();

    private static final AtomicInteger activas =
        new AtomicInteger();

    private static final AtomicInteger consecutivo =
        new AtomicInteger();

    private static final ConcurrentLinkedQueue<Solicitud> cola =
        new ConcurrentLinkedQueue<>();

    private static final ConcurrentHashMap<String, Solicitud> solicitudes =
        new ConcurrentHashMap<>();

    private static final boolean LOG_DETALLADO = false;

    public static String registrarSolicitud(String ip) {

        String id =
            String.valueOf(
                consecutivo.incrementAndGet()
            );

        Solicitud solicitud =
            new Solicitud(id, ip);

        solicitudes.put(id, solicitud);
        cola.add(solicitud);
        recibidas.incrementAndGet();

        if (LOG_DETALLADO) {
            System.out.println(
                "[Solicitud #" + id +
                "] Encolada | Cola: " +
                cola.size()
            );
        }

        return id;
    }

    public static void rechazarSolicitud(
        String id
    ) {

        Solicitud solicitud =
            solicitudes.remove(id);

        if (solicitud != null) {
            cola.remove(solicitud);
        }

        rechazadas.incrementAndGet();

        if (LOG_DETALLADO) {
            System.out.println(
                "[Solicitud #" + id +
                "] Rechazada por cola llena"
            );
        }
    }

    public static void actualizarRuta(
        String id,
        String ruta
    ) {

        Solicitud solicitud =
            solicitudes.get(id);

        if (solicitud != null) {
            solicitud.ruta = ruta;
        }
    }

    public static void iniciarSolicitud(
        String id
    ) {

        Solicitud solicitud =
            solicitudes.get(id);

        if (solicitud != null) {
            cola.remove(solicitud);
        }

        activas.incrementAndGet();

        if (LOG_DETALLADO) {
            System.out.println(
                "[Solicitud #" + id +
                "] Procesando | Cola: " +
                cola.size()
            );
        }
    }

    public static void finalizarSolicitud(
        String id,
        String ip,
        String ruta,
        String hilo
    ) {

        activas.decrementAndGet();
        procesadas.incrementAndGet();
        solicitudes.remove(id);

        if (LOG_DETALLADO) {
            System.out.println(
                "[Solicitud #" + id +
                "] Completada | Procesadas: " +
                procesadas.get() +
                " | Cola: " +
                cola.size()
            );
        }
    }

    public static int getRecibidas() {
        return recibidas.get();
    }

    public static int getProcesadas() {
        return procesadas.get();
    }

    public static int getRechazadas() {
        return rechazadas.get();
    }

    public static int getActivas() {
        return activas.get();
    }

    public static int getEnCola() {
        return cola.size();
    }

    public static String getColaHtml() {

        if (cola.isEmpty()) {
            return "<p class='vacio'>" +
                   "No hay solicitudes en cola." +
                   "</p>";
        }

        StringBuilder html =
            new StringBuilder();

        int cantidad = 0;

        for (Solicitud solicitud : cola) {

            html.append(
                "<div class='cola-item'>" +
                "<b>#" +
                solicitud.id +
                "</b> GET " +
                solicitud.ruta +
                "<span>" +
                solicitud.ip +
                "</span>" +
                "</div>"
            );

            cantidad++;

            if (cantidad == 10) {
                break;
            }
        }

        int totalCola = cola.size();

        if (totalCola > 10) {
            html.append(
                "<p class='vacio'>+ " +
                (totalCola - 10) +
                " solicitudes más...</p>"
            );
        }

        return html.toString();
    }

    private static class Solicitud {

        String id;
        String ip;
        String ruta = "pendiente";

        Solicitud(
            String id,
            String ip
        ) {
            this.id = id;
            this.ip = ip;
        }
    }
}