import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManejadorCliente {

    private static final int TIMEOUT_LECTURA_MS = 5000;
    private static final long PLAZO_LECTURA_NANOS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT_LECTURA_MS);
    private static final long GRACIA_TRAS_COLA_NANOS = TimeUnit.MILLISECONDS.toNanos(300);
    private static final long MAX_TAMANO_IMAGEN_BYTES = 8L * 1024 * 1024;
    private static final long MAX_CACHE_IMAGENES_BYTES = 32L * 1024 * 1024;

    private static final ConcurrentHashMap<Path, byte[]> CACHE_IMAGENES =
        new ConcurrentHashMap<>();
    private static final Object BLOQUEO_CACHE_IMAGENES = new Object();
    private static volatile long bytesCacheImagenes;
    private static final AtomicLong TIMEOUTS_LECTURA = new AtomicLong();
    private static final AtomicLong ERRORES_IO = new AtomicLong();
    private static final AtomicLong IMAGENES_NO_ENCONTRADAS = new AtomicLong();
    private static final AtomicLong NOMBRES_CORREGIDOS = new AtomicLong();
    private static volatile Path carpetaImagenesCache;

    public static void atender(Socket clienteSocket, String id) {
        atender(clienteSocket, id, System.nanoTime());
    }

    public static void atender(Socket clienteSocket, String id, long aceptadaNanos) {

        String hilo = Thread.currentThread().getName();
        String ipConexion = clienteSocket.getInetAddress().getHostAddress();
        String ip = ipConexion;
        String ruta = Rutas.INICIO;

        Estadisticas.iniciarSolicitud(id);

        try {
            SolicitudHttp solicitud;
            try {
                solicitud = SolicitudHttp.leerHasta(clienteSocket, plazoLectura(aceptadaNanos));
            } catch (SolicitudHttp.SolicitudInvalidaException e) {
                responderSolicitudInvalida(clienteSocket, ip, e, true);
                return;
            }

            if (solicitud == null) {
                Estadisticas.registrarSinRespuesta(
                    Estadisticas.MotivoSinRespuesta.CONEXION_VACIA
                );
                return;
            }

            OrigenCliente.Origen origen = OrigenCliente.resolver(clienteSocket, solicitud);
            ip = origen.ip();
            Estadisticas.actualizarIp(id, ip);

            if (origen.tipo() == OrigenCliente.Tipo.CLOUDFLARE_RECHAZADO) {
                Estadisticas.registrarEvento(
                    Estadisticas.TipoEvento.ORIGEN_NO_VERIFICADO,
                    ip,
                    "CF-Connecting-IP sin cabecera secreta válida"
                );
            }

            boolean incluirCuerpo = !solicitud.pideSoloCabeceras();

            LimitadorSolicitudes.Resultado decisionGlobal =
                LimitadorSolicitudes.comprobarGlobal(ip);

            if (!decisionGlobal.permitido()) {
                Estadisticas.registrarEvento(
                    decisionGlobal.sinCapacidad()
                        ? Estadisticas.TipoEvento.CAPACIDAD_IPS
                        : decisionGlobal.ipBloqueada()
                            ? Estadisticas.TipoEvento.IP_BLOQUEADA
                            : Estadisticas.TipoEvento.LIMITE_GLOBAL,
                    ip,
                    ""
                );
                enviarLimiteSolicitudes(
                    clienteSocket,
                    decisionGlobal.retryAfterSegundos(),
                    incluirCuerpo
                );
                return;
            }

            try {
                solicitud.interpretarLinea();
            } catch (SolicitudHttp.SolicitudInvalidaException e) {
                responderSolicitudInvalida(clienteSocket, ip, e, incluirCuerpo);
                return;
            }

            String metodo = solicitud.metodo();
            incluirCuerpo = !metodo.equals("HEAD");

            if (Rutas.esRutaAdmin(solicitud.objetivoSinConsulta())) {
                ruta = Rutas.ADMIN;
                AdminHandler.atender(clienteSocket, id, solicitud, origen, incluirCuerpo);
                return;
            }

            if (!metodo.equals("GET") && !metodo.equals("HEAD")) {
                RespuestaHttp.html(
                    405,
                    paginaError(405, "Método no permitido", "Este servidor solo admite solicitudes GET y HEAD.")
                )
                    .cabecera("Allow", "GET, HEAD")
                    .escribir(clienteSocket, true);
                return;
            }

            try {
                solicitud.interpretarRuta();
            } catch (SolicitudHttp.SolicitudInvalidaException e) {
                ruta = "";
                responderSolicitudInvalida(clienteSocket, ip, e, incluirCuerpo);
                return;
            }

            ruta = solicitud.ruta();

            if (Rutas.esRutaProtegida(ruta)) {
                Estadisticas.registrarEvento(
                    Estadisticas.TipoEvento.RUTA_PROTEGIDA,
                    ip,
                    ruta
                );

                RespuestaHttp.html(404, paginaError("Recurso no encontrado."))
                    .escribir(clienteSocket, incluirCuerpo);
                return;
            }

            boolean esImagen = Rutas.esImagen(ruta);
            LimitadorSolicitudes.Resultado decisionRecurso =
                LimitadorSolicitudes.comprobar(ip, esImagen);

            if (!decisionRecurso.permitido()) {
                Estadisticas.registrarEvento(
                    decisionRecurso.sinCapacidad()
                        ? Estadisticas.TipoEvento.CAPACIDAD_IPS
                        : esImagen
                            ? Estadisticas.TipoEvento.LIMITE_IMAGENES
                            : Estadisticas.TipoEvento.LIMITE_PAGINAS,
                    ip,
                    Rutas.rutaEstadistica(ruta)
                );
                enviarLimiteSolicitudes(
                    clienteSocket,
                    decisionRecurso.retryAfterSegundos(),
                    incluirCuerpo
                );
                return;
            }

            Estadisticas.actualizarRuta(id, ruta);

            if (esImagen) {
                enviarImagen(ruta, clienteSocket, incluirCuerpo);
                return;
            }

            if (!Rutas.esPaginaValida(ruta)) {
                RespuestaHttp.html(404, paginaError("La página solicitada no existe."))
                    .escribir(clienteSocket, incluirCuerpo);
                return;
            }

            RespuestaHttp.html(200, PaginaWeb.generarBytes(ruta, hilo))
                .escribir(clienteSocket, incluirCuerpo);

        } catch (SocketTimeoutException e) {
            Estadisticas.registrarSinRespuesta(
                Estadisticas.MotivoSinRespuesta.TIEMPO_AGOTADO
            );
            long total = TIMEOUTS_LECTURA.incrementAndGet();
            if (total == 1 || total % 100 == 0) {
                System.err.println(
                    "[LECTURA] Tiempos agotados: " + total + " | última IP: " + ip
                );
            }

        } catch (IOException e) {
            Estadisticas.registrarSinRespuesta(
                Estadisticas.MotivoSinRespuesta.ERROR_IO
            );
            long total = ERRORES_IO.incrementAndGet();
            if (total == 1 || total % 100 == 0) {
                System.err.println(
                    "[IO] Errores atendiendo solicitudes: " + total +
                    " | último error: " + e.getMessage()
                );
            }

        } finally {
            Estadisticas.finalizarSolicitud(
                id,
                ip,
                ruta,
                hilo
            );

            try {
                clienteSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    static long plazoLectura(long aceptadaNanos) {
        long desdeLlegada = aceptadaNanos + PLAZO_LECTURA_NANOS;
        long minimo = System.nanoTime() + GRACIA_TRAS_COLA_NANOS;
        return desdeLlegada - minimo > 0 ? desdeLlegada : minimo;
    }

    static void responderSolicitudInvalida(
        Socket socket,
        String ip,
        SolicitudHttp.SolicitudInvalidaException error,
        boolean incluirCuerpo
    ) throws IOException {
        Estadisticas.registrarEvento(
            Estadisticas.TipoEvento.SOLICITUD_INVALIDA,
            ip,
            error.codigo() + " " + error.getMessage()
        );

        RespuestaHttp.html(
            error.codigo(),
            paginaError(error.codigo(), error.titulo(), error.getMessage())
        ).escribir(socket, incluirCuerpo);
    }

    static void enviarLimiteSolicitudes(
        Socket socket,
        long retryAfterSegundos,
        boolean incluirCuerpo
    ) throws IOException {
        RespuestaHttp.html(
            429,
            paginaError(
                429,
                "Demasiadas solicitudes",
                "Se alcanzó el límite temporal de solicitudes. Espera un momento y vuelve a intentarlo."
            )
        )
            .cabecera("Retry-After", Long.toString(Math.max(1L, retryAfterSegundos)))
            .cabecera("Cache-Control", "no-store")
            .escribir(socket, incluirCuerpo);
    }

    private static void enviarImagen(
        String ruta,
        Socket socket,
        boolean incluirCuerpo
    ) throws IOException {

        String nombre = ruta.substring(
            (Rutas.IMAGENES + "/").length()
        );

        if (
            nombre.isBlank() ||
            nombre.contains("/") ||
            nombre.contains("\\") ||
            nombre.equals(".") ||
            nombre.equals("..")
        ) {
            RespuestaHttp.html(404, paginaError("Imagen no encontrada."))
                .escribir(socket, incluirCuerpo);
            return;
        }

        Path carpeta;
        try {
            carpeta = localizarCarpetaImagenes();
        } catch (IOException e) {
            System.err.println("[IMG] No se pudo localizar la carpeta imagenes: " + e.getMessage());
            RespuestaHttp.html(
                500,
                paginaError(500, "Error de configuración", "El servidor no encuentra la carpeta de imágenes.")
            ).escribir(socket, incluirCuerpo);
            return;
        }

        Path candidato = carpeta.resolve(nombre).normalize();

        if (
            !candidato.startsWith(carpeta) ||
            !Files.isRegularFile(candidato)
        ) {
            Path alternativo = buscarNombreSinDistinguirMayusculas(carpeta, nombre);
            if (alternativo != null) {
                candidato = alternativo;
            } else {
                long total = IMAGENES_NO_ENCONTRADAS.incrementAndGet();
                if (total == 1 || total % 100 == 0) {
                    System.err.println(
                        "[IMG] Imágenes no encontradas: " + total +
                        " | última: " + nombre + " | carpeta: " + carpeta
                    );
                }
                RespuestaHttp.html(404, paginaError("Imagen no encontrada: " + nombre))
                    .escribir(socket, incluirCuerpo);
                return;
            }
        }

        Path archivo = candidato.toRealPath();

        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            RespuestaHttp.html(404, paginaError("Imagen no encontrada."))
                .escribir(socket, incluirCuerpo);
            return;
        }

        String tipo = tipoContenidoImagen(nombre);

        if (tipo == null) {
            RespuestaHttp.html(
                415,
                paginaError(
                    415,
                    "Formato de imagen no compatible",
                    "Usa PNG, JPG, JPEG, WEBP, GIF, BMP, AVIF o ICO."
                )
            ).escribir(socket, incluirCuerpo);
            return;
        }

        long tamano = Files.size(archivo);

        if (tamano <= 0) {
            RespuestaHttp.html(404, paginaError("La imagen está vacía o no se puede leer."))
                .escribir(socket, incluirCuerpo);
            return;
        }

        if (tamano > MAX_TAMANO_IMAGEN_BYTES) {
            RespuestaHttp.html(
                413,
                paginaError(
                    413,
                    "Imagen demasiado grande",
                    "El límite de tamaño por imagen es 8 MiB."
                )
            ).escribir(socket, incluirCuerpo);
            return;
        }

        RespuestaHttp respuesta = RespuestaHttp.nueva(200).tipo(tipo);

        byte[] imagen = incluirCuerpo ? obtenerImagenCacheada(archivo, tamano) : null;
        if (imagen != null) {
            respuesta.cuerpo(imagen);
        } else {
            respuesta.archivo(archivo, tamano);
        }

        respuesta
            .cabecera("X-Content-Type-Options", "nosniff")
            .cabecera("Cache-Control", "public, max-age=3600")
            .escribir(socket, incluirCuerpo);
    }

    private static byte[] obtenerImagenCacheada(Path archivo, long tamanoArchivo)
        throws IOException {

        byte[] existente = CACHE_IMAGENES.get(archivo);
        if (existente != null) {
            return existente;
        }

        if (tamanoArchivo > MAX_CACHE_IMAGENES_BYTES ||
            bytesCacheImagenes + tamanoArchivo > MAX_CACHE_IMAGENES_BYTES) {
            return null;
        }

        synchronized (BLOQUEO_CACHE_IMAGENES) {
            existente = CACHE_IMAGENES.get(archivo);
            if (existente != null) {
                return existente;
            }

            if (tamanoArchivo > MAX_CACHE_IMAGENES_BYTES ||
                bytesCacheImagenes + tamanoArchivo > MAX_CACHE_IMAGENES_BYTES) {
                return null;
            }

            byte[] datos = Files.readAllBytes(archivo);
            if (datos.length > MAX_CACHE_IMAGENES_BYTES ||
                bytesCacheImagenes + datos.length > MAX_CACHE_IMAGENES_BYTES) {
                return null;
            }

            CACHE_IMAGENES.put(archivo, datos);
            bytesCacheImagenes += datos.length;
            return datos;
        }
    }

    private static Path localizarCarpetaImagenes() throws IOException {
        Path cache = carpetaImagenesCache;
        if (cache != null && Files.isDirectory(cache)) {
            return cache;
        }

        List<Path> candidatos = new ArrayList<>();

        String configurada = System.getProperty("nexus.imagenes.dir");
        if (configurada != null && !configurada.isBlank()) {
            agregarCandidato(candidatos, Paths.get(configurada));
        }

        Path directorioActual = Paths.get("").toAbsolutePath().normalize();
        agregarCandidato(candidatos, directorioActual.resolve("imagenes"));

        try {
            Path ubicacionClases = Paths.get(
                ManejadorCliente.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            ).toAbsolutePath().normalize();

            Path base = Files.isDirectory(ubicacionClases)
                ? ubicacionClases
                : ubicacionClases.getParent();

            for (int nivel = 0; base != null && nivel < 4; nivel++) {
                agregarCandidato(candidatos, base.resolve("imagenes"));
                base = base.getParent();
            }
        } catch (Exception e) {
            System.err.println("[IMG] No se pudo inspeccionar la ubicación de las clases: " + e.getMessage());
        }

        for (Path candidato : candidatos) {
            if (Files.isDirectory(candidato)) {
                Path real = candidato.toRealPath();
                carpetaImagenesCache = real;
                System.out.println("[IMG] Directorio de imágenes: " + real);
                return real;
            }
        }

        throw new IOException(
            "No se encontró la carpeta 'imagenes'. Directorio actual: " +
            directorioActual + ". Rutas probadas: " + candidatos
        );
    }

    private static void agregarCandidato(List<Path> candidatos, Path ruta) {
        Path normalizada = ruta.toAbsolutePath().normalize();
        if (!candidatos.contains(normalizada)) {
            candidatos.add(normalizada);
        }
    }

    private static Path buscarNombreSinDistinguirMayusculas(
        Path carpeta,
        String nombre
    ) throws IOException {
        try (var archivos = Files.list(carpeta)) {
            List<Path> coincidencias = archivos
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equalsIgnoreCase(nombre))
                .limit(2)
                .toList();

            if (coincidencias.size() == 1) {
                long total = NOMBRES_CORREGIDOS.incrementAndGet();
                if (total == 1 || total % 100 == 0) {
                    System.err.println(
                        "[IMG] Nombres corregidos por mayúsculas/minúsculas: " + total +
                        " | último: " + nombre + " -> " + coincidencias.get(0).getFileName()
                    );
                }
                return coincidencias.get(0).normalize();
            }
            return null;
        }
    }

    private static String tipoContenidoImagen(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return null;
        }

        String extension = nombre.substring(punto).toLowerCase(Locale.ROOT);

        return switch (extension) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg", ".jpe", ".jfif" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".avif" -> "image/avif";
            case ".ico" -> "image/x-icon";
            default -> null;
        };
    }

    private static String paginaError(String mensaje) {
        return paginaError(404, "Página no encontrada", mensaje);
    }

    static String paginaError(
        int codigo,
        String titulo,
        String mensaje
    ) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="icon" href="data:,">
                <title>%s | Nexus UMG</title>
            </head>
            <body>
                <h1>%d - %s</h1>
                <p>%s</p>
                <a href="/">Volver al inicio</a>
            </body>
            </html>
            """.formatted(escaparHtml(titulo), codigo, escaparHtml(titulo), escaparHtml(mensaje));
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