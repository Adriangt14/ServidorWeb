import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ManejadorCliente {

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

        System.out.println(
            "[" + nombreHilo + "] "
            + "Solicitud #" + id
            + " iniciada"
        );

        try {

            BufferedReader entrada =
                new BufferedReader(
                    new InputStreamReader(
                        clienteSocket.getInputStream()
                    )
                );

            PrintWriter salida =
                new PrintWriter(
                    clienteSocket.getOutputStream(),
                    true
                );

            String peticion =
                entrada.readLine();

            System.out.println(
                "[" + nombreHilo + "] Petición: "
                + peticion
            );

            if (peticion != null) {

                if (peticion.startsWith("GET ")) {

                    String[] partes =
                        peticion.split(" ");

                    if (partes.length >= 2) {
                        ruta = partes[1];
                    }
                }

                Estadisticas.actualizarRuta(
                    id,
                    ruta
                );

                System.out.println(
                    "[" + nombreHilo + "] Ruta solicitada: "
                    + ruta
                );

                boolean paginaValida =
                    ruta.equals("/") ||
                    ruta.equals("/productos") ||
                    ruta.equals("/solicitudes") ||
                    ruta.equals("/hilos") ||
                    ruta.equals("/clientes") ||
                    esProductoValido(ruta);

                if (paginaValida) {

                    if (ruta.startsWith("/producto/")) {

                        System.out.println(
                            "[" + nombreHilo + "] "
                            + "Producto solicitado: "
                            + ruta.substring(
                                "/producto/".length()
                            )
                        );
                    }

                    String pagina =
                        PaginaWeb.generar(
                            ruta,
                            nombreHilo
                        );

                    enviarRespuesta(
                        salida,
                        "HTTP/1.1 200 OK",
                        pagina
                    );

                } else {

                    System.out.println(
                        "[" + nombreHilo + "] 404: "
                        + ruta
                    );

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
                }
            }

        } catch (IOException e) {

            System.err.println(
                "[" + nombreHilo + "] Error atendiendo cliente: "
                + e.getMessage()
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
                    "Error cerrando conexión: "
                    + e.getMessage()
                );
            }

            System.out.println(
                "[" + nombreHilo + "] "
                + "Cliente desconectado"
            );
        }
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

        salida.println(pagina);
    }
}