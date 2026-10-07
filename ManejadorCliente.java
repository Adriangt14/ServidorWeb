import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
<<<<<<< HEAD
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
=======
import java.net.Socket;
>>>>>>> 173514d705126cc26753a3b63ee579bc05c6f1c8
import java.nio.charset.StandardCharsets;

public class ManejadorCliente {

    private static final int TIMEOUT_LECTURA_MS = 5_000;
    private static final int MAX_LINEA_HTTP = 4_096;

    public static void atender(
        Socket clienteSocket,
        String id
    ) {

        String nombreHilo =
            Thread.currentThread().getName();

        String ip =
            clienteSocket.getInetAddress()
                .getHostAddress();

        String ruta = "/";

        Estadisticas.iniciarSolicitud(id);

        try {

            clienteSocket.setSoTimeout(
                TIMEOUT_LECTURA_MS
            );

<<<<<<< HEAD
            String peticion = entrada.readLine();

            System.out.println(
                "[" + nombreHilo + "] Petición: " + peticion
            );

            if (peticion != null) {

                String ruta = "/";

                if (peticion.startsWith("GET ")) {

                    String[] partes = peticion.split(" ");

                    if (partes.length >= 2) {
                        ruta = partes[1];
                    }
                }

                System.out.println(
                    "[" + nombreHilo + "] Ruta solicitada: " + ruta
                );

                if (ruta.startsWith("/imagenes/")) {

                    enviarImagen(
=======
            BufferedReader entrada =
                new BufferedReader(
                    new InputStreamReader(
                        clienteSocket.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                );

            PrintWriter salida =
                new PrintWriter(
                    clienteSocket.getOutputStream(),
                    true,
                    StandardCharsets.UTF_8
                );

            String peticion =
                leerLineaLimitada(
                    entrada,
                    MAX_LINEA_HTTP
                );

            if (peticion == null || peticion.isBlank()) {
                return;
            }

            if (!peticion.startsWith("GET ")) {

                enviarRespuesta(
                    salida,
                    "HTTP/1.1 400 Bad Request",
                    PaginaWeb.error404(
                        "Solicitud no soportada",
                        nombreHilo
                    )
                );

                return;
            }

            String[] partes =
                peticion.split(" ", 3);

            if (partes.length < 2) {

                enviarRespuesta(
                    salida,
                    "HTTP/1.1 400 Bad Request",
                    PaginaWeb.error404(
                        "Solicitud invalida",
                        nombreHilo
                    )
                );

                return;
            }

            ruta = partes[1];

            Estadisticas.actualizarRuta(
                id,
                ruta
            );

            boolean paginaValida =
                ruta.equals("/") ||
                ruta.equals("/productos") ||
                ruta.equals("/solicitudes") ||
                ruta.equals("/hilos") ||
                ruta.equals("/clientes") ||
                esProductoValido(ruta);

            if (paginaValida) {

                String pagina =
                    PaginaWeb.generar(
>>>>>>> 173514d705126cc26753a3b63ee579bc05c6f1c8
                        ruta,
                        clienteSocket
                    );

<<<<<<< HEAD
                } else {

                    PrintWriter salida = new PrintWriter(
                        clienteSocket.getOutputStream(),
                        true
                    );

                    if (
                        ruta.equals("/") ||
                        ruta.equals("/productos") ||
                        ruta.equals("/solicitudes") ||
                        ruta.equals("/hilos") ||
                        ruta.equals("/clientes")
                    ) {

                        String pagina = PaginaWeb.generar(
                            ruta,
                            nombreHilo
                        );

                        byte[] paginaBytes = pagina.getBytes(
                            StandardCharsets.UTF_8
                        );

                        salida.println("HTTP/1.1 200 OK");
                        salida.println(
                            "Content-Type: text/html; charset=UTF-8"
                        );
                        salida.println(
                            "Content-Length: " + paginaBytes.length
                        );
                        salida.println("Connection: close");
                        salida.println();

                        salida.println(pagina);

                    } else {

                        salida.println(
                            "HTTP/1.1 404 Not Found"
                        );

                        salida.println(
                            "Content-Type: text/html; charset=UTF-8"
                        );

                        salida.println(
                            "Connection: close"
                        );

                        salida.println();

                        salida.println(
                            "<html><body>"
                        );

                        salida.println(
                            "<h2>404 - Página no encontrada</h2>"
                        );

                        salida.println(
                            "<p>Ruta solicitada: " +
                            ruta +
                            "</p>"
                        );

                        salida.println(
                            "</body></html>"
                        );
                    }
                }
=======
                enviarRespuesta(
                    salida,
                    "HTTP/1.1 200 OK",
                    pagina
                );

            } else {

                String pagina404 =
                    PaginaWeb.error404(
                        ruta,
                        nombreHilo
                    );

                enviarRespuesta(
                    salida,
                    "HTTP/1.1 404 Not Found",
                    pagina404
                );
>>>>>>> 173514d705126cc26753a3b63ee579bc05c6f1c8
            }

        } catch (java.net.SocketTimeoutException e) {

            System.err.println(
                "[" + nombreHilo +
                "] Timeout leyendo cliente: " +
                ip
            );

        } catch (IOException e) {

            System.err.println(
                "[" + nombreHilo +
                "] Error atendiendo cliente: " +
                e.getMessage()
            );

        } finally {

            Estadisticas.finalizarSolicitud(
                id,
                ip,
                ruta,
                nombreHilo
            );

            try {

                clienteSocket.close();

            } catch (IOException e) {

                System.err.println(
                    "Error cerrando conexión: " +
                    e.getMessage()
                );
            }
        }
    }

<<<<<<< HEAD
    private static void enviarImagen(
        String ruta,
        Socket clienteSocket
    ) throws IOException {

        String nombreArchivo = ruta.substring(
            "/imagenes/".length()
        );

        Path carpetaImagenes = Paths.get(
            "imagenes"
        ).toAbsolutePath().normalize();

        Path archivo = carpetaImagenes
            .resolve(nombreArchivo)
            .normalize();

        OutputStream salida =
            clienteSocket.getOutputStream();

        if (
            !archivo.startsWith(carpetaImagenes) ||
            !Files.exists(archivo) ||
            !nombreArchivo.toLowerCase().endsWith(".png")
        ) {

            String respuesta =
                "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                "Imagen no encontrada";

            salida.write(
                respuesta.getBytes(
                    StandardCharsets.UTF_8
                )
            );

            salida.flush();

            return;
        }

        byte[] imagen = Files.readAllBytes(archivo);

        String encabezados =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: image/png\r\n" +
            "Content-Length: " + imagen.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

        salida.write(
            encabezados.getBytes(
                StandardCharsets.UTF_8
            )
        );

        salida.write(imagen);
=======
    private static String leerLineaLimitada(
        BufferedReader entrada,
        int maxLongitud
    ) throws IOException {

        StringBuilder linea =
            new StringBuilder();

        int caracter;

        while ((caracter = entrada.read()) != -1) {

            if (caracter == '\n') {
                break;
            }

            if (caracter == '\r') {
                continue;
            }

            if (linea.length() >= maxLongitud) {
                throw new IOException(
                    "Linea HTTP demasiado larga"
                );
            }

            linea.append((char) caracter);
        }

        if (
            caracter == -1 &&
            linea.length() == 0
        ) {
            return null;
        }

        return linea.toString();
    }

    private static boolean esProductoValido(
        String ruta
    ) {

        if (!ruta.startsWith("/producto/")) {
            return false;
        }

        String id =
            ruta.substring(
                "/producto/".length()
            );

        return id.equals("1") ||
               id.equals("2") ||
               id.equals("3") ||
               id.equals("4") ||
               id.equals("5") ||
               id.equals("6") ||
               id.equals("7");
    }

    private static void enviarRespuesta(
        PrintWriter salida,
        String estado,
        String pagina
    ) {

        int longitud =
            pagina.getBytes(
                StandardCharsets.UTF_8
            ).length;

        salida.println(estado);

        salida.println(
            "Content-Type: text/html; charset=UTF-8"
        );

        salida.println(
            "Content-Length: " + longitud
        );

        salida.println(
            "Connection: close"
        );

        salida.println();

        salida.print(pagina);
>>>>>>> 173514d705126cc26753a3b63ee579bc05c6f1c8
        salida.flush();
    }
}