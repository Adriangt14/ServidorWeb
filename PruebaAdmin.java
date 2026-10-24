import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PruebaAdmin {

    static int fallos = 0;
    static final String ORIGEN = "Origin: http://127.0.0.1:8080\r\nHost: 127.0.0.1:8080\r\n";

    record Respuesta(int codigo, String cabeceras, String cuerpo) {
        String cabecera(String nombre) {
            for (String linea : cabeceras.split("\r\n")) {
                int dos = linea.indexOf(':');
                if (dos > 0 && linea.substring(0, dos).equalsIgnoreCase(nombre)) return linea.substring(dos + 1).trim();
            }
            return null;
        }
        String cookieSesion() {
            String c = cabecera("Set-Cookie");
            if (c == null) return null;
            Matcher m = Pattern.compile("nexus_sesion=([^;]*)").matcher(c);
            return m.find() ? m.group(1) : null;
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

    static Respuesta post(String ruta, String cabeceras, String cuerpo) throws Exception {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        return enviar("POST " + ruta + " HTTP/1.1\r\n" + cabeceras +
            "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: " + b.length + "\r\n\r\n" + cuerpo);
    }

    static String ip(String ip) {
        return "CF-Connecting-IP: " + ip + "\r\n";
    }

    static void caso(String nombre, boolean ok, String detalle) {
        if (!ok) fallos++;
        System.out.printf("%-62s %s %s%n", nombre, ok ? "OK   " : "FALLO", detalle);
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("nexus-admin");
        Path archivo = carpeta.resolve("admins.properties");
        System.setProperty("nexus.admins.archivo", archivo.toString());

        try {
            AdminAuth.guardarUsuario(archivo, "adrian", "corta".toCharArray());
            caso("contraseña corta rechazada al crear usuario", false, "");
        } catch (IllegalArgumentException e) {
            caso("contraseña corta rechazada al crear usuario", true, "");
        }

        AdminAuth.guardarUsuario(archivo, "adrian", "clave-de-prueba-segura".toCharArray());
        String contenido = Files.readString(archivo);
        caso("archivo con hash PBKDF2, sin la contraseña en texto", contenido.startsWith("adrian=pbkdf2-sha256$600000$") && !contenido.contains("clave-de-prueba"), "");
        caso("permisos del archivo 600", Files.getPosixFilePermissions(archivo).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")), Files.getPosixFilePermissions(archivo).toString());

        Thread t = new Thread(() -> Servidor.main(new String[0]));
        t.setDaemon(true);
        t.start();
        Thread.sleep(2500);

        Respuesta r = get("/admin", "");
        caso("GET /admin sin sesión -> 303 a /admin/entrar", r.codigo() == 303 && "/admin/entrar".equals(r.cabecera("Location")), "" + r.codigo());

        r = get("/admin/entrar", "");
        caso("GET /admin/entrar -> 200 con formulario y no-store", r.codigo() == 200 && r.cuerpo().contains("name=\"clave\"") && "no-store".equals(r.cabecera("Cache-Control")), "" + r.codigo());
        caso("cabecera X-Robots-Tag noindex", r.cabecera("X-Robots-Tag") != null && r.cabecera("X-Robots-Tag").contains("noindex"), "");

        r = get("/admin/otra", "");
        caso("ruta interna sin sesión -> 303 (no revela nada)", r.codigo() == 303, "" + r.codigo());

        r = post("/admin/entrar", "Host: 127.0.0.1:8080\r\n" + ip("198.51.100.1"), "usuario=adrian&clave=clave-de-prueba-segura");
        caso("POST sin Origin -> 403", r.codigo() == 403, "" + r.codigo());

        r = post("/admin/entrar", "Origin: https://evil.example\r\nHost: 127.0.0.1:8080\r\n" + ip("198.51.100.1"), "usuario=adrian&clave=clave-de-prueba-segura");
        caso("POST con Origin de otro sitio -> 403", r.codigo() == 403, "" + r.codigo());

        r = enviar("POST /admin/entrar HTTP/1.1\r\n" + ORIGEN + ip("198.51.100.1") + "Content-Type: text/plain\r\nContent-Length: 3\r\n\r\nabc");
        caso("Content-Type incorrecto -> 415", r.codigo() == 415, "" + r.codigo());

        r = enviar("POST /admin/entrar HTTP/1.1\r\n" + ORIGEN + ip("198.51.100.1") + "Content-Type: application/x-www-form-urlencoded\r\nContent-Length: 9000\r\n\r\n");
        caso("cuerpo de 9000 bytes -> 413", r.codigo() == 413, "" + r.codigo());

        r = post("/admin/entrar", ORIGEN + ip("198.51.100.1"), "usuario=adrian&clave=%zz");
        caso("formulario mal codificado -> 400", r.codigo() == 400, "" + r.codigo());

        long t0 = System.nanoTime();
        r = post("/admin/entrar", ORIGEN + ip("198.51.100.2"), "usuario=adrian&clave=incorrecta-123");
        long msExiste = (System.nanoTime() - t0) / 1_000_000;
        caso("contraseña incorrecta -> 401 mensaje genérico", r.codigo() == 401 && r.cuerpo().contains("Usuario o contraseña incorrectos"), "" + r.codigo());
        caso("sin cookie tras fallo", r.cabecera("Set-Cookie") == null, "");

        t0 = System.nanoTime();
        r = post("/admin/entrar", ORIGEN + ip("198.51.100.3"), "usuario=noexiste&clave=incorrecta-123");
        long msNoExiste = (System.nanoTime() - t0) / 1_000_000;
        caso("usuario inexistente -> mismo 401 y mismo mensaje", r.codigo() == 401 && r.cuerpo().contains("Usuario o contraseña incorrectos"), "");
        caso("tiempo similar (no revela si el usuario existe)", Math.abs(msExiste - msNoExiste) < 250, msExiste + " ms vs " + msNoExiste + " ms");

        r = post("/admin/entrar", ORIGEN + ip("198.51.100.4"), "usuario=<script>&clave=x");
        caso("usuario con HTML se escapa en la página", !r.cuerpo().contains("<script>") && r.cuerpo().contains("&lt;script&gt;"), "");

        int codigosQuinto = 0;
        for (int i = 0; i < 5; i++) {
            r = post("/admin/entrar", ORIGEN + ip("198.51.100.9"), "usuario=adrian&clave=mala-" + i);
            codigosQuinto = r.codigo();
        }
        caso("5 fallos desde una IP -> el 5º ya avisa del bloqueo", codigosQuinto == 401 && r.cuerpo().contains("Demasiados intentos"), "" + codigosQuinto);
        r = post("/admin/entrar", ORIGEN + ip("198.51.100.9"), "usuario=adrian&clave=clave-de-prueba-segura");
        caso("IP bloqueada: ni la contraseña correcta entra -> 429", r.codigo() == 429 && r.cabecera("Retry-After") != null && r.cookieSesion() == null, r.codigo() + " Retry-After=" + r.cabecera("Retry-After"));

        r = post("/admin/entrar", ORIGEN + ip("203.0.113.20"), "usuario=ADRIAN&clave=clave-de-prueba-segura");
        String cookie = r.cabecera("Set-Cookie");
        String token = r.cookieSesion();
        caso("login correcto (usuario sin distinguir mayúsculas) -> 303 a /admin", r.codigo() == 303 && "/admin".equals(r.cabecera("Location")), "" + r.codigo());
        caso("cookie HttpOnly; SameSite=Strict; Path=/admin; Max-Age=28800", cookie != null && cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict") && cookie.contains("Path=/admin") && cookie.contains("Max-Age=28800"), String.valueOf(cookie));
        caso("sin Secure en acceso local HTTP", cookie != null && !cookie.contains("Secure"), "");
        caso("token de 43 caracteres", token != null && token.length() == 43, "");

        r = get("/admin", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("GET /admin con sesión -> 200 y muestra el usuario", r.codigo() == 200 && r.cuerpo().contains("<strong>adrian</strong>"), "" + r.codigo());
        Matcher m = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(r.cuerpo());
        String csrf = m.find() ? m.group(1) : null;
        caso("el panel incluye token CSRF para cerrar sesión", csrf != null && csrf.length() == 43, "");

        r = get("/admin/entrar", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("con sesión, /admin/entrar redirige al panel", r.codigo() == 303 && "/admin".equals(r.cabecera("Location")), "" + r.codigo());

        r = get("/admin/otra", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("con sesión, ruta inexistente del panel -> 404", r.codigo() == 404, "" + r.codigo());

        r = get("/admin", "Cookie: nexus_sesion=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n");
        caso("cookie inventada -> 303 (no entra)", r.codigo() == 303, "" + r.codigo());

        r = enviar("HEAD /admin HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nCookie: nexus_sesion=" + token + "\r\n\r\n");
        caso("HEAD /admin con sesión -> 200 sin cuerpo", r.codigo() == 200 && r.cuerpo().isEmpty(), "");

        r = get("/admin/salir", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("GET /admin/salir -> 405 (solo POST)", r.codigo() == 405 && "POST".equals(r.cabecera("Allow")), "" + r.codigo());

        r = post("/admin/salir", ORIGEN + "Cookie: nexus_sesion=" + token + "\r\n", "csrf=incorrecto");
        caso("cerrar sesión con CSRF incorrecto -> 403", r.codigo() == 403, "" + r.codigo());
        r = get("/admin", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("la sesión sigue activa tras el intento con CSRF falso", r.codigo() == 200, "" + r.codigo());

        r = post("/admin/salir", ORIGEN + "Cookie: nexus_sesion=" + token + "\r\n", "csrf=" + csrf);
        String borrada = r.cabecera("Set-Cookie");
        caso("cerrar sesión correcto -> 303 y borra la cookie", r.codigo() == 303 && borrada != null && borrada.contains("Max-Age=0"), "" + r.codigo());
        r = get("/admin", "Cookie: nexus_sesion=" + token + "\r\n");
        caso("la cookie antigua ya no sirve", r.codigo() == 303, "" + r.codigo());

        r = post("/admin/entrar", ORIGEN + ip("203.0.113.21") + "X-Forwarded-Proto: https\r\n", "usuario=adrian&clave=clave-de-prueba-segura");
        caso("login vía HTTPS (túnel) -> cookie con Secure", r.codigo() == 303 && r.cabecera("Set-Cookie").contains("; Secure"), "");

        r = post("/admin/entrar", "Origin: https://nexusumg.app\r\nHost: localhost:8080\r\n" + ip("203.0.113.22"), "usuario=adrian&clave=clave-de-prueba-segura");
        caso("Origin https://nexusumg.app aceptado aunque Host sea distinto", r.codigo() == 303, "" + r.codigo());

        r = get("/", "");
        caso("el sitio público sigue funcionando", r.codigo() == 200, "" + r.codigo());
        r = post("/", "Host: 127.0.0.1:8080\r\n", "a=b");
        caso("POST al sitio público sigue siendo 405", r.codigo() == 405, "" + r.codigo());

        SesionesAdmin corta = new SesionesAdmin(300, 10_000, 3);
        String tk = corta.crear("ana");
        caso("sesión válida recién creada", corta.obtener(tk) != null, "");
        Thread.sleep(450);
        caso("sesión expira por inactividad", corta.obtener(tk) == null, "");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) tokens.add(corta.crear("u" + i));
        caso("máximo de sesiones respetado (3)", corta.activas() == 3 && corta.obtener(tokens.get(0)) == null && corta.obtener(tokens.get(4)) != null, "activas=" + corta.activas());

        var e = Estadisticas.instantanea().eventosPorTipo();
        caso("eventos registrados (éxito, fallo, bloqueo, rechazo)",
            e.get(Estadisticas.TipoEvento.LOGIN_EXITOSO) == 3 &&
            e.get(Estadisticas.TipoEvento.LOGIN_FALLIDO) >= 7 &&
            e.get(Estadisticas.TipoEvento.LOGIN_BLOQUEADO) >= 1 &&
            e.get(Estadisticas.TipoEvento.ACCESO_ADMIN_RECHAZADO) >= 3,
            e.toString().replaceAll(".*LOGIN", "LOGIN"));
        boolean claveFiltrada = Estadisticas.getEventosRecientes(100).stream().anyMatch(ev -> ev.detalle().contains("clave-de-prueba") || ev.detalle().contains("mala-"));
        caso("ninguna contraseña aparece en los eventos", !claveFiltrada, "");

        System.out.println(fallos == 0 ? "RESULTADO: TODO OK" : "RESULTADO: " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}