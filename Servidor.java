import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class Servidor {

    public static void main(String[] args) {

        int puerto = 8080;

        GestorHilos gestorHilos =
            new GestorHilos(10);

        System.out.println(
            "Servidor iniciado en localhost:"
            + puerto
        );

        System.out.println(
            "Thread Pool: 10 hilos"
        );

        System.out.println(
            "Esperando clientes..."
        );

        try (
            ServerSocket serverSocket =
                new ServerSocket(puerto)
        ) {

            while (true) {

                Socket clienteSocket =
                    serverSocket.accept();

                gestorHilos.ejecutar(
                    clienteSocket
                );
            }

        } catch (IOException e) {

            System.err.println(
                "Error en el servidor: "
                + e.getMessage()
            );
        }
    }
}