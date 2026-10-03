import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestorHilos {

    private ExecutorService threadPool;

    public GestorHilos(int cantidadHilos) {
        threadPool = Executors.newFixedThreadPool(cantidadHilos);
    }

    public void ejecutar(Runnable tarea) {
        threadPool.execute(tarea);
    }

    public void cerrar() {
        threadPool.shutdown();
    }
}