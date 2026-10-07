import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ManejadorCliente {

    public static void atender(Socket clienteSocket) {

        String nombreHilo = Thread.currentThread().getName();

        System.out.println(
            "[" + nombreHilo + "] Cliente conectado"
        );

        try {

            BufferedReader entrada = new BufferedReader(
                new InputStreamReader(
                    clienteSocket.getInputStream()
                )
            );

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
                        ruta,
                        clienteSocket
                    );

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
            }

            clienteSocket.close();

            System.out.println(
                "[" + nombreHilo + "] Cliente desconectado"
            );

        } catch (IOException e) {

            System.err.println(
                "Error atendiendo cliente: "
                + e.getMessage()
            );
        }
    }

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
        salida.flush();
    }
}