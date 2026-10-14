import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

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

    private static final ConcurrentHashMap<String, LongAdder> topRutas =
        new ConcurrentHashMap<>();

    private static final boolean LOG_DETALLADO = false;

    public static String registrarSolicitud(String ip) {

        String id = String.valueOf(
            consecutivo.incrementAndGet()
        );

        Solicitud solicitud = new Solicitud(id, ip);

        solicitudes.put(id, solicitud);
        cola.add(solicitud);
        recibidas.incrementAndGet();

        if (LOG_DETALLADO) {
            System.out.println(
                "[Solicitud #" + id + "] Encolada"
            );
        }

        return id;
    }

    public static void rechazarSolicitud(String id) {

        Solicitud solicitud = solicitudes.remove(id);

        if (solicitud != null) {
            cola.remove(solicitud);
        }

        rechazadas.incrementAndGet();
    }

    public static void actualizarRuta(String id, String ruta) {

        Solicitud solicitud = solicitudes.get(id);

        String rutaLimpia = Rutas.rutaEstadistica(ruta);

        if (solicitud != null) {
            solicitud.ruta = rutaLimpia;
        }

        topRutas.computeIfAbsent(
            rutaLimpia,
            clave -> new LongAdder()
        ).increment();
    }

    public static void iniciarSolicitud(String id) {

        Solicitud solicitud = solicitudes.get(id);

        if (solicitud != null) {
            cola.remove(solicitud);
        }

        activas.incrementAndGet();
    }

    public static void finalizarSolicitud(
        String id,
        String ip,
        String ruta,
        String hilo
    ) {

        activas.updateAndGet(valor -> Math.max(0, valor - 1));

        procesadas.incrementAndGet();

        solicitudes.remove(id);
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

    public static String getTopRutasHtml() {

        List<Map.Entry<String, LongAdder>> lista =
            new ArrayList<>(topRutas.entrySet());

        lista.sort(
            Comparator.comparingLong(
                (Map.Entry<String, LongAdder> entrada) ->
                    entrada.getValue().sum()
            ).reversed()
        );

        StringBuilder html = new StringBuilder();

        int limite = Math.min(10, lista.size());

        for (int i = 0; i < limite; i++) {

            Map.Entry<String, LongAdder> entrada = lista.get(i);

            html.append("""
                <div class='ruta-estadistica'>
                    <span>%s</span>
                    <strong>%d</strong>
                </div>
                """.formatted(
                    escaparHtml(entrada.getKey()),
                    entrada.getValue().sum()
                ));
        }

        if (html.isEmpty()) {
            return "<p>No hay estadísticas disponibles.</p>";
        }

        return html.toString();
    }

    public static String getColaHtml() {

        if (cola.isEmpty()) {
            return "<p class='vacio'>No hay solicitudes en cola.</p>";
        }

        StringBuilder html = new StringBuilder();

        int cantidad = 0;

        for (Solicitud solicitud : cola) {

            html.append(
                "<div class='cola-item'>" +
                "<b>#" + escaparHtml(solicitud.id) + "</b> " +
                "GET " + escaparHtml(solicitud.ruta) +
                "<span>" + escaparHtml(solicitud.ip) + "</span>" +
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

    private static String escaparHtml(String texto) {

        if (texto == null) {
            return "";
        }

        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static class Solicitud {

        final String id;
        final String ip;
        volatile String ruta = "pendiente";

        Solicitud(String id, String ip) {
            this.id = id;
            this.ip = ip;
        }
    }
}