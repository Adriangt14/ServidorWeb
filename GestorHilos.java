import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class GestorHilos {

    private static final int CAPACIDAD_COLA_POR_DEFECTO = 100;
    private static final int TIEMPO_CIERRE_GRACIOSO_SEGUNDOS = 5;
    private static final int TIEMPO_CIERRE_FORZOSO_SEGUNDOS = 2;

    private static final byte[] RESPUESTA_SATURACION = (
        "HTTP/1.1 503 Service Unavailable\r\n" +
        "Content-Type: text/html; charset=UTF-8\r\n" +
        "Content-Length: 143\r\n" +
        "Retry-After: 1\r\n" +
        "Cache-Control: no-store\r\n" +
        "X-Content-Type-Options: nosniff\r\n" +
        "Connection: close\r\n\r\n" +
        "<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><title>503</title>" +
        "<h1>503 - Servidor ocupado</h1><p>Intenta de nuevo en unos segundos.</p>"
    ).getBytes(StandardCharsets.UTF_8);

    private final ThreadPoolExecutor threadPool;
    private final Set<Socket> socketsControlados =
        ConcurrentHashMap.newKeySet();
    private final AtomicBoolean cierreIniciado =
        new AtomicBoolean(false);
    private final AtomicLong rechazosPorSaturacion =
        new AtomicLong();

    public GestorHilos(int cantidadHilos) {
        this(cantidadHilos, CAPACIDAD_COLA_POR_DEFECTO);
    }

    public GestorHilos(int cantidadHilos, int capacidadCola) {
        if (cantidadHilos < 1) {
            throw new IllegalArgumentException(
                "La cantidad de hilos debe ser mayor que cero."
            );
        }

        if (capacidadCola < 1) {
            throw new IllegalArgumentException(
                "La capacidad de cola debe ser mayor que cero."
            );
        }

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
        if (clienteSocket == null) {
            return false;
        }

        String ip =
            clienteSocket.getInetAddress().getHostAddress();

        String id = Estadisticas.registrarSolicitud(ip);
        socketsControlados.add(clienteSocket);

        try {
            threadPool.execute(
                new TareaCliente(clienteSocket, id)
            );
            return true;

        } catch (RejectedExecutionException e) {
            Estadisticas.rechazarSolicitud(id);
            responderServicioNoDisponible(clienteSocket);
            socketsControlados.remove(clienteSocket);
            return false;
        }
    }

    public void cerrar() {
        if (!cierreIniciado.compareAndSet(false, true)) {
            return;
        }

        threadPool.shutdown();

        try {
            if (!threadPool.awaitTermination(
                    TIEMPO_CIERRE_GRACIOSO_SEGUNDOS,
                    TimeUnit.SECONDS)) {

                List<Runnable> pendientes =
                    threadPool.shutdownNow();

                for (Runnable pendiente : pendientes) {
                    if (pendiente instanceof TareaCliente tarea) {
                        tarea.cancelarPendiente();
                    }
                }

                cerrarSocketsControlados();

                if (!threadPool.awaitTermination(
                        TIEMPO_CIERRE_FORZOSO_SEGUNDOS,
                        TimeUnit.SECONDS)) {
                    System.err.println(
                        "[SERVIDOR] Algunos hilos no terminaron tras el cierre forzoso."
                    );
                }
            }

        } catch (InterruptedException e) {
            List<Runnable> pendientes =
                threadPool.shutdownNow();

            for (Runnable pendiente : pendientes) {
                if (pendiente instanceof TareaCliente tarea) {
                    tarea.cancelarPendiente();
                }
            }

            cerrarSocketsControlados();
            Thread.currentThread().interrupt();
        }
    }

    private void responderServicioNoDisponible(
        Socket clienteSocket
    ) {
        long total = rechazosPorSaturacion.incrementAndGet();

        if (total == 1 || total % 100 == 0) {
            System.err.println(
                "[SOBRECARGA] Solicitudes rechazadas por pool/cola llena: "
                + total
            );
        }

        try (
            Socket socket = clienteSocket;
            OutputStream salida = socket.getOutputStream()
        ) {
            socket.setTcpNoDelay(true);
            salida.write(RESPUESTA_SATURACION);
            salida.flush();

        } catch (IOException ignored) {
        }
    }

    private void cerrarSocketsControlados() {
        for (Socket socket : socketsControlados) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        socketsControlados.clear();
    }

    private final class TareaCliente implements Runnable {
        private final Socket socket;
        private final String id;
        private final AtomicBoolean terminada =
            new AtomicBoolean(false);

        private TareaCliente(Socket socket, String id) {
            this.socket = socket;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                ManejadorCliente.atender(socket, id);
            } finally {
                socketsControlados.remove(socket);
                terminada.set(true);
            }
        }

        private void cancelarPendiente() {
            if (terminada.compareAndSet(false, true)) {
                Estadisticas.rechazarSolicitud(id);
                socketsControlados.remove(socket);

                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}