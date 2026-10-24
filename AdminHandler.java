import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AdminHandler {

    public static final String NOMBRE_COOKIE = "nexus_sesion";
    public static final String PROPIEDAD_ORIGENES = "nexus.admin.origenes";
    public static final String VARIABLE_ORIGENES = "NEXUS_ADMIN_ORIGENES";

    private static final String RUTA_PANEL = Rutas.ADMIN;
    private static final String RUTA_ENTRAR = Rutas.ADMIN_ENTRAR;
    private static final String RUTA_SALIR = Rutas.ADMIN_SALIR;
    private static final int MAX_CAMPOS_FORMULARIO = 10;

    private static final SesionesAdmin SESIONES = new SesionesAdmin();
    private static final Set<String> ORIGENES_PERMITIDOS = cargarOrigenes();

    private AdminHandler() {
    }

    public static SesionesAdmin sesiones() {
        return SESIONES;
    }

    public static void atender(
        Socket socket,
        String id,
        SolicitudHttp solicitud,
        OrigenCliente.Origen origen,
        boolean incluirCuerpo
    ) throws IOException {

        String ip = origen.ip();

        try {
            solicitud.interpretarRuta();
        } catch (SolicitudHttp.SolicitudInvalidaException e) {
            ManejadorCliente.responderSolicitudInvalida(socket, ip, e, incluirCuerpo);
            return;
        }

        String ruta = solicitud.ruta();

        LimitadorSolicitudes.Resultado limite = LimitadorSolicitudes.comprobar(ip, false);
        if (!limite.permitido()) {
            Estadisticas.registrarEvento(
                limite.sinCapacidad()
                    ? Estadisticas.TipoEvento.CAPACIDAD_IPS
                    : Estadisticas.TipoEvento.LIMITE_PAGINAS,
                ip,
                Rutas.ADMIN
            );
            ManejadorCliente.enviarLimiteSolicitudes(socket, limite.retryAfterSegundos(), incluirCuerpo);
            return;
        }

        Estadisticas.actualizarRuta(id, ruta);

        String metodo = solicitud.metodo();
        boolean lectura = metodo.equals("GET") || metodo.equals("HEAD");
        String token = solicitud.cookie(NOMBRE_COOKIE);
        SesionesAdmin.Sesion sesion = SESIONES.obtener(token);

        if (ruta.equals(RUTA_PANEL)) {
            if (!lectura) {
                metodoNoPermitido(socket, "GET, HEAD");
                return;
            }
            if (sesion == null) {
                redirigir(socket, RUTA_ENTRAR, null, incluirCuerpo);
                return;
            }
            html(200, paginaPanel(sesion)).escribir(socket, incluirCuerpo);
            return;
        }

        if (ruta.equals(RUTA_ENTRAR)) {
            if (lectura) {
                if (sesion != null) {
                    redirigir(socket, RUTA_PANEL, null, incluirCuerpo);
                    return;
                }
                html(200, paginaEntrar("", null)).escribir(socket, incluirCuerpo);
                return;
            }
            if (metodo.equals("POST")) {
                procesarEntrada(socket, solicitud, ip);
                return;
            }
            metodoNoPermitido(socket, "GET, HEAD, POST");
            return;
        }

        if (ruta.equals(RUTA_SALIR)) {
            if (!metodo.equals("POST")) {
                metodoNoPermitido(socket, "POST");
                return;
            }
            procesarSalida(socket, solicitud, ip, token, sesion);
            return;
        }

        if (sesion == null) {
            redirigir(socket, RUTA_ENTRAR, null, incluirCuerpo);
            return;
        }

        html(404, ManejadorCliente.paginaError(404, "Página no encontrada", "Esta sección del panel no existe."))
            .escribir(socket, incluirCuerpo);
    }

    private static void procesarEntrada(Socket socket, SolicitudHttp solicitud, String ip) throws IOException {
        if (!origenPermitido(solicitud)) {
            rechazarOrigen(socket, ip, "inicio de sesión con Origin no permitido");
            return;
        }

        long espera = AdminAuth.segundosBloqueo(ip);
        if (espera > 0) {
            Estadisticas.registrarEvento(Estadisticas.TipoEvento.LOGIN_BLOQUEADO, ip, "intento durante el bloqueo");
            html(429, paginaEntrar("", "Demasiados intentos fallidos. Espera " + minutos(espera) + " antes de volver a intentarlo."))
                .cabecera("Retry-After", Long.toString(espera))
                .escribir(socket, true);
            return;
        }

        Map<String, String> campos = leerFormulario(socket, solicitud, ip);
        if (campos == null) {
            return;
        }

        String usuario = campos.getOrDefault("usuario", "").trim().toLowerCase(Locale.ROOT);
        String clave = campos.getOrDefault("clave", "");

        AdminAuth.Verificacion resultado = AdminAuth.verificar(usuario, clave, ip);

        if (resultado == AdminAuth.Verificacion.OCUPADO) {
            html(503, paginaEntrar(usuario, "El servidor está ocupado verificando otros accesos. Intenta de nuevo en unos segundos."))
                .cabecera("Retry-After", "2")
                .escribir(socket, true);
            return;
        }

        if (resultado == AdminAuth.Verificacion.INCORRECTA) {
            Estadisticas.registrarEvento(Estadisticas.TipoEvento.LOGIN_FALLIDO, ip, "credenciales incorrectas");
            long bloqueo = AdminAuth.segundosBloqueo(ip);
            String mensaje = bloqueo > 0
                ? "Demasiados intentos fallidos. Espera " + minutos(bloqueo) + " antes de volver a intentarlo."
                : "Usuario o contraseña incorrectos.";
            html(401, paginaEntrar(usuario, mensaje)).escribir(socket, true);
            return;
        }

        String nuevoToken = SESIONES.crear(usuario);
        Estadisticas.registrarEvento(Estadisticas.TipoEvento.LOGIN_EXITOSO, ip, usuario);
        redirigir(socket, RUTA_PANEL, cookieSesion(nuevoToken, esHttps(solicitud)), true);
    }

    private static void procesarSalida(
        Socket socket,
        SolicitudHttp solicitud,
        String ip,
        String token,
        SesionesAdmin.Sesion sesion
    ) throws IOException {
        if (!origenPermitido(solicitud)) {
            rechazarOrigen(socket, ip, "cierre de sesión con Origin no permitido");
            return;
        }

        if (sesion == null) {
            redirigir(socket, RUTA_ENTRAR, cookieBorrada(esHttps(solicitud)), true);
            return;
        }

        Map<String, String> campos = leerFormulario(socket, solicitud, ip);
        if (campos == null) {
            return;
        }

        if (!SesionesAdmin.tokensIguales(sesion.tokenCsrf(), campos.get("csrf"))) {
            rechazarOrigen(socket, ip, "token CSRF no válido");
            return;
        }

        SESIONES.cerrar(token);
        redirigir(socket, RUTA_ENTRAR, cookieBorrada(esHttps(solicitud)), true);
    }

    private static Map<String, String> leerFormulario(
        Socket socket,
        SolicitudHttp solicitud,
        String ip
    ) throws IOException {
        String tipo = solicitud.cabecera("content-type");
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            html(415, ManejadorCliente.paginaError(415, "Formato no admitido", "El formulario debe enviarse como application/x-www-form-urlencoded."))
                .escribir(socket, true);
            return null;
        }

        byte[] cuerpo;
        try {
            cuerpo = solicitud.leerCuerpo();
        } catch (SolicitudHttp.SolicitudInvalidaException e) {
            ManejadorCliente.responderSolicitudInvalida(socket, ip, e, true);
            return null;
        }

        Map<String, String> campos = new HashMap<>();
        String texto = new String(cuerpo, StandardCharsets.ISO_8859_1);
        if (texto.isEmpty()) {
            return campos;
        }

        String[] pares = texto.split("&", MAX_CAMPOS_FORMULARIO + 1);
        if (pares.length > MAX_CAMPOS_FORMULARIO) {
            html(400, ManejadorCliente.paginaError(400, "Solicitud no válida", "El formulario contiene demasiados campos."))
                .escribir(socket, true);
            return null;
        }

        try {
            for (String par : pares) {
                int igual = par.indexOf('=');
                String nombre = URLDecoder.decode(igual >= 0 ? par.substring(0, igual) : par, StandardCharsets.UTF_8);
                String valor = igual >= 0 ? URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8) : "";
                campos.putIfAbsent(nombre, valor);
            }
        } catch (IllegalArgumentException e) {
            html(400, ManejadorCliente.paginaError(400, "Solicitud no válida", "El formulario tiene un formato no válido."))
                .escribir(socket, true);
            return null;
        }

        return campos;
    }

    private static boolean origenPermitido(SolicitudHttp solicitud) {
        String origen = solicitud.cabecera("origin");
        if (origen == null || origen.isBlank() || origen.equals("null")) {
            return false;
        }

        String normalizado = origen.trim().toLowerCase(Locale.ROOT);
        if (ORIGENES_PERMITIDOS.contains(normalizado)) {
            return true;
        }

        String host = solicitud.cabecera("host");
        if (host == null || host.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(normalizado);
            if (uri.getHost() == null) {
                return false;
            }
            String hostOrigen = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
            return hostOrigen.equalsIgnoreCase(host.trim());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static void rechazarOrigen(Socket socket, String ip, String detalle) throws IOException {
        Estadisticas.registrarEvento(Estadisticas.TipoEvento.ACCESO_ADMIN_RECHAZADO, ip, detalle);
        html(403, ManejadorCliente.paginaError(403, "Acceso denegado", "La solicitud no procede de este sitio."))
            .escribir(socket, true);
    }

    private static boolean esHttps(SolicitudHttp solicitud) {
        String protocolo = solicitud.cabecera("x-forwarded-proto");
        if (protocolo != null && protocolo.trim().equalsIgnoreCase("https")) {
            return true;
        }
        String visitante = solicitud.cabecera("cf-visitor");
        return visitante != null && visitante.replace(" ", "").contains("\"scheme\":\"https\"");
    }

    private static String cookieSesion(String token, boolean segura) {
        return NOMBRE_COOKIE + "=" + token +
            "; Path=" + RUTA_PANEL +
            "; Max-Age=" + SESIONES.duracionMaximaSegundos() +
            "; HttpOnly; SameSite=Strict" +
            (segura ? "; Secure" : "");
    }

    private static String cookieBorrada(boolean segura) {
        return NOMBRE_COOKIE + "=; Path=" + RUTA_PANEL + "; Max-Age=0; HttpOnly; SameSite=Strict" +
            (segura ? "; Secure" : "");
    }

    private static void redirigir(Socket socket, String destino, String cookie, boolean incluirCuerpo) throws IOException {
        RespuestaHttp respuesta = RespuestaHttp.nueva(303)
            .cabecera("Location", destino)
            .cabecera("Cache-Control", "no-store");
        if (cookie != null) {
            respuesta.cabecera("Set-Cookie", cookie);
        }
        respuesta.escribir(socket, incluirCuerpo);
    }

    private static void metodoNoPermitido(Socket socket, String permitidos) throws IOException {
        html(405, ManejadorCliente.paginaError(405, "Método no permitido", "Esta ruta solo admite: " + permitidos + "."))
            .cabecera("Allow", permitidos)
            .escribir(socket, true);
    }

    private static RespuestaHttp html(int codigo, String pagina) {
        return RespuestaHttp.html(codigo, pagina)
            .cabecera("Cache-Control", "no-store")
            .cabecera("X-Robots-Tag", "noindex, nofollow");
    }

    private static String minutos(long segundos) {
        long min = Math.max(1, (segundos + 59) / 60);
        return min == 1 ? "1 minuto" : min + " minutos";
    }

    private static Set<String> cargarOrigenes() {
        Set<String> origenes = new LinkedHashSet<>();
        origenes.add("https://nexusumg.app");

        String valor = System.getProperty(PROPIEDAD_ORIGENES);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(VARIABLE_ORIGENES);
        }
        if (valor != null) {
            for (String origen : valor.split(",")) {
                if (!origen.isBlank()) {
                    origenes.add(origen.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return Set.copyOf(origenes);
    }

    private static String paginaEntrar(String usuario, String mensaje) {
        String aviso = mensaje == null ? "" : "<p class=\"aviso\" role=\"alert\">" + escaparHtml(mensaje) + "</p>";

        return plantilla(
            "Acceso al panel",
            """
            <form class="tarjeta" method="post" action="%s" autocomplete="on">
                <p class="etiqueta">NEXUS UMG · ADMINISTRACIÓN</p>
                <h1>Acceso al panel</h1>
                %s
                <label for="usuario">Usuario</label>
                <input id="usuario" name="usuario" type="text" value="%s" maxlength="32" autocomplete="username" autocapitalize="none" spellcheck="false" required autofocus>
                <label for="clave">Contraseña</label>
                <input id="clave" name="clave" type="password" maxlength="128" autocomplete="current-password" required>
                <button type="submit">Entrar</button>
                <a class="volver" href="/">← Volver al sitio</a>
            </form>
            """.formatted(RUTA_ENTRAR, aviso, escaparHtml(usuario))
        );
    }

    private static String paginaPanel(SesionesAdmin.Sesion sesion) {
        return plantilla(
            "Panel de administración",
            """
            <section class="tarjeta ancha">
                <p class="etiqueta">NEXUS UMG · ADMINISTRACIÓN</p>
                <h1>Panel de administración</h1>
                <p>Sesión iniciada como <strong>%s</strong>.</p>
                <p class="nota">El dashboard de estadísticas se añadirá en el siguiente paso.</p>
                <form method="post" action="%s">
                    <input type="hidden" name="csrf" value="%s">
                    <button type="submit">Cerrar sesión</button>
                </form>
            </section>
            """.formatted(escaparHtml(sesion.usuario()), RUTA_SALIR, escaparHtml(sesion.tokenCsrf()))
        );
    }

    private static String plantilla(String titulo, String contenido) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="robots" content="noindex, nofollow">
                <link rel="icon" href="data:,">
                <title>%s | Nexus UMG</title>
                <style>
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 24px 16px;
                        background: #10171d;
                        color: #e8e0d4;
                        font-family: Inter, Arial, sans-serif;
                    }
                    .tarjeta {
                        width: 100%%;
                        max-width: 380px;
                        display: flex;
                        flex-direction: column;
                        gap: 10px;
                        padding: 28px;
                        background: #0a0f13;
                        border: 1px solid #514532;
                    }
                    .ancha { max-width: 560px; }
                    .etiqueta {
                        margin: 0;
                        color: #c9a46a;
                        font-size: 10px;
                        font-weight: 600;
                        letter-spacing: 2px;
                    }
                    h1 {
                        margin: 0 0 8px;
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-weight: 500;
                        font-size: 32px;
                        color: #f5ecdf;
                    }
                    label { font-size: 12px; color: #aab0b5; }
                    input {
                        padding: 11px 12px;
                        background: #151e24;
                        color: #f0e6d6;
                        border: 1px solid #514532;
                        font-size: 15px;
                    }
                    input:focus { outline: 2px solid #c9a46a; outline-offset: 1px; }
                    button {
                        margin-top: 10px;
                        padding: 12px 15px;
                        border: 1px solid #c9a46a;
                        background: #c9a46a;
                        color: #171b1e;
                        font-size: 13px;
                        font-weight: 600;
                        cursor: pointer;
                    }
                    button:hover { background: transparent; color: #f0e6d6; }
                    .aviso {
                        margin: 0;
                        padding: 10px 12px;
                        background: #2a1616;
                        border: 1px solid #8a3b3b;
                        color: #f2c9c9;
                        font-size: 13px;
                    }
                    .nota { color: #aab0b5; font-size: 13px; }
                    .volver { margin-top: 6px; color: #aab0b5; font-size: 12px; text-decoration: none; }
                    .volver:hover { color: #c9a46a; }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """.formatted(escaparHtml(titulo), contenido);
    }

    private static String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}