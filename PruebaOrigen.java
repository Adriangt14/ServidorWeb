import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PruebaOrigen {
    static int fallos = 0;
    static final String SECRETO = "prueba_0123456789abcdefghijklmnopqrstuvwxyz";

    static String get(String ruta, String cabeceras) throws Exception {
        try (Socket s = new Socket("127.0.0.1", 8080)) {
            OutputStream o = s.getOutputStream();
            o.write(("GET " + ruta + " HTTP/1.1\r\nHost: x\r\n" + cabeceras + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            o.flush();
            byte[] r = s.getInputStream().readAllBytes();
            return new String(r, 0, Math.min(12, r.length), StandardCharsets.ISO_8859_1).substring(9);
        }
    }

    static int[] rafaga(int n, java.util.function.IntFunction<String> cabeceras) throws Exception {
        int ok = 0, bloq = 0;
        for (int i = 0; i < n; i++) {
            String c = get("/servicios", cabeceras.apply(i));
            if (c.equals("200")) ok++;
            if (c.equals("429")) bloq++;
        }
        return new int[] {ok, bloq};
    }

    static void esperar(String nombre, boolean ok, String detalle) {
        if (!ok) fallos++;
        System.out.printf("%-58s %s  %s%n", nombre, ok ? "OK   " : "FALLO", detalle);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("nexus.secreto.origen", SECRETO);
        Thread t = new Thread(() -> Servidor.main(new String[0])); t.setDaemon(true); t.start();
        Thread.sleep(1500);

        esperar("verificacion activa", OrigenCliente.verificacionActiva(), "");

        int[] falsas = rafaga(125, i -> "CF-Connecting-IP: 198.51.100." + (i % 200 + 1) + "\r\n");
        esperar("125 IP falsas SIN secreto comparten el limite de 127.0.0.1",
            falsas[1] > 0 && falsas[0] <= 122, "200=" + falsas[0] + " 429=" + falsas[1]);

        int[] secretoMalo = rafaga(3, i -> "CF-Connecting-IP: 203.0.113.77\r\nX-Nexus-Origen: " + SECRETO + "x\r\n");
        esperar("secreto incorrecto tampoco se acepta (sigue bloqueado)", secretoMalo[0] == 0, "200=" + secretoMalo[0] + " 429=" + secretoMalo[1]);

        int[] buenaA = rafaga(125, i -> "CF-Connecting-IP: 203.0.113.10\r\nX-Nexus-Origen: " + SECRETO + "\r\n");
        esperar("IP verificada A: limite propio (~121 permitidas)",
            buenaA[0] >= 119 && buenaA[0] <= 122 && buenaA[1] > 0, "200=" + buenaA[0] + " 429=" + buenaA[1]);

        int[] buenaB = rafaga(5, i -> "CF-Connecting-IP: 203.0.113.11\r\nX-Nexus-Origen: " + SECRETO + "\r\n");
        esperar("IP verificada B: no afectada por A", buenaB[0] == 5, "200=" + buenaB[0]);

        Thread.sleep(200);
        var e = Estadisticas.instantanea();
        long rechazados = e.eventosPorTipo().get(Estadisticas.TipoEvento.ORIGEN_NO_VERIFICADO);
        esperar("eventos ORIGEN_NO_VERIFICADO = 125 + 3", rechazados == 128, "real=" + rechazados);

        boolean ipFalsaRegistrada = Estadisticas.getEventosRecientes(100).stream()
            .anyMatch(ev -> ev.ip().startsWith("198.51.100."));
        esperar("ninguna IP falsa aparece en los eventos", !ipFalsaRegistrada, "");

        boolean ipVerificadaEnLimite = Estadisticas.getEventosRecientes(100).stream()
            .anyMatch(ev -> ev.tipo() == Estadisticas.TipoEvento.LIMITE_PAGINAS && ev.ip().equals("203.0.113.10"));
        esperar("el 429 de A se registra con su IP real verificada", ipVerificadaEnLimite, "");

        System.out.println(fallos == 0 ? "RESULTADO: TODO OK" : "RESULTADO: " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}