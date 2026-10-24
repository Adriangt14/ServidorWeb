import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PruebaSolicitudHttp {
    static int fallos = 0;

    static String procesar(ServerSocket servidor, String crudo, long pausaMs, String despues) throws Exception {
        Thread cliente = new Thread(() -> {
            try (Socket s = new Socket("127.0.0.1", servidor.getLocalPort())) {
                OutputStream o = s.getOutputStream();
                o.write(crudo.getBytes(StandardCharsets.ISO_8859_1)); o.flush();
                if (pausaMs > 0) { Thread.sleep(pausaMs); o.write(despues.getBytes(StandardCharsets.ISO_8859_1)); o.flush(); }
                Thread.sleep(200);
            } catch (Exception ignored) { }
        });
        cliente.start();
        try (Socket s = servidor.accept()) {
            SolicitudHttp sol = SolicitudHttp.leer(s, 1000);
            sol.interpretarLinea();
            sol.interpretarRuta();
            byte[] cuerpo = sol.leerCuerpo();
            return "OK cuerpo=" + new String(cuerpo, StandardCharsets.UTF_8) + " ruta=" + sol.ruta() + " consulta=" + sol.consulta()
                + " cookie=" + sol.cookie("sesion");
        } catch (SolicitudHttp.SolicitudInvalidaException e) {
            return "ERROR " + e.codigo() + " " + e.getMessage();
        } catch (Exception e) {
            return "EXC " + e.getClass().getSimpleName() + " " + e.getMessage();
        } finally {
            cliente.join();
        }
    }

    static void caso(String nombre, String real, String esperadoComienza) {
        boolean ok = real.startsWith(esperadoComienza);
        if (!ok) fallos++;
        System.out.printf("%-34s %s  -> %s%n", nombre, ok ? "OK   " : "FALLO", real);
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket srv = new ServerSocket(0)) {
            caso("POST con cuerpo y cookie", procesar(srv,
                "POST /admin/login?x=1 HTTP/1.1\r\nCookie: tema=oscuro; sesion=abc123 ; otra=1\r\nContent-Length: 11\r\n\r\nusuario=ana", 0, ""),
                "OK cuerpo=usuario=ana ruta=/admin/login consulta=x=1 cookie=abc123");
            caso("sin Content-Length", procesar(srv, "POST / HTTP/1.1\r\n\r\nabc", 0, ""), "ERROR 411");
            caso("Transfer-Encoding", procesar(srv, "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n3\r\nabc\r\n0\r\n\r\n", 0, ""), "ERROR 501");
            caso("Content-Length no numerico", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 1e3\r\n\r\n", 0, ""), "ERROR 400");
            caso("Content-Length negativo", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: -5\r\n\r\n", 0, ""), "ERROR 400");
            caso("cuerpo de 9000 bytes (>8 KB)", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 9000\r\n\r\n", 0, ""), "ERROR 413");
            caso("Content-Length duplicado", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 3\r\nContent-Length: 5\r\n\r\nabc", 0, ""), "ERROR 400 Cabecera content-length duplicada");
            caso("cuerpo incompleto (cierra antes)", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc", 0, ""), "EXC IOException Cuerpo HTTP incompleto");
            caso("cuerpo lento (supera plazo 1 s)", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 6\r\n\r\nabc", 1500, "def"), "EXC SocketTimeoutException");
            caso("cuerpo vacio Content-Length: 0", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 0\r\n\r\n", 0, ""), "OK cuerpo= ruta=/ consulta=null cookie=null");
            caso("cuerpo en dos partes (a tiempo)", procesar(srv, "POST / HTTP/1.1\r\nContent-Length: 6\r\n\r\nabc", 300, "def"), "OK cuerpo=abcdef");
        }
        System.out.println(fallos == 0 ? "RESULTADO: TODO OK" : "RESULTADO: " + fallos + " FALLOS");
        System.exit(fallos == 0 ? 0 : 1);
    }
}