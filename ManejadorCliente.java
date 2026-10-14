import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

public class ManejadorCliente {

    private static final int TIMEOUT_LECTURA_MS = 5000;
    private static final int MAX_LINEA_HTTP = 4096;

    private static final Set<String> IMAGENES_PERMITIDAS = Set.of(
        "encabezado.jpeg",
        "Audifonos_Razer.webp",
        "Figura_Miku.png",
        "Figura_Garen_Pingu.webp",
        "Logitech_Pro.png",
        "Logitech_Basico_Teclado.png",
        "teclado.png",
        "Album_Nothingbutthebest.webp",
        "Album_Lavozdeoro.jpg"
    );

    public static void atender(Socket clienteSocket, String id) {

        String hilo = Thread.currentThread().getName();

        String ip = clienteSocket.getInetAddress().getHostAddress();

        String ruta = Rutas.INICIO;

        Estadisticas.iniciarSolicitud(id);

        try {

            clienteSocket.setSoTimeout(TIMEOUT_LECTURA_MS);

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(
                    clienteSocket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            String peticion = leerLineaLimitada(entrada);

            if (peticion == null || peticion.isBlank()) {
                return;
            }

            String[] partes = peticion.split("\\s+", 3);

            if (partes.length < 2) {

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 400 Bad Request",
                    paginaError("Solicitud no válida.")
                );

                return;
            }

            if (!partes[0].equals("GET")) {

                enviarTexto(
                    clienteSocket,
                    "HTTP/1.1 405 Method Not Allowed",
                    paginaError("Método HTTP no permitido.")
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
                    paginaError("Ruta no válida.")
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

        } catch (java.net.SocketTimeoutException e) {

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
        BufferedReader entrada
    ) throws IOException {

        StringBuilder linea = new StringBuilder();

        int caracter;

        while ((caracter = entrada.read()) != -1) {

            if (caracter == '\n') {
                break;
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

        if (caracter == -1 && linea.isEmpty()) {
            return null;
        }

        return linea.toString();
    }

    private static void enviarTexto(
        Socket socket,
        String estado,
        String pagina
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

        if (!IMAGENES_PERMITIDAS.contains(nombre)) {

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

        Path candidato = carpeta
            .resolve(nombre)
            .normalize();

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

        if (!archivo.startsWith(carpeta)) {

            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Imagen no encontrada.")
            );

            return;
        }

        String extension = "";

        int punto = nombre.lastIndexOf('.');

        if (punto >= 0) {
            extension = nombre.substring(punto).toLowerCase(Locale.ROOT);
        }

        String tipo = switch (extension) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            default -> null;
        };

        if (tipo == null) {

            enviarTexto(
                socket,
                "HTTP/1.1 404 Not Found",
                paginaError("Formato de imagen no permitido.")
            );

            return;
        }

        byte[] imagen = Files.readAllBytes(archivo);

        String encabezados =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: " + tipo + "\r\n" +
            "Content-Length: " + imagen.length + "\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Cache-Control: public, max-age=86400\r\n" +
            "Connection: close\r\n\r\n";

        OutputStream salida = socket.getOutputStream();

        salida.write(
            encabezados.getBytes(StandardCharsets.UTF_8)
        );

        salida.write(imagen);
        salida.flush();
    }

    private static String paginaError(String mensaje) {

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Nexus UMG</title>
            </head>
            <body>
                <h1>404 - Página no encontrada</h1>
                <p>%s</p>
                <a href="/">Volver al inicio</a>
            </body>
            </html>
            """.formatted(mensaje);
    }
}