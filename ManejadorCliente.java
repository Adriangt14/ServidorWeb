import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

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

                if (
                    ruta.equals("/") ||
                    ruta.equals("/solicitudes") ||
                    ruta.equals("/hilos") ||
                    ruta.equals("/clientes")
                ) {

                    String pagina = PaginaWeb.generar(
                        ruta,
                        nombreHilo
                    );

                    salida.println("HTTP/1.1 200 OK");
                    salida.println(
                        "Content-Type: text/html; charset=UTF-8"
                    );
                    salida.println(
                        "Content-Length: " +
                        pagina.getBytes().length
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
}