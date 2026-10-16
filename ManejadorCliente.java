import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ManejadorCliente {

    private static final int TIMEOUT_LECTURA_MS = 5000;
    private static final int MAX_LINEA_HTTP = 4096;
    private static final int MAX_CABECERAS_HTTP = 50;
    private static final int MAX_BYTES_CABECERAS = 16_384;
    private static final long MAX_TAMANO_IMAGEN_BYTES = 8L * 1024 * 1024;


    public static void atender(Socket clienteSocket, String id) {

        String hilo = Thread.currentThread().getName();
        String ipConexion = clienteSocket.getInetAddress().getHostAddress();
        String ip = ipConexion;
        String ruta = Rutas.INICIO;

        Estadisticas.iniciarSolicitud(id);

        try {

            clienteSocket.setSoTimeout(TIMEOUT_LECTURA_MS);

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(
                    clienteSocket.getInputStream(),
                    StandardCharsets.ISO_8859_1
                )
            );

            // Un solo plazo total evita que un cliente extienda indefinidamente
            // la lectura enviando una cabecera cada varios segundos.
            long plazoLectura = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_LECTURA_MS);

            String peticion = leerLineaLimitada(
                entrada,
                clienteSocket,
                plazoLectura
            );

            if (peticion == null || peticion.isBlank()) {
                return;
            }

            Map<String, String> cabeceras;
            try {
                cabeceras = leerCabecerasLimitadas(
                    entrada,
                    clienteSocket,
                    plazoLectura
                );
            } catch (CabecerasInvalidasException e) {
                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 400 Bad Request",
                    paginaError(400, "Solicitud no válida", e.getMessage())
                );
                return;
            }

            // Solo se acepta CF-Connecting-IP cuando la conexión viene de
            // loopback, porque el servidor está ligado a la interfaz local y
            // recibe tráfico público únicamente mediante Cloudflare Tunnel.
            ip = obtenerIpCliente(clienteSocket, cabeceras, ipConexion);
            Estadisticas.actualizarIp(id, ip);

            LimitadorSolicitudes.Resultado decision =
                LimitadorSolicitudes.comprobar(ip);

            if (!decision.permitido()) {
                Estadisticas.actualizarRuta(id, Rutas.BLOQUEADO);
                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 429 Too Many Requests",
                    paginaError(
                        429,
                        "Demasiadas solicitudes",
                        "Se alcanzó el límite temporal de solicitudes. Espera un momento y vuelve a intentarlo."
                    ),
                    decision.retryAfterSegundos()
                );
                return;
            }

            String[] partes = peticion.trim().split("\\s+");

            if (
                partes.length != 3 ||
                !partes[2].matches("HTTP/1\\.[01]")
            ) {

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 400 Bad Request",
                    paginaError(400, "Solicitud HTTP no válida", "La petición recibida no tiene el formato esperado.")
                );

                return;
            }

            if (!partes[0].equals("GET")) {

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 405 Method Not Allowed",
                    paginaError(405, "Método no permitido", "Este servidor solo admite solicitudes GET.")
                );

                return;
            }

            try {

                URI uri = new URI(partes[1]);

                ruta = uri.getPath();

                if (ruta == null || !ruta.startsWith("/")) {
                    ruta = "";
                }

            } catch (URISyntaxException e) {

                ruta = "";

                Estadisticas.actualizarRuta(
                    id,
                    Rutas.BLOQUEADO
                );

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 400 Bad Request",
                    paginaError(400, "Ruta no válida", "La ruta solicitada no tiene un formato válido.")
                );

                return;
            }

            if (Rutas.esRutaProtegida(ruta)) {

                Estadisticas.actualizarRuta(
                    id,
                    Rutas.BLOQUEADO
                );

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 404 Not Found",
                    paginaError("Recurso no encontrado.")
                );

                return;
            }

            Estadisticas.actualizarRuta(id, ruta);

            if (Rutas.esImagen(ruta)) {
                enviarImagen(ruta, clienteSocket);
                return;
            }

            if (!Rutas.esPaginaValida(ruta)) {

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 404 Not Found",
                    paginaError("La página solicitada no existe.")
                );

                return;
            }

            String pagina = PaginaWeb.generar(ruta, hilo);

            enviarTexto(
                clienteSocket,
                "HTTP/1.1 200 OK",
                pagina
            );

        } catch (SocketTimeoutException e) {

            System.err.println(
                "[" + hilo + "] Tiempo de lectura agotado: " + ip
            );

        } catch (IOException e) {

            System.err.println(
                "[" + hilo + "] Error atendiendo solicitud: " +
                e.getMessage()
            );

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

    private static String leerLineaLimitada(
        BufferedReader entrada,
        Socket socket,
        long limite
    ) throws IOException {

        StringBuilder linea = new StringBuilder();

        while (true) {

            long restante = limite - System.nanoTime();

            if (restante <= 0) {
                throw new SocketTimeoutException(
                    "Tiempo máximo para recibir la petición excedido."
                );
            }

            long timeoutMs =
                (restante + 999_999L) / 1_000_000L;

            socket.setSoTimeout(
                (int) Math.max(1L, timeoutMs)
            );

            int caracter = entrada.read();

            if (System.nanoTime() - limite >= 0) {
                throw new SocketTimeoutException(
                    "Tiempo máximo para recibir la petición excedido."
                );
            }

            if (caracter == -1) {

                if (linea.isEmpty()) {
                    return null;
                }

                throw new IOException(
                    "Petición HTTP incompleta."
                );
            }

            if (caracter == '\n') {
                return linea.toString();
            }

            if (caracter == '\r') {
                continue;
            }

            if (linea.length() >= MAX_LINEA_HTTP) {
                throw new IOException(
                    "Línea HTTP demasiado larga."
                );
            }

            linea.append((char) caracter);
        }
    }

    private static void enviarTexto(
        Socket socket,
        String estado,
        String pagina
    ) throws IOException {
        enviarTexto(socket, estado, pagina, 0);
    }

    private static void enviarTexto(
        Socket socket,
        String estado,
        String pagina,
        long retryAfterSegundos
    ) throws IOException {

        byte[] cuerpo =
            pagina.getBytes(StandardCharsets.UTF_8);

        String encabezados =
            estado + "\r\n" +
            "Content-Type: text/html; charset=UTF-8\r\n" +
            "Content-Length: " + cuerpo.length + "\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "X-Frame-Options: DENY\r\n" +
            "Referrer-Policy: no-referrer\r\n" +
            "Content-Security-Policy: default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'\r\n" +
            (retryAfterSegundos > 0 ? "Retry-After: " + retryAfterSegundos + "\r\n" : "") +
            (retryAfterSegundos > 0 ? "Cache-Control: no-store\r\n" : "") +
            "Connection: close\r\n\r\n";

        OutputStream salida = socket.getOutputStream();

        salida.write(
            encabezados.getBytes(StandardCharsets.UTF_8)
        );

        salida.write(cuerpo);
        salida.flush();
    }

    private static void enviarImagen(
        String ruta,
        Socket socket
    ) throws IOException {

        String nombre = ruta.substring(
            (Rutas.IMAGENES + "/").length()
        );

        // Solo se sirven archivos directamente dentro de /imagenes.
        // Se rechazan subdirectorios y nombres de ruta sospechosos.
        if (
            nombre.isBlank() ||
            nombre.contains("/") ||
            nombre.contains("\\") ||
            nombre.equals(".") ||
            nombre.equals("..")
        ) {
            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Imagen no encontrada.")
            );
            return;
        }

        Path carpetaBase = Paths.get("imagenes")
            .toAbsolutePath()
            .normalize();

        if (!Files.isDirectory(carpetaBase)) {
            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Imagen no encontrada.")
            );
            return;
        }

        Path carpeta = carpetaBase.toRealPath();
        Path candidato = carpeta.resolve(nombre).normalize();

        if (
            !candidato.startsWith(carpeta) ||
            !Files.isRegularFile(candidato)
        ) {
            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Imagen no encontrada.")
            );
            return;
        }

        Path archivo = candidato.toRealPath();

        // Impide servir enlaces simbólicos que salgan de la carpeta de imágenes.
        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Imagen no encontrada.")
            );
            return;
        }

        String tipo = tipoContenidoImagen(nombre);

        if (tipo == null) {
            enviarTexto(
                socket,
                "HTTP/1.1 415 Unsupported Media Type",
                paginaError(
                    415,
                    "Formato de imagen no compatible",
                    "Usa PNG, JPG, JPEG, WEBP, GIF, BMP, AVIF o ICO."
                )
            );
            return;
        }

        long tamano = Files.size(archivo);

        if (tamano <= 0) {
            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("La imagen está vacía o no se puede leer.")
            );
            return;
        }

        if (tamano > MAX_TAMANO_IMAGEN_BYTES) {
            enviarTexto(
                socket,
                "HTTP/1.1 413 Content Too Large",
                paginaError(
                    413,
                    "Imagen demasiado grande",
                    "El límite de tamaño por imagen es 8 MiB."
                )
            );
            return;
        }

        byte[] imagen = Files.readAllBytes(archivo);

        String encabezados =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: " + tipo + "\r\n" +
            "Content-Length: " + imagen.length + "\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Cache-Control: public, max-age=3600\r\n" +
            "Connection: close\r\n\r\n";

        OutputStream salida = socket.getOutputStream();
        salida.write(encabezados.getBytes(StandardCharsets.UTF_8));
        salida.write(imagen);
        salida.flush();
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

    private static String paginaError(
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

    private static Map<String, String> leerCabecerasLimitadas(
        BufferedReader entrada,
        Socket socket,
        long plazoLectura
    ) throws IOException {

        Map<String, String> cabeceras = new HashMap<>();
        int cantidad = 0;
        int bytesAproximados = 0;

        while (true) {
            String linea = leerLineaLimitada(entrada, socket, plazoLectura);
            if (linea == null) {
                throw new CabecerasInvalidasException("Cabeceras HTTP incompletas.");
            }
            if (linea.isEmpty()) {
                return cabeceras;
            }

            cantidad++;
            bytesAproximados += linea.length() + 2;
            if (cantidad > MAX_CABECERAS_HTTP || bytesAproximados > MAX_BYTES_CABECERAS) {
                throw new CabecerasInvalidasException("La solicitud contiene demasiadas cabeceras.");
            }

            if (linea.charAt(0) == ' ' || linea.charAt(0) == '\t') {
                throw new CabecerasInvalidasException("Formato de cabecera HTTP no válido.");
            }

            int separador = linea.indexOf(':');
            if (separador <= 0) {
                throw new CabecerasInvalidasException("Formato de cabecera HTTP no válido.");
            }

            String nombre = linea.substring(0, separador).trim().toLowerCase(Locale.ROOT);
            if (!nombre.matches("[!#$%&'*+.^_`|~0-9a-z-]+")) {
                throw new CabecerasInvalidasException("Nombre de cabecera HTTP no válido.");
            }

            String valor = linea.substring(separador + 1).trim();
            for (int i = 0; i < valor.length(); i++) {
                char c = valor.charAt(i);
                if ((c < 0x20 && c != '\t') || c == 0x7f) {
                    throw new CabecerasInvalidasException("Valor de cabecera HTTP no válido.");
                }
            }

            if (nombre.equals("cf-connecting-ip") && cabeceras.containsKey(nombre)) {
                throw new CabecerasInvalidasException("Cabecera CF-Connecting-IP duplicada.");
            }

            // Para cabeceras repetidas que no afectan a la seguridad, conservamos
            // el primer valor; CF-Connecting-IP nunca admite duplicados.
            cabeceras.putIfAbsent(nombre, valor);
        }
    }

    private static String obtenerIpCliente(
        Socket socket,
        Map<String, String> cabeceras,
        String ipConexion
    ) {
        if (!socket.getInetAddress().isLoopbackAddress()) {
            return ipConexion;
        }

        String ipCloudflare = cabeceras.get("cf-connecting-ip");
        String ipNormalizada = normalizarIp(ipCloudflare);
        return ipNormalizada != null ? ipNormalizada : ipConexion;
    }

    /** Valida direcciones IP numéricas sin permitir nombres de host ni DNS. */
    private static String normalizarIp(String candidata) {
        if (candidata == null || candidata.isBlank() || candidata.length() > 45) {
            return null;
        }

        String ip = candidata.trim();

        if (ip.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            String[] octetos = ip.split("\\.");
            StringBuilder canonica = new StringBuilder();
            for (int i = 0; i < octetos.length; i++) {
                int valor;
                try {
                    valor = Integer.parseInt(octetos[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (valor < 0 || valor > 255) {
                    return null;
                }
                if (i > 0) canonica.append('.');
                canonica.append(valor);
            }
            return canonica.toString();
        }

        // Se permiten únicamente caracteres de una dirección IPv6 literal.
        // El ':' impide que InetAddress intente resolver un nombre DNS.
        if (ip.indexOf(':') >= 0 && ip.matches("[0-9a-fA-F:.]+")) {
            try {
                InetAddress direccion = InetAddress.getByName(ip);
                return direccion.getHostAddress().toLowerCase(Locale.ROOT);
            } catch (UnknownHostException ignored) {
                return null;
            }
        }

        return null;
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

    private static final class CabecerasInvalidasException extends IOException {
        private static final long serialVersionUID = 1L;

        private CabecerasInvalidasException(String mensaje) {
            super(mensaje);
        }
    }
}
