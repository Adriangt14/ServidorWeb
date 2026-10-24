import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PruebaBloqueo {

    static int fallos = 0;

    record Respuesta(int codigo, String cabeceras, String cuerpo) {
        String cabecera(String nombre) {
            for (String linea : cabeceras.split("\r\n")) {
                int dos = linea.indexOf(':');
                if (dos > 0 && linea.substring(0, dos).equalsIgnoreCase(nombre)) return linea.substring(dos + 1).trim();
            }
            return null;
        }
    }

    static Respuesta enviar(String crudo) throws Exception {
        try (Socket s = new Socket("127.0.0.1", 8080)) {
            s.setSoTimeout(15000);
            OutputStream o = s.getOutputStream();
            o.write(crudo.getBytes(StandardCharsets.UTF_8));
            o.flush();
            String r = new String(s.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int fin = r.indexOf("\r\n\r\n");
            return new Respuesta(Integer.parseInt(r.substring(9, 12)), r.substring(0, fin), r.substring(fin + 4));
        }
    }

    static Respuesta get(String ruta, String extra) throws Exception {
        return enviar("GET " + ruta + " HTTP/1.1\r\nHost: 127.0.0.1:8080\r\n" + extra + "\r\n");
    }

    static String ip(String ip) {
        return "CF-Connecting-IP: " + ip + "\r\n";
    }

    static void caso(String nombre, boolean ok, String detalle) {
        if (!ok) fallos++;
        System.out.printf("%-64s %s %s%n", nombre, ok ? "OK   " : "FALLO", detalle);
    }

    public static void main(String[] args) throws Exception {
        long s = TimeUnit.SECONDS.toNanos(1);
        LimitadorSolicitudes.Rechazos r = new LimitadorSolicitudes.Rechazos();
        long t = 1_000_000_000_000L;
        boolean antes = false;
        for (int i = 0; i < 59; i++) antes |= r.registrar(t + i * s / 2, 60, 60 * s, 120 * s);
        boolean umbral = r.registrar(t + 30 * s, 60, 60 * s, 120 * s);
        caso("unidad: 59 rechazos no bloquean, el 60.º sí", !antes && umbral, "");
        caso("unidad: bloqueo dura 120 s", r.restanteNanos(t + 30 * s) == 120 * s && r.restanteNanos(t + 149 * s) > 0 && r.restanteNanos(t + 150 * s) == 0, "");
        caso("unidad: durante el bloqueo no se reinicia ni alarga", !r.registrar(t + 31 * s, 60, 60 * s, 120 * s), "");

        LimitadorSolicitudes.Rechazos lento = new LimitadorSolicitudes.Rechazos();
        boolean bloqueoLento = false;
        for (int i = 0; i < 200; i++) bloqueoLento |= lento.registrar(t + i * 2 * s, 60, 60 * s, 120 * s);
        caso("unidad: 1 rechazo cada 2 s (30/min) nunca bloquea", !bloqueoLento, "");

        caso("unidad: loopback exento", LimitadorSolicitudes.esLoopback("127.0.0.1") && LimitadorSolicitudes.esLoopback("::1") && !LimitadorSolicitudes.esLoopback("203.0.113.5"), "");

        Path carpeta = Files.createTempDirectory("nexus-bloqueo");
        Path archivo = carpeta.resolve("admins.properties");
        System.setProperty("nexus.admins.archivo", archivo.toString());
        AdminAuth.guardarUsuario(archivo, "adrian", "clave-de-prueba-segura".toCharArray());

        Thread hilo = new Thread(() -> Servidor.main(new String[0]));
        hilo.setDaemon(true);
        hilo.start();
        Thread.sleep(2500);

        String atacante = "198.51.100.50";
        int ok = 0, frenadas = 0, primeraBloqueada = -1;
        long retryBloqueo = -1;
        for (int i = 0; i < 260; i++) {
            Respuesta x = get("/servicios", ip(atacante));
            if (x.codigo() == 200) ok++;
            if (x.codigo() == 429) {
                frenadas++;
                long retry = Long.parseLong(x.cabecera("Retry-After"));
                if (retry > 60 && primeraBloqueada < 0) {
                    primeraBloqueada = i;
                    retryBloqueo = retry;
                }
            }
        }
        caso("ráfaga: las primeras ~120 páginas se sirven", ok >= 119 && ok <= 123, "200=" + ok);
        caso("ráfaga: bloqueo tras 60 rechazos (respuesta ~181)", primeraBloqueada >= 175 && primeraBloqueada <= 186, "primera bloqueada=#" + primeraBloqueada);
        caso("bloqueada: Retry-After ~120 s", retryBloqueo >= 110 && retryBloqueo <= 120, "Retry-After=" + retryBloqueo);

        Respuesta img = get("/imagenes/Mousepad.png", ip(atacante));
        caso("bloqueada en todo el sitio (también imágenes)", img.codigo() == 429, "" + img.codigo());

        Respuesta otra = get("/servicios", ip("198.51.100.51"));
        caso("otra IP no se ve afectada", otra.codigo() == 200, "" + otra.codigo());

        caso("IPs bloqueadas ahora = 1", LimitadorSolicitudes.ipsBloqueadasAhora() == 1, "" + LimitadorSolicitudes.ipsBloqueadasAhora());

        int localFrenadas = 0;
        long maxRetryLocal = 0;
        for (int i = 0; i < 200; i++) {
            Respuesta x = get("/servicios", "");
            if (x.codigo() == 429) {
                localFrenadas++;
                maxRetryLocal = Math.max(maxRetryLocal, Long.parseLong(x.cabecera("Retry-After")));
            }
        }
        caso("local 127.0.0.1: se frena pero nunca se bloquea", localFrenadas >= 60 && maxRetryLocal <= 2 && LimitadorSolicitudes.ipsBloqueadasAhora() == 1, "429=" + localFrenadas + " maxRetry=" + maxRetryLocal);

        get("/.%3Cscript%3Ealert(1)%3C/script%3E", ip("203.0.113.70"));

        var e = Estadisticas.instantanea();
        long inicios = e.eventosPorTipo().get(Estadisticas.TipoEvento.BLOQUEO_TEMPORAL);
        long durante = e.eventosPorTipo().get(Estadisticas.TipoEvento.IP_BLOQUEADA);
        caso("evento BLOQUEO_TEMPORAL registrado una vez", inicios == 1, "" + inicios);
        caso("solicitudes durante el bloqueo contadas (IP_BLOQUEADA)", durante == 260 - primeraBloqueada + 1, "" + durante);
        caso("frenadas (429) incluye las bloqueadas", e.bloqueadasPorLimitador() == e.respuestasPorCodigo().getOrDefault(429, 0L), e.bloqueadasPorLimitador() + " vs " + e.respuestasPorCodigo().get(429));

        String sesionIp = "203.0.113.80";
        byte[] cuerpo = "usuario=adrian&clave=clave-de-prueba-segura".getBytes(StandardCharsets.UTF_8);
        Respuesta login = enviar("POST /admin/entrar HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nOrigin: http://127.0.0.1:8080\r\n" + ip(sesionIp) +
            "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " + cuerpo.length + "\r\n\r\nusuario=adrian&clave=clave-de-prueba-segura");
        Matcher m = Pattern.compile("nexus_sesion=([^;]*)").matcher(String.valueOf(login.cabecera("Set-Cookie")));
        String token = m.find() ? m.group(1) : "";
        Respuesta panel = get("/admin", ip(sesionIp) + "Cookie: nexus_sesion=" + token + "\r\n");
        String html = panel.cuerpo();
        caso("panel 200 con sesión", panel.codigo() == 200, "" + panel.codigo());
        caso("panel: recarga automática 15 s", html.contains("<meta http-equiv=\"refresh\" content=\"15\">"), "");
        caso("panel: cifras (recibidas, IPs bloqueadas, sesiones)", html.contains("Recibidas") && html.contains("IPs bloqueadas ahora") && html.contains("Sesiones admin"), "");
        caso("panel: muestra el bloqueo y la IP", html.contains("IP bloqueada temporalmente") && html.contains(atacante), "");
        caso("panel: códigos 200 y 429 en la tabla", html.contains("200 OK") && html.contains("429 Demasiadas solicitudes"), "");
        caso("panel: rutas más visitadas incluye /servicios", html.contains("<span class=\"nombre\">/servicios</span>"), "");
        caso("panel: HTML de un atacante se muestra escapado", !html.contains("<script>") && html.contains("&lt;script&gt;"), "");
        caso("panel: conserva usuario y CSRF para cerrar sesión", html.contains("<strong>adrian</strong>") && html.matches("(?s).*name=\"csrf\" value=\"[A-Za-z0-9_-]{43}\".*"), "");
        caso("panel: sin scripts (CSP intacta)", !html.contains("<script") && "same-origin".equals(panel.cabecera("Referrer-Policy")), "");

        Respuesta sinSesion = get("/admin", ip("203.0.113.81"));
        caso("panel sin sesión sigue redirigiendo al formulario", sinSesion.codigo() == 303, "" + sinSesion.codigo());
        caso("panel: fondo de la portada y vista de pantalla completa", html.contains("/imagenes/encabezado.jpeg") && html.contains("class=\"app\""), "");
        Respuesta formulario = get("/admin/entrar", ip("203.0.113.82"));
        caso("formulario de acceso con el mismo fondo", formulario.codigo() == 200 && formulario.cuerpo().contains("/imagenes/encabezado.jpeg") && formulario.cuerpo().contains("name=\"clave\""), "" + formulario.codigo());

        Files.writeString(Path.of(System.getProperty("java.io.tmpdir"), "panel_prueba.html"), html);
        System.out.println(fallos == 0 ? "RESULTADO: TODO OK" : "RESULTADO: " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}