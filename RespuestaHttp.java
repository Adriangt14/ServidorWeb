import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RespuestaHttp {

    public static final String POLITICA_CONTENIDO =
        "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'";

    private static final Pattern PATRON_NOMBRE_CABECERA =
        Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");

    private static final Map<Integer, String> RAZONES = Map.ofEntries(
        Map.entry(200, "OK"),
        Map.entry(204, "No Content"),
        Map.entry(303, "See Other"),
        Map.entry(400, "Bad Request"),
        Map.entry(401, "Unauthorized"),
        Map.entry(403, "Forbidden"),
        Map.entry(404, "Not Found"),
        Map.entry(405, "Method Not Allowed"),
        Map.entry(411, "Length Required"),
        Map.entry(413, "Content Too Large"),
        Map.entry(414, "URI Too Long"),
        Map.entry(415, "Unsupported Media Type"),
        Map.entry(429, "Too Many Requests"),
        Map.entry(431, "Request Header Fields Too Large"),
        Map.entry(500, "Internal Server Error"),
        Map.entry(501, "Not Implemented"),
        Map.entry(503, "Service Unavailable")
    );

    private final int codigo;
    private final List<String[]> cabeceras = new ArrayList<>();
    private String tipoContenido;
    private byte[] cuerpo = new byte[0];
    private Path archivo;
    private long longitudArchivo;

    private RespuestaHttp(int codigo) {
        if (!RAZONES.containsKey(codigo)) {
            throw new IllegalArgumentException("Código HTTP no admitido: " + codigo);
        }
        this.codigo = codigo;
    }

    public static RespuestaHttp nueva(int codigo) {
        return new RespuestaHttp(codigo);
    }

    public static RespuestaHttp html(int codigo, String pagina) {
        return html(codigo, pagina.getBytes(StandardCharsets.UTF_8));
    }

    public static RespuestaHttp html(int codigo, byte[] pagina) {
        return new RespuestaHttp(codigo)
            .tipo("text/html; charset=UTF-8")
            .cuerpo(pagina)
            .cabecera("X-Content-Type-Options", "nosniff")
            .cabecera("X-Frame-Options", "DENY")
            .cabecera("Referrer-Policy", "no-referrer")
            .cabecera("Content-Security-Policy", POLITICA_CONTENIDO);
    }

    public RespuestaHttp tipo(String tipo) {
        validarValor(tipo);
        this.tipoContenido = tipo;
        return this;
    }

    public RespuestaHttp cuerpo(byte[] datos) {
        this.cuerpo = datos == null ? new byte[0] : datos;
        this.archivo = null;
        return this;
    }

    public RespuestaHttp archivo(Path ruta, long longitud) {
        this.archivo = ruta;
        this.longitudArchivo = longitud;
        return this;
    }

    public RespuestaHttp cabecera(String nombre, String valor) {
        if (nombre == null || !PATRON_NOMBRE_CABECERA.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de cabecera no válido: " + nombre);
        }
        validarValor(valor);
        cabeceras.add(new String[] {nombre, valor});
        return this;
    }

    public RespuestaHttp reemplazarCabecera(String nombre, String valor) {
        cabeceras.removeIf(cabecera -> cabecera[0].equalsIgnoreCase(nombre));
        return cabecera(nombre, valor);
    }

    public int codigo() {
        return codigo;
    }

    public void escribir(Socket socket, boolean incluirCuerpo) throws IOException {
        long longitud = archivo != null ? longitudArchivo : cuerpo.length;

        StringBuilder encabezados = new StringBuilder(512);
        encabezados
            .append("HTTP/1.1 ").append(codigo).append(' ').append(RAZONES.get(codigo)).append("\r\n");

        if (tipoContenido != null) {
            encabezados.append("Content-Type: ").append(tipoContenido).append("\r\n");
        }

        encabezados.append("Content-Length: ").append(longitud).append("\r\n");

        for (String[] cabecera : cabeceras) {
            encabezados.append(cabecera[0]).append(": ").append(cabecera[1]).append("\r\n");
        }

        encabezados.append("Connection: close\r\n\r\n");

        OutputStream salida = socket.getOutputStream();
        salida.write(encabezados.toString().getBytes(StandardCharsets.UTF_8));

        if (incluirCuerpo) {
            if (archivo != null) {
                Files.copy(archivo, salida);
            } else {
                salida.write(cuerpo);
            }
        }

        salida.flush();
        Estadisticas.registrarRespuesta(codigo);
    }

    private static void validarValor(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor de cabecera nulo.");
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\r' || c == '\n') {
                throw new IllegalArgumentException("Valor de cabecera con salto de línea.");
            }
        }
    }
}