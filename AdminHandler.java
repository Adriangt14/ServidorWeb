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
        String aviso = mensaje == null ? "" : "<p class=\"error-entrada\" role=\"alert\">" + escaparHtml(mensaje) + "</p>";

        String contenido = """
            <main class="entrada">
                <form class="vidrio tarjeta-entrada" method="post" action="%s" autocomplete="on">
                    <a class="marca" href="/">NEXUS <span>UMG</span></a>
                    <p class="etiqueta">Centro de control</p>
                    <h1>Acceso al panel</h1>
                    %s
                    <label for="usuario">Usuario</label>
                    <input id="usuario" name="usuario" type="text" value="%s" maxlength="32" autocomplete="username" autocapitalize="none" spellcheck="false" required autofocus>
                    <label for="clave">Contraseña</label>
                    <input id="clave" name="clave" type="password" maxlength="128" autocomplete="current-password" required>
                    <button type="submit">Entrar</button>
                    <a class="volver" href="/">← Volver al sitio</a>
                </form>
            </main>
            """.formatted(RUTA_ENTRAR, aviso, escaparHtml(usuario));

        return plantilla("Acceso al panel", contenido, 0);
    }

    private static String paginaPanel(SesionesAdmin.Sesion sesion) {
        Estadisticas.Instantanea e = Estadisticas.instantanea();
        int ipsBloqueadas = LimitadorSolicitudes.ipsBloqueadasAhora();

        StringBuilder html = new StringBuilder(16_384);
        html.append("<div class=\"app\">");

        html.append("<header class=\"barra vidrio\">");
        html.append("<div class=\"barra-marca\"><a class=\"marca\" href=\"/\">NEXUS <span>UMG</span></a>")
            .append("<span class=\"subtitulo\">Centro de control</span></div>");
        html.append("<div class=\"barra-estado\"><span class=\"punto\"></span>En línea · encendido hace ")
            .append(escaparHtml(duracion(e.momentoMillis() - e.inicioMillis())))
            .append(" · actualizado ")
            .append(escaparHtml(FORMATO_HORA.format(Instant.ofEpochMilli(e.momentoMillis()))))
            .append("</div>");
        html.append("<div class=\"acciones\"><span class=\"usuario\">Sesión iniciada como <strong>")
            .append(escaparHtml(sesion.usuario())).append("</strong></span>");
        html.append("<a class=\"boton-sec\" href=\"").append(RUTA_PANEL).append("\">Actualizar</a>");
        html.append("<form method=\"post\" action=\"").append(RUTA_SALIR).append("\">");
        html.append("<input type=\"hidden\" name=\"csrf\" value=\"")
            .append(escaparHtml(sesion.tokenCsrf())).append("\">");
        html.append("<button type=\"submit\">Cerrar sesión</button></form></div>");
        html.append("</header>");

        html.append("<main class=\"tablero\">");

        html.append("<section class=\"kpis\" aria-label=\"Resumen\">");
        cifra(html, "Recibidas", e.recibidas(), "");
        cifra(html, "Respuestas 2xx", e.aceptadas(), "bien");
        cifra(html, "Frenadas (429)", e.bloqueadasPorLimitador(), e.bloqueadasPorLimitador() > 0 ? "aviso" : "");
        cifra(html, "Saturación (503)", e.rechazadasPorCapacidad(), e.rechazadasPorCapacidad() > 0 ? "alerta" : "");
        cifra(html, "IPs bloqueadas ahora", ipsBloqueadas, ipsBloqueadas > 0 ? "alerta" : "");
        cifra(html, "Atendidas", e.atendidas(), "");
        cifra(html, "Activas ahora", e.activas(), "");
        cifra(html, "En cola", e.enCola(), e.enCola() > 0 ? "aviso" : "");
        cifra(html, "IPs en seguimiento", LimitadorSolicitudes.ipsSeguidas(), "");
        cifra(html, "Sesiones admin", SESIONES.activas(), "");
        html.append("</section>");

        abrirPanel(html, "p-codigos", "Respuestas por código");
        long maxCodigo = 1;
        for (long valor : e.respuestasPorCodigo().values()) {
            maxCodigo = Math.max(maxCodigo, valor);
        }
        if (e.respuestasPorCodigo().isEmpty()) {
            vacio(html, "Sin respuestas todavía.");
        }
        for (Map.Entry<Integer, Long> entrada : e.respuestasPorCodigo().entrySet()) {
            barra(html, entrada.getKey() + " " + razon(entrada.getKey()), entrada.getValue(), maxCodigo, claseCodigo(entrada.getKey()));
        }
        cerrarPanel(html);

        abrirPanel(html, "p-tipos", "Eventos por tipo");
        long maxEvento = 1;
        for (long valor : e.eventosPorTipo().values()) {
            maxEvento = Math.max(maxEvento, valor);
        }
        boolean hayEventos = false;
        for (Map.Entry<Estadisticas.TipoEvento, Long> entrada : e.eventosPorTipo().entrySet()) {
            if (entrada.getValue() > 0) {
                hayEventos = true;
                barra(html, nombreEvento(entrada.getKey()), entrada.getValue(), maxEvento, gravedad(entrada.getKey()));
            }
        }
        if (!hayEventos) {
            vacio(html, "Sin eventos registrados.");
        }
        cerrarPanel(html);

        abrirPanel(html, "p-rutas", "Rutas más visitadas");
        List<Estadisticas.RutaVisitada> rutas = Estadisticas.getTopRutas(10);
        long maxRuta = rutas.isEmpty() ? 1 : Math.max(1, rutas.get(0).total());
        if (rutas.isEmpty()) {
            vacio(html, "Sin visitas todavía.");
        }
        for (Estadisticas.RutaVisitada ruta : rutas) {
            barra(html, ruta.ruta(), ruta.total(), maxRuta, "oro");
        }
        cerrarPanel(html);

        html.append("<section class=\"vidrio panel p-sin\"><h2>Conexiones sin respuesta</h2><div class=\"chips\">");
        boolean haySinRespuesta = false;
        for (Map.Entry<Estadisticas.MotivoSinRespuesta, Long> entrada : e.sinRespuesta().entrySet()) {
            if (entrada.getValue() > 0) {
                haySinRespuesta = true;
                html.append("<span class=\"chip\">").append(escaparHtml(nombreMotivo(entrada.getKey())))
                    .append(" <b>").append(entrada.getValue()).append("</b></span>");
            }
        }
        if (!haySinRespuesta) {
            html.append("<span class=\"vacio\">Ninguna.</span>");
        }
        html.append("</div></section>");

        html.append("<aside class=\"vidrio panel p-feed\"><h2>Últimos eventos</h2><div class=\"cuerpo\"><ol class=\"feed\">");
        List<Estadisticas.EventoReciente> recientes = Estadisticas.getEventosRecientes(MAX_EVENTOS_PANEL);
        if (recientes.isEmpty()) {
            html.append("<li class=\"vacio\">Sin eventos registrados.</li>");
        }
        for (Estadisticas.EventoReciente evento : recientes) {
            html.append("<li class=\"ev ").append(gravedad(evento.tipo())).append("\">")
                .append("<span class=\"ev-hora\">")
                .append(escaparHtml(FORMATO_HORA.format(Instant.ofEpochMilli(evento.ultimoMillis()))))
                .append("</span><div class=\"ev-texto\"><strong>").append(escaparHtml(nombreEvento(evento.tipo())))
                .append("</strong><span class=\"ev-ip\">").append(escaparHtml(evento.ip())).append("</span>");
            if (!evento.detalle().isEmpty()) {
                html.append("<span class=\"ev-det\">").append(escaparHtml(evento.detalle())).append("</span>");
            }
            html.append("</div>");
            if (evento.repeticiones() > 1) {
                html.append("<span class=\"ev-veces\">×").append(evento.repeticiones()).append("</span>");
            }
            html.append("</li>");
        }
        html.append("</ol></div></aside>");

        html.append("</main>");
        html.append("</div>");

        return plantilla("Panel de administración", html.toString(), SEGUNDOS_RECARGA);
    }

    private static void cifra(StringBuilder html, String nombre, long valor, String clase) {
        html.append("<div class=\"vidrio cifra ").append(clase).append("\"><span>")
            .append(escaparHtml(nombre)).append("</span><strong>").append(valor).append("</strong></div>");
    }

    private static void abrirPanel(StringBuilder html, String clase, String titulo) {
        html.append("<section class=\"vidrio panel ").append(clase).append("\"><h2>")
            .append(escaparHtml(titulo)).append("</h2><div class=\"cuerpo\"><ul class=\"barras\">");
    }

    private static void cerrarPanel(StringBuilder html) {
        html.append("</ul></div></section>");
    }

    private static void vacio(StringBuilder html, String texto) {
        html.append("<li class=\"vacio\">").append(escaparHtml(texto)).append("</li>");
    }

    private static void barra(StringBuilder html, String nombre, long valor, long maximo, String clase) {
        long ancho = Math.max(2, Math.min(100, Math.round(valor * 100.0 / Math.max(1, maximo))));
        html.append("<li class=\"").append(clase).append("\"><div class=\"fila\"><span class=\"nombre\">")
            .append(escaparHtml(nombre)).append("</span><span class=\"valor\">").append(valor)
            .append("</span></div><div class=\"pista\"><i style=\"width:").append(ancho).append("%\"></i></div></li>");
    }

    private static String claseCodigo(int codigo) {
        if (codigo >= 500) {
            return "alerta";
        }
        if (codigo == 429 || codigo == 401 || codigo == 403) {
            return "aviso";
        }
        if (codigo >= 400) {
            return "neutro";
        }
        if (codigo >= 300) {
            return "info";
        }
        return "bien";
    }

    private static String gravedad(Estadisticas.TipoEvento tipo) {
        return switch (tipo) {
            case LOGIN_EXITOSO -> "bien";
            case LIMITE_GLOBAL, LIMITE_PAGINAS, LIMITE_IMAGENES, RUTA_PROTEGIDA, SOLICITUD_INVALIDA -> "aviso";
            case CAPACIDAD_IPS, SATURACION, ORIGEN_NO_VERIFICADO, LOGIN_FALLIDO, LOGIN_BLOQUEADO,
                ACCESO_ADMIN_RECHAZADO, BLOQUEO_TEMPORAL, IP_BLOQUEADA -> "alerta";
        };
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

    private static String plantilla(String titulo, String contenido, int segundosRecarga) {
        String recarga = segundosRecarga > 0
            ? "<meta http-equiv=\"refresh\" content=\"" + segundosRecarga + "\">"
            : "";
        return PLANTILLA
            .replace("{{RECARGA}}", recarga)
            .replace("{{TITULO}}", escaparHtml(titulo))
            .replace("{{CONTENIDO}}", contenido);
    }

    private static final String PLANTILLA = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="robots" content="noindex, nofollow">
            <meta name="theme-color" content="#0a0f13">
            {{RECARGA}}
            <link rel="icon" href="data:,">
            <title>{{TITULO}} | Nexus UMG</title>
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
            <style>
                :root {
                    --oro: #c9a46a;
                    --marfil: #f5ecdf;
                    --texto: #e8e0d4;
                    --suave: #aab0b5;
                    --tenue: #7d868f;
                    --vidrio: rgba(10, 15, 19, 0.7);
                    --borde: rgba(201, 164, 106, 0.26);
                    --linea: rgba(255, 255, 255, 0.07);
                    --bien: #86c9a0;
                    --info: #8fb4d9;
                    --aviso: #e2c27a;
                    --alerta: #ec8f8f;
                }
                * { box-sizing: border-box; }
                html, body { margin: 0; min-height: 100%; }
                body {
                    background: #0a0f13;
                    color: var(--texto);
                    font-family: Inter, Arial, sans-serif;
                    font-size: 14px;
                    line-height: 1.45;
                }
                body::before {
                    content: "";
                    position: fixed;
                    inset: 0;
                    z-index: -1;
                    background:
                        linear-gradient(115deg, rgba(8, 13, 17, 0.9) 0%, rgba(8, 13, 17, 0.72) 50%, rgba(8, 13, 17, 0.42) 100%),
                        url('/imagenes/encabezado.jpeg') center / cover no-repeat;
                }
                a { color: inherit; text-decoration: none; }
                .vidrio {
                    background: var(--vidrio);
                    border: 1px solid var(--borde);
                    backdrop-filter: blur(10px);
                    -webkit-backdrop-filter: blur(10px);
                }
                .marca {
                    font-family: 'Cormorant Garamond', Georgia, serif;
                    font-size: 26px;
                    letter-spacing: 1.5px;
                    color: var(--marfil);
                }
                .marca span { color: var(--oro); }
                .etiqueta {
                    margin: 0;
                    color: var(--oro);
                    font-size: 10px;
                    font-weight: 600;
                    letter-spacing: 2px;
                    text-transform: uppercase;
                }
                h1 {
                    margin: 0 0 6px;
                    font-family: 'Cormorant Garamond', Georgia, serif;
                    font-weight: 500;
                    font-size: 34px;
                    color: var(--marfil);
                }
                button {
                    padding: 10px 15px;
                    border: 1px solid var(--oro);
                    background: var(--oro);
                    color: #171b1e;
                    font: 600 13px Inter, Arial, sans-serif;
                    cursor: pointer;
                }
                button:hover { background: transparent; color: var(--marfil); }
                .boton-sec {
                    padding: 9px 14px;
                    border: 1px solid var(--borde);
                    color: var(--texto);
                    font-size: 13px;
                }
                .boton-sec:hover { border-color: var(--oro); color: var(--oro); }

                .entrada {
                    min-height: 100vh;
                    display: grid;
                    place-items: center;
                    padding: 24px 16px;
                }
                .tarjeta-entrada {
                    width: 100%;
                    max-width: 400px;
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                    padding: 32px 30px;
                }
                .tarjeta-entrada .marca { margin-bottom: 14px; }
                label { margin-top: 4px; font-size: 12px; color: var(--suave); }
                input {
                    padding: 11px 12px;
                    background: rgba(21, 30, 36, 0.85);
                    color: var(--marfil);
                    border: 1px solid var(--borde);
                    font-size: 15px;
                }
                input:focus { outline: 2px solid var(--oro); outline-offset: 1px; }
                .tarjeta-entrada button { margin-top: 12px; }
                .error-entrada {
                    margin: 0;
                    padding: 10px 12px;
                    background: rgba(70, 24, 24, 0.75);
                    border: 1px solid #8a3b3b;
                    color: #f2c9c9;
                    font-size: 13px;
                }
                .volver { margin-top: 6px; color: var(--suave); font-size: 12px; }
                .volver:hover { color: var(--oro); }

                .app {
                    height: 100vh;
                    display: grid;
                    grid-template-rows: auto minmax(0, 1fr);
                    gap: 12px;
                    padding: 14px 18px;
                }
                .barra {
                    display: flex;
                    flex-wrap: wrap;
                    align-items: center;
                    justify-content: space-between;
                    gap: 10px 20px;
                    padding: 10px 16px;
                }
                .barra-marca { display: flex; align-items: baseline; gap: 12px; }
                .subtitulo {
                    color: var(--oro);
                    font-size: 10px;
                    font-weight: 600;
                    letter-spacing: 2px;
                    text-transform: uppercase;
                }
                .barra-estado { display: flex; align-items: center; gap: 8px; color: var(--suave); font-size: 12px; }
                .punto {
                    width: 8px;
                    height: 8px;
                    border-radius: 50%;
                    background: var(--bien);
                    box-shadow: 0 0 0 3px rgba(134, 201, 160, 0.18);
                }
                .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
                .acciones form { margin: 0; }
                .usuario { color: var(--suave); font-size: 12px; }
                .usuario strong { color: var(--marfil); }

                .tablero {
                    min-height: 0;
                    display: grid;
                    grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(290px, 0.95fr);
                    grid-template-rows: auto minmax(0, 1fr) auto;
                    gap: 12px;
                }
                .kpis {
                    grid-column: 1 / 4;
                    display: grid;
                    grid-template-columns: repeat(5, minmax(0, 1fr));
                    gap: 10px;
                }
                .cifra { padding: 10px 14px; border-left: 3px solid var(--borde); }
                .cifra span {
                    display: block;
                    color: var(--suave);
                    font-size: 10px;
                    letter-spacing: 1px;
                    text-transform: uppercase;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                .cifra strong {
                    display: block;
                    margin-top: 2px;
                    color: var(--marfil);
                    font-size: 26px;
                    font-weight: 600;
                    font-variant-numeric: tabular-nums;
                }
                .cifra.bien { border-left-color: var(--bien); }
                .cifra.aviso { border-left-color: var(--aviso); }
                .cifra.aviso strong { color: var(--aviso); }
                .cifra.alerta { border-left-color: var(--alerta); }
                .cifra.alerta strong { color: var(--alerta); }

                .panel {
                    min-height: 0;
                    display: flex;
                    flex-direction: column;
                    padding: 12px 14px;
                }
                .panel h2 {
                    margin: 0 0 10px;
                    color: var(--oro);
                    font: 600 11px Inter, Arial, sans-serif;
                    letter-spacing: 1.5px;
                    text-transform: uppercase;
                }
                .cuerpo { min-height: 0; overflow-y: auto; flex: 1; scrollbar-width: thin; scrollbar-color: #514532 transparent; }
                .barras, .feed { list-style: none; margin: 0; padding: 0; }
                .barras li { padding: 6px 0; border-bottom: 1px solid var(--linea); }
                .barras li:last-child { border-bottom: 0; }
                .fila { display: flex; justify-content: space-between; gap: 10px; font-size: 13px; }
                .nombre { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                .valor { color: var(--marfil); font-weight: 600; font-variant-numeric: tabular-nums; }
                .pista { height: 4px; margin-top: 5px; background: var(--linea); }
                .pista i { display: block; height: 100%; background: var(--oro); }
                .bien .pista i { background: var(--bien); }
                .info .pista i { background: var(--info); }
                .aviso .pista i { background: var(--aviso); }
                .alerta .pista i { background: var(--alerta); }
                .neutro .pista i { background: var(--tenue); }
                .vacio { color: var(--tenue); font-size: 12px; border: 0; }

                .p-sin { grid-column: 1 / 4; flex-direction: row; align-items: center; gap: 14px; padding: 10px 14px; }
                .p-sin h2 { margin: 0; white-space: nowrap; }
                .chips { display: flex; flex-wrap: wrap; gap: 8px; }
                .chip { padding: 3px 10px; border: 1px solid var(--borde); font-size: 12px; color: var(--suave); }
                .chip b { color: var(--marfil); }

                .p-feed { grid-column: 4; grid-row: 1 / 4; }
                .ev {
                    display: grid;
                    grid-template-columns: auto minmax(0, 1fr) auto;
                    gap: 10px;
                    padding: 8px 0 8px 10px;
                    border-bottom: 1px solid var(--linea);
                    border-left: 3px solid var(--tenue);
                }
                .ev.bien { border-left-color: var(--bien); }
                .ev.aviso { border-left-color: var(--aviso); }
                .ev.alerta { border-left-color: var(--alerta); }
                .ev-hora { color: var(--tenue); font-size: 11px; font-variant-numeric: tabular-nums; padding-top: 1px; }
                .ev-texto { display: flex; flex-direction: column; min-width: 0; }
                .ev-texto strong { font-size: 13px; font-weight: 500; color: var(--marfil); }
                .ev-ip, .ev-det { font-size: 12px; color: var(--suave); overflow-wrap: anywhere; }
                .ev-det { color: var(--tenue); }
                .ev-veces { align-self: start; padding: 1px 7px; border: 1px solid var(--borde); color: var(--oro); font-size: 11px; }

                @media (max-width: 1100px), (max-height: 600px) {
                    .app { height: auto; min-height: 100vh; }
                    .tablero { grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-rows: none; }
                    .kpis, .p-sin, .p-feed { grid-column: 1 / -1; grid-row: auto; }
                    .kpis { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); }
                    .panel { max-height: 360px; }
                    .p-feed { max-height: 460px; }
                    .p-rutas { grid-column: 1 / -1; }
                }
                @media (max-width: 640px) {
                    .app { padding: 10px; }
                    .tablero { grid-template-columns: minmax(0, 1fr); }
                    .kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                    .barra { padding: 12px; }
                    .cifra strong { font-size: 22px; }
                    .p-sin { flex-direction: column; align-items: flex-start; }
                }
            </style>
        </head>
        <body>
            {{CONTENIDO}}
        </body>
        </html>
        """;

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