import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PruebaEstadisticas {

    static int fallos = 0;

    static String enviar(String crudo) throws Exception {
        try (Socket s = new Socket("127.0.0.1", 8080)) {
            s.setSoTimeout(10000);
            OutputStream o = s.getOutputStream();
            o.write(crudo.getBytes(StandardCharsets.ISO_8859_1));
            o.flush();
            InputStream in = s.getInputStream();
            byte[] buf = in.readAllBytes();
            return buf.length >= 12 ? new String(buf, StandardCharsets.ISO_8859_1).substring(9, 12) : "SIN";
        }
    }

    static String get(String ruta, String extra) throws Exception {
        return enviar("GET " + ruta + " HTTP/1.1\r\nHost: x\r\n" + extra + "\r\n");
    }

    static void esperar(String nombre, long real, long esperado) {
        boolean ok = real == esperado;
        if (!ok) fallos++;
        System.out.printf("%-45s esperado=%-5d real=%-5d %s%n", nombre, esperado, real, ok ? "OK" : "FALLO");
    }

    public static void main(String[] args) throws Exception {
        Thread servidor = new Thread(() -> Servidor.main(new String[0]));
        servidor.setDaemon(true);
        servidor.start();
        Thread.sleep(1500);

        for (int i = 0; i < 5; i++) get("/", "");
        for (int i = 0; i < 3; i++) get("/noexiste", "");
        for (int i = 0; i < 2; i++) get("/hilos", "");
        get("/" + "a".repeat(5000), "");
        get("/", "X-A: " + "a".repeat(5000) + "\r\n");
        enviar("POST / HTTP/1.1\r\nHost: x\r\n\r\n");
        enviar("BASURA\r\nHost: x\r\n\r\n");
        get("/imagenes/Mousepad.png", "");
        get("/imagenes/noexiste.png", "");

        int ok200 = 0, n429 = 0;
        for (int i = 0; i < 125; i++) {
            String c = get("/productos", "CF-Connecting-IP: 203.0.113.9\r\n");
            if (c.equals("200")) ok200++;
            if (c.equals("429")) n429++;
        }
        System.out.println("rafaga /productos misma IP: 200=" + ok200 + " 429=" + n429);

        new Socket("127.0.0.1", 8080).close();
        Thread.sleep(300);

        List<Socket> inactivos = new ArrayList<>();
        int n503 = 0;
        for (int i = 0; i < 340; i++) inactivos.add(new Socket("127.0.0.1", 8080));
        Thread.sleep(500);
        for (Socket s : inactivos) {
            s.setSoTimeout(300);
            try {
                byte[] b = s.getInputStream().readNBytes(300);
                if (b.length >= 12 && new String(b, StandardCharsets.ISO_8859_1).contains("503")) n503++;
            } catch (Exception ignored) {
            }
        }
        for (Socket s : inactivos) s.close();
        System.out.println("saturacion: 503 recibidos por el cliente=" + n503);

        Estadisticas.Instantanea e;
        long inicio = System.currentTimeMillis();
        do {
            Thread.sleep(200);
            e = Estadisticas.instantanea();
        } while ((e.activas() > 0 || e.enCola() > 0) && System.currentTimeMillis() - inicio < 30000);

        System.out.println();
        System.out.println("recibidas=" + e.recibidas() + " atendidas=" + e.atendidas() + " aceptadas=" + e.aceptadas()
            + " rechazadasPorCapacidad=" + e.rechazadasPorCapacidad() + " bloqueadasPorLimitador=" + e.bloqueadasPorLimitador()
            + " activas=" + e.activas() + " enCola=" + e.enCola());
        System.out.println("codigos=" + e.respuestasPorCodigo());
        System.out.println("eventos=" + e.eventosPorTipo());
        System.out.println("sinRespuesta=" + e.sinRespuesta());
        System.out.println();

        Map<Integer, Long> c = e.respuestasPorCodigo();
        esperar("200 (5 / + 1 imagen + rafaga)", c.getOrDefault(200, 0L), 5 + 1 + ok200);
        esperar("404 (3 noexiste + 2 hilos + 1 imagen)", c.getOrDefault(404, 0L), 6);
        esperar("405 (POST)", c.getOrDefault(405, 0L), 1);
        esperar("400 (BASURA)", c.getOrDefault(400, 0L), 1);
        esperar("414", c.getOrDefault(414, 0L), 1);
        esperar("431", c.getOrDefault(431, 0L), 1);
        esperar("429", c.getOrDefault(429, 0L), n429);
        esperar("503", c.getOrDefault(503, 0L), n503);
        esperar("evento LIMITE_PAGINAS", e.eventosPorTipo().get(Estadisticas.TipoEvento.LIMITE_PAGINAS), n429);
        esperar("evento RUTA_PROTEGIDA", e.eventosPorTipo().get(Estadisticas.TipoEvento.RUTA_PROTEGIDA), 2);
        esperar("evento SOLICITUD_INVALIDA (414+431+400)", e.eventosPorTipo().get(Estadisticas.TipoEvento.SOLICITUD_INVALIDA), 3);
        esperar("evento SATURACION", e.eventosPorTipo().get(Estadisticas.TipoEvento.SATURACION), n503);
        esperar("rechazadasPorCapacidad", e.rechazadasPorCapacidad(), n503);
        esperar("bloqueadasPorLimitador", e.bloqueadasPorLimitador(), n429);

        long totalRespuestas = c.values().stream().mapToLong(Long::longValue).sum();
        long totalSin = e.sinRespuesta().values().stream().mapToLong(Long::longValue).sum();
        esperar("INVARIANTE recibidas = respuestas + sin respuesta", e.recibidas(), totalRespuestas + totalSin);

        System.out.println();
        System.out.println("eventos recientes (mas nuevo primero):");
        for (Estadisticas.EventoReciente ev : Estadisticas.getEventosRecientes(10)) {
            System.out.println("  " + ev.tipo() + " ip=" + ev.ip() + " detalle='" + ev.detalle() + "' x" + ev.repeticiones());
        }
        System.out.println("top rutas: " + Estadisticas.getTopRutas(5));
        System.out.println();
        System.out.println(fallos == 0 ? "RESULTADO: TODO OK" : "RESULTADO: " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}