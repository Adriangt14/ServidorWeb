import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GestorHilos {

    private static final int CAPACIDAD_COLA_POR_DEFECTO = 100;

    private final ThreadPoolExecutor threadPool;

    public GestorHilos(int cantidadHilos) {
        this(cantidadHilos, CAPACIDAD_COLA_POR_DEFECTO);
    }

    public GestorHilos(int cantidadHilos, int capacidadCola) {

        threadPool = new ThreadPoolExecutor(
            cantidadHilos,
            cantidadHilos,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(capacidadCola),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public boolean ejecutar(Socket clienteSocket) {

        String ip =
            clienteSocket.getInetAddress().getHostAddress();

        String id =
            Estadisticas.registrarSolicitud(ip);

        try {

            threadPool.execute(() ->
                ManejadorCliente.atender(
                    clienteSocket,
                    id
                )
            );

            return true;

        } catch (RejectedExecutionException e) {

            Estadisticas.rechazarSolicitud(id);

            try {
                clienteSocket.close();
            } catch (Exception ignored) {
            }

            return false;
        }
    }

    public void cerrar() {
        threadPool.shutdown();
    }
}