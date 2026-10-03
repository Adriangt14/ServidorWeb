import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestorHilos {

    private ExecutorService threadPool;

    public GestorHilos(int cantidadHilos) {
        threadPool =
            Executors.newFixedThreadPool(cantidadHilos);
    }

    public void ejecutar(Socket clienteSocket) {

        String ip =
            clienteSocket.getInetAddress().getHostAddress();

        String id =
            Estadisticas.registrarSolicitud(ip);

        threadPool.execute(() -> {

            ManejadorCliente.atender(
                clienteSocket,
                id
            );
        });
    }

    public void cerrar() {
        threadPool.shutdown();
    }
}