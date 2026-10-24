import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
    private static final int MAX_EVENTOS_PANEL = 20;
    private static final int SEGUNDOS_RECARGA = 15;
    private static final DateTimeFormatter FORMATO_HORA =
        DateTimeFormatter.ofPattern("dd/MM HH:mm:ss").withZone(ZoneId.systemDefault());

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
            .reemplazarCabecera("Referrer-Policy", "same-origin")
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
        Estadisticas.Instantanea e = Estadisticas.instantanea();
        int ipsBloqueadas = LimitadorSolicitudes.ipsBloqueadasAhora();

        StringBuilder html = new StringBuilder(8192);
        html.append("<section class=\"tarjeta panel\">");
        html.append("<div class=\"cabecera-panel\"><div>");
        html.append("<p class=\"etiqueta\">NEXUS UMG · ADMINISTRACIÓN</p>");
        html.append("<h1>Panel de administración</h1>");
        html.append("<p class=\"meta\">Sesión iniciada como <strong>")
            .append(escaparHtml(sesion.usuario()))
            .append("</strong>. Actualizado ")
            .append(escaparHtml(FORMATO_HORA.format(Instant.ofEpochMilli(e.momentoMillis()))))
            .append(" · Encendido hace ")
            .append(escaparHtml(duracion(e.momentoMillis() - e.inicioMillis())))
            .append(" · Se recarga cada ").append(SEGUNDOS_RECARGA).append(" s.</p>");
        html.append("</div><div class=\"acciones\">");
        html.append("<a class=\"boton-sec\" href=\"").append(RUTA_PANEL).append("\">Actualizar</a>");
        html.append("<form method=\"post\" action=\"").append(RUTA_SALIR).append("\">");
        html.append("<input type=\"hidden\" name=\"csrf\" value=\"")
            .append(escaparHtml(sesion.tokenCsrf())).append("\">");
        html.append("<button type=\"submit\">Cerrar sesión</button></form>");
        html.append("</div></div>");

        html.append("<div class=\"cifras\">");
        cifra(html, "Recibidas", e.recibidas(), false);
        cifra(html, "Atendidas", e.atendidas(), false);
        cifra(html, "Respuestas 2xx", e.aceptadas(), false);
        cifra(html, "Frenadas (429)", e.bloqueadasPorLimitador(), e.bloqueadasPorLimitador() > 0);
        cifra(html, "Saturación (503)", e.rechazadasPorCapacidad(), e.rechazadasPorCapacidad() > 0);
        cifra(html, "Activas ahora", e.activas(), false);
        cifra(html, "En cola", e.enCola(), e.enCola() > 0);
        cifra(html, "IPs bloqueadas ahora", ipsBloqueadas, ipsBloqueadas > 0);
        cifra(html, "IPs en seguimiento", LimitadorSolicitudes.ipsSeguidas(), false);
        cifra(html, "Sesiones admin", SESIONES.activas(), false);
        html.append("</div>");

        html.append("<div class=\"rejilla\">");

        abrirBloque(html, "Respuestas por código", "Código", "Total");
        if (e.respuestasPorCodigo().isEmpty()) {
            filaVacia(html, 2, "Sin respuestas todavía.");
        }
        for (Map.Entry<Integer, Long> entrada : e.respuestasPorCodigo().entrySet()) {
            fila(html, entrada.getKey() + " " + razon(entrada.getKey()), entrada.getValue());
        }
        cerrarBloque(html);

        abrirBloque(html, "Eventos por tipo", "Evento", "Total");
        boolean hayEventos = false;
        for (Map.Entry<Estadisticas.TipoEvento, Long> entrada : e.eventosPorTipo().entrySet()) {
            if (entrada.getValue() > 0) {
                hayEventos = true;
                fila(html, nombreEvento(entrada.getKey()), entrada.getValue());
            }
        }
        if (!hayEventos) {
            filaVacia(html, 2, "Sin eventos registrados.");
        }
        cerrarBloque(html);

        abrirBloque(html, "Rutas más visitadas", "Ruta", "Visitas");
        List<Estadisticas.RutaVisitada> rutas = Estadisticas.getTopRutas(10);
        if (rutas.isEmpty()) {
            filaVacia(html, 2, "Sin visitas todavía.");
        }
        for (Estadisticas.RutaVisitada ruta : rutas) {
            fila(html, ruta.ruta(), ruta.total());
        }
        cerrarBloque(html);

        abrirBloque(html, "Conexiones sin respuesta", "Motivo", "Total");
        boolean haySinRespuesta = false;
        for (Map.Entry<Estadisticas.MotivoSinRespuesta, Long> entrada : e.sinRespuesta().entrySet()) {
            if (entrada.getValue() > 0) {
                haySinRespuesta = true;
                fila(html, nombreMotivo(entrada.getKey()), entrada.getValue());
            }
        }
        if (!haySinRespuesta) {
            filaVacia(html, 2, "Ninguna.");
        }
        cerrarBloque(html);

        html.append("</div>");

        html.append("<div class=\"bloque\"><h2>Últimos eventos</h2><div class=\"tabla\"><table>");
        html.append("<thead><tr><th>Hora</th><th>Evento</th><th>IP</th><th>Detalle</th><th class=\"num\">Veces</th></tr></thead><tbody>");
        List<Estadisticas.EventoReciente> recientes = Estadisticas.getEventosRecientes(MAX_EVENTOS_PANEL);
        if (recientes.isEmpty()) {
            filaVacia(html, 5, "Sin eventos registrados.");
        }
        for (Estadisticas.EventoReciente evento : recientes) {
            html.append("<tr><td>")
                .append(escaparHtml(FORMATO_HORA.format(Instant.ofEpochMilli(evento.ultimoMillis()))))
                .append("</td><td>").append(escaparHtml(nombreEvento(evento.tipo())))
                .append("</td><td>").append(escaparHtml(evento.ip()))
                .append("</td><td>").append(escaparHtml(evento.detalle()))
                .append("</td><td class=\"num\">").append(evento.repeticiones())
                .append("</td></tr>");
        }
        html.append("</tbody></table></div></div>");

        html.append("<p class=\"vacio\">Datos en memoria desde el último arranque del servidor. ")
            .append("Al reiniciarlo vuelven a cero. Se guardan como máximo los 100 eventos más recientes.</p>");
        html.append("</section>");

        return plantilla("Panel de administración", html.toString(), SEGUNDOS_RECARGA);
    }

    private static void cifra(StringBuilder html, String nombre, long valor, boolean alerta) {
        html.append("<div class=\"cifra").append(alerta ? " alerta" : "").append("\"><span>")
            .append(escaparHtml(nombre)).append("</span><strong>").append(valor).append("</strong></div>");
    }

    private static void abrirBloque(StringBuilder html, String titulo, String columna, String columnaNumero) {
        html.append("<div class=\"bloque\"><h2>").append(escaparHtml(titulo))
            .append("</h2><div class=\"tabla\"><table><thead><tr><th>").append(escaparHtml(columna))
            .append("</th><th class=\"num\">").append(escaparHtml(columnaNumero))
            .append("</th></tr></thead><tbody>");
    }

    private static void cerrarBloque(StringBuilder html) {
        html.append("</tbody></table></div></div>");
    }

    private static void fila(StringBuilder html, String nombre, long valor) {
        html.append("<tr><td>").append(escaparHtml(nombre))
            .append("</td><td class=\"num\">").append(valor).append("</td></tr>");
    }

    private static void filaVacia(StringBuilder html, int columnas, String texto) {
        html.append("<tr><td class=\"vacio\" colspan=\"").append(columnas).append("\">")
            .append(escaparHtml(texto)).append("</td></tr>");
    }

    private static String razon(int codigo) {
        return switch (codigo) {
            case 200 -> "OK";
            case 303 -> "Redirección";
            case 400 -> "Solicitud no válida";
            case 401 -> "No autorizado";
            case 403 -> "Prohibido";
            case 404 -> "No encontrado";
            case 405 -> "Método no permitido";
            case 411 -> "Falta longitud";
            case 413 -> "Demasiado grande";
            case 414 -> "Dirección demasiado larga";
            case 415 -> "Formato no admitido";
            case 429 -> "Demasiadas solicitudes";
            case 431 -> "Cabeceras demasiado grandes";
            case 500 -> "Error interno";
            case 501 -> "No implementado";
            case 503 -> "Servidor ocupado";
            default -> "";
        };
    }

    private static String nombreEvento(Estadisticas.TipoEvento tipo) {
        return switch (tipo) {
            case LIMITE_GLOBAL -> "Límite global superado";
            case LIMITE_PAGINAS -> "Límite de páginas superado";
            case LIMITE_IMAGENES -> "Límite de imágenes superado";
            case CAPACIDAD_IPS -> "Sin capacidad para más IPs";
            case SATURACION -> "Servidor saturado (503)";
            case RUTA_PROTEGIDA -> "Ruta protegida solicitada";
            case SOLICITUD_INVALIDA -> "Solicitud no válida";
            case ORIGEN_NO_VERIFICADO -> "Origen Cloudflare no verificado";
            case LOGIN_EXITOSO -> "Inicio de sesión correcto";
            case LOGIN_FALLIDO -> "Inicio de sesión fallido";
            case LOGIN_BLOQUEADO -> "Intento durante bloqueo de login";
            case ACCESO_ADMIN_RECHAZADO -> "Acceso al panel rechazado";
            case BLOQUEO_TEMPORAL -> "IP bloqueada temporalmente";
            case IP_BLOQUEADA -> "Solicitud de IP bloqueada";
        };
    }

    private static String nombreMotivo(Estadisticas.MotivoSinRespuesta motivo) {
        return switch (motivo) {
            case CONEXION_VACIA -> "Conexión vacía";
            case TIEMPO_AGOTADO -> "Tiempo agotado";
            case ERROR_IO -> "Error de red";
            case CANCELADA_AL_CERRAR -> "Cancelada al cerrar";
        };
    }

    private static String duracion(long millis) {
        long segundos = Math.max(0, millis / 1000);
        long dias = segundos / 86_400;
        long horas = (segundos % 86_400) / 3600;
        long minutos = (segundos % 3600) / 60;
        if (dias > 0) {
            return dias + " d " + horas + " h";
        }
        if (horas > 0) {
            return horas + " h " + minutos + " min";
        }
        if (minutos > 0) {
            return minutos + " min " + (segundos % 60) + " s";
        }
        return segundos + " s";
    }

    private static String plantilla(String titulo, String contenido) {
        return plantilla(titulo, contenido, 0);
    }

    private static String plantilla(String titulo, String contenido, int segundosRecarga) {
        String recarga = segundosRecarga > 0
            ? "<meta http-equiv=\"refresh\" content=\"" + segundosRecarga + "\">"
            : "";
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="robots" content="noindex, nofollow">
                %s
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
                    .panel { max-width: 1100px; gap: 18px; }
                    .cabecera-panel { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: flex-end; gap: 14px; }
                    .meta { margin: 0; color: #aab0b5; font-size: 13px; line-height: 1.5; }
                    .meta strong { color: #f0e6d6; }
                    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
                    .acciones form { margin: 0; }
                    .acciones button { margin-top: 0; }
                    .boton-sec { padding: 11px 14px; border: 1px solid #514532; color: #e8e0d4; font-size: 13px; text-decoration: none; }
                    .boton-sec:hover { border-color: #c9a46a; color: #c9a46a; }
                    .cifras { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 10px; }
                    .cifra { padding: 14px; background: #151e24; border: 1px solid #2c3640; }
                    .cifra span { display: block; color: #aab0b5; font-size: 11px; letter-spacing: 1px; text-transform: uppercase; }
                    .cifra strong { display: block; margin-top: 6px; color: #f5ecdf; font-size: 26px; font-weight: 600; }
                    .cifra.alerta { border-color: #8a3b3b; }
                    .cifra.alerta strong { color: #f2b0b0; }
                    .rejilla { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
                    .bloque { min-width: 0; padding: 14px; background: #0d1318; border: 1px solid #2c3640; }
                    .bloque h2 { margin: 0 0 10px; color: #c9a46a; font-family: Inter, Arial, sans-serif; font-size: 12px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; }
                    .tabla { overflow-x: auto; }
                    table { width: 100%%; border-collapse: collapse; font-size: 13px; }
                    th, td { padding: 6px 8px; text-align: left; border-bottom: 1px solid #1f2a33; vertical-align: top; }
                    th { color: #aab0b5; font-size: 11px; font-weight: 500; letter-spacing: 1px; text-transform: uppercase; }
                    .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
                    .vacio { margin: 0; color: #77808a; font-size: 12px; }
                    @media (max-width: 600px) { .panel { padding: 18px; } .cifra strong { font-size: 22px; } }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """.formatted(recarga, escaparHtml(titulo), contenido);
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