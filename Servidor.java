import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    private static final int PUERTO = 8080;

    private static final int HILOS = 10;

    private static final int COLA_MAXIMA = 100;

    private static final int BACKLOG = 100;

    public static void main(String[] args) {

        GestorHilos gestorHilos =
            new GestorHilos(
                HILOS,
                COLA_MAXIMA
            );

        System.out.println(
            "Servidor iniciado en " +
            "http://127.0.0.1:" +
            PUERTO
        );

        System.out.println(
            "Thread Pool: " +
            HILOS +
            " hilos"
        );

        System.out.println(
            "Cola maxima: " +
            COLA_MAXIMA +
            " solicitudes"
        );

        System.out.println(
            "Esperando clientes..."
        );

        Runtime.getRuntime().addShutdownHook(
            new Thread(
                gestorHilos::cerrar
            )
        );

        try (
            ServerSocket serverSocket =
                new ServerSocket(
                    PUERTO,
                    BACKLOG,
                    InetAddress.getLoopbackAddress()
                )
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
                "Error en el servidor: " +
                e.getMessage()
            );

        } finally {

            gestorHilos.cerrar();
        }
    }
}