import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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

            PrintWriter salida = new PrintWriter(
                clienteSocket.getOutputStream(),
                true
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
                            "[" + nombreHilo + "] Producto solicitado: "
                            + ruta.substring("/producto/".length())
                        );
                    }

                    String pagina = PaginaWeb.generar(
                        ruta,
                        nombreHilo
                    );

                    enviarRespuesta(salida, "HTTP/1.1 200 OK", pagina);

                } else {

                    System.out.println(
                        "[" + nombreHilo + "] 404: " + ruta
                    );

                    String pagina404 = PaginaWeb.error404(
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

    private static boolean esProductoValido(String ruta) {

        if (!ruta.startsWith("/producto/")) {
            return false;
        }

        String id = ruta.substring("/producto/".length());

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

        int longitud = pagina.getBytes(StandardCharsets.UTF_8).length;

        salida.println(estado);
        salida.println("Content-Type: text/html; charset=UTF-8");
        salida.println("Content-Length: " + longitud);
        salida.println("Connection: close");
        salida.println();
        salida.println(pagina);
    }
}
