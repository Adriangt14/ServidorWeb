import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SolicitudHttp {

    public static final int MAX_LINEA_HTTP = 4096;
    public static final int MAX_CABECERAS_HTTP = 50;
    public static final int MAX_BYTES_CABECERAS = 16_384;
    public static final int MAX_BYTES_CUERPO = 8_192;

    private static final Pattern PATRON_ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern PATRON_NOMBRE_CABECERA =
        Pattern.compile("[!#$%&'*+.^_`|~0-9a-z-]+");
    private static final Pattern PATRON_DIGITOS = Pattern.compile("[0-9]{1,10}");
    private static final Set<String> CABECERAS_UNICAS =
        Set.of("cf-connecting-ip", "content-length", "transfer-encoding");

    private final Socket socket;
    private final BufferedReader entrada;
    private final long plazoLectura;
    private final String lineaPeticion;
    private final Map<String, String> cabeceras;

    private String metodo;
    private String objetivo;
    private String version;
    private String ruta;
    private String consulta;
    private byte[] cuerpo;

    private SolicitudHttp(
        Socket socket,
        BufferedReader entrada,
        long plazoLectura,
        String lineaPeticion,
        Map<String, String> cabeceras
    ) {
        this.socket = socket;
        this.entrada = entrada;
        this.plazoLectura = plazoLectura;
        this.lineaPeticion = lineaPeticion;
        this.cabeceras = cabeceras;
    }

    public static SolicitudHttp leer(Socket socket, int timeoutMs) throws IOException {
        socket.setSoTimeout(timeoutMs);

        BufferedReader entrada = new BufferedReader(
            new InputStreamReader(
                socket.getInputStream(),
                StandardCharsets.ISO_8859_1
            )
        );

        long plazo = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        String linea;
        try {
            linea = leerLineaLimitada(entrada, socket, plazo);
        } catch (LineaDemasiadoLargaException e) {
            throw new SolicitudInvalidaException(
                414,
                "Dirección demasiado larga",
                "La dirección solicitada supera el tamaño permitido."
            );
        }

        if (linea == null || linea.isBlank()) {
            return null;
        }

        Map<String, String> cabeceras;
        try {
            cabeceras = leerCabecerasLimitadas(entrada, socket, plazo);
        } catch (LineaDemasiadoLargaException e) {
            throw new SolicitudInvalidaException(
                431,
                "Cabeceras demasiado grandes",
                "Las cabeceras de la solicitud superan el tamaño permitido."
            );
        }

        return new SolicitudHttp(socket, entrada, plazo, linea, cabeceras);
    }

    public boolean pideSoloCabeceras() {
        return lineaPeticion.startsWith("HEAD ");
    }

    public void interpretarLinea() throws SolicitudInvalidaException {
        String[] partes = PATRON_ESPACIOS.split(lineaPeticion.trim());

        if (
            partes.length != 3 ||
            !(partes[2].equals("HTTP/1.0") || partes[2].equals("HTTP/1.1"))
        ) {
            throw new SolicitudInvalidaException(
                400,
                "Solicitud HTTP no válida",
                "La petición recibida no tiene el formato esperado."
            );
        }

        metodo = partes[0];
        objetivo = partes[1];
        version = partes[2];
    }

    public void interpretarRuta() throws SolicitudInvalidaException {
        try {
            URI uri = new URI(objetivo);
            String camino = uri.getPath();
            ruta = (camino == null || !camino.startsWith("/")) ? "" : camino;
            consulta = uri.getRawQuery();
        } catch (URISyntaxException e) {
            throw new SolicitudInvalidaException(
                400,
                "Ruta no válida",
                "La ruta solicitada no tiene un formato válido."
            );
        }
    }

    public byte[] leerCuerpo() throws IOException {
        if (cuerpo != null) {
            return cuerpo;
        }

        if (cabeceras.containsKey("transfer-encoding")) {
            throw new SolicitudInvalidaException(
                501,
                "Codificación no admitida",
                "El servidor no admite Transfer-Encoding en el cuerpo."
            );
        }

        String longitud = cabeceras.get("content-length");
        if (longitud == null) {
            throw new SolicitudInvalidaException(
                411,
                "Longitud requerida",
                "La solicitud debe indicar Content-Length."
            );
        }

        if (!PATRON_DIGITOS.matcher(longitud).matches()) {
            throw new SolicitudInvalidaException(
                400,
                "Solicitud no válida",
                "Content-Length no válido."
            );
        }

        long bytes = Long.parseLong(longitud);
        if (bytes > MAX_BYTES_CUERPO) {
            throw new SolicitudInvalidaException(
                413,
                "Contenido demasiado grande",
                "El cuerpo de la solicitud supera el tamaño permitido."
            );
        }

        byte[] datos = new byte[(int) bytes];
        for (int i = 0; i < datos.length; i++) {
            int caracter = leerCaracter(entrada, socket, plazoLectura);
            if (caracter == -1) {
                throw new IOException("Cuerpo HTTP incompleto.");
            }
            datos[i] = (byte) caracter;
        }

        cuerpo = datos;
        return datos;
    }

    public String metodo() {
        return metodo;
    }

    public String version() {
        return version;
    }

    public String objetivo() {
        return objetivo;
    }

    public String objetivoSinConsulta() {
        if (objetivo == null) {
            return null;
        }
        int consultaInicio = objetivo.indexOf('?');
        return consultaInicio >= 0 ? objetivo.substring(0, consultaInicio) : objetivo;
    }

    public String ruta() {
        return ruta;
    }

    public String consulta() {
        return consulta;
    }

    public String cabecera(String nombre) {
        return nombre == null ? null : cabeceras.get(nombre.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> cabeceras() {
        return Collections.unmodifiableMap(cabeceras);
    }

    public String cookie(String nombre) {
        String valor = cabeceras.get("cookie");
        if (valor == null || nombre == null) {
            return null;
        }

        for (String parte : valor.split(";")) {
            int igual = parte.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            if (parte.substring(0, igual).trim().equals(nombre)) {
                return parte.substring(igual + 1).trim();
            }
        }
        return null;
    }

    private static int leerCaracter(
        BufferedReader entrada,
        Socket socket,
        long limite
    ) throws IOException {
        long restante = limite - System.nanoTime();

        if (restante <= 0) {
            throw new SocketTimeoutException(
                "Tiempo máximo para recibir la petición excedido."
            );
        }

        long timeoutMs = (restante + 999_999L) / 1_000_000L;
        socket.setSoTimeout((int) Math.max(1L, timeoutMs));

        int caracter = entrada.read();

        if (System.nanoTime() - limite >= 0) {
            throw new SocketTimeoutException(
                "Tiempo máximo para recibir la petición excedido."
            );
        }

        return caracter;
    }

    private static String leerLineaLimitada(
        BufferedReader entrada,
        Socket socket,
        long limite
    ) throws IOException {

        StringBuilder linea = new StringBuilder();

        while (true) {
            int caracter = leerCaracter(entrada, socket, limite);

            if (caracter == -1) {
                if (linea.isEmpty()) {
                    return null;
                }

                throw new IOException(
                    "Petición HTTP incompleta."
                );
            }

            if (caracter == '\n') {
                return linea.toString();
            }

            if (caracter == '\r') {
                continue;
            }

            if (linea.length() >= MAX_LINEA_HTTP) {
                throw new LineaDemasiadoLargaException(
                    "Línea HTTP demasiado larga."
                );
            }

            linea.append((char) caracter);
        }
    }

    private static Map<String, String> leerCabecerasLimitadas(
        BufferedReader entrada,
        Socket socket,
        long plazoLectura
    ) throws IOException {

        Map<String, String> cabeceras = new HashMap<>();
        int cantidad = 0;
        int bytesAproximados = 0;

        while (true) {
            String linea = leerLineaLimitada(entrada, socket, plazoLectura);
            if (linea == null) {
                throw invalida("Cabeceras HTTP incompletas.");
            }
            if (linea.isEmpty()) {
                return cabeceras;
            }

            cantidad++;
            bytesAproximados += linea.length() + 2;
            if (cantidad > MAX_CABECERAS_HTTP || bytesAproximados > MAX_BYTES_CABECERAS) {
                throw new SolicitudInvalidaException(
                    431,
                    "Cabeceras demasiado grandes",
                    "Las cabeceras de la solicitud superan el tamaño permitido."
                );
            }

            if (linea.charAt(0) == ' ' || linea.charAt(0) == '\t') {
                throw invalida("Formato de cabecera HTTP no válido.");
            }

            int separador = linea.indexOf(':');
            if (separador <= 0) {
                throw invalida("Formato de cabecera HTTP no válido.");
            }

            String nombre = linea.substring(0, separador).trim().toLowerCase(Locale.ROOT);
            if (!PATRON_NOMBRE_CABECERA.matcher(nombre).matches()) {
                throw invalida("Nombre de cabecera HTTP no válido.");
            }

            String valor = linea.substring(separador + 1).trim();
            for (int i = 0; i < valor.length(); i++) {
                char c = valor.charAt(i);
                if ((c < 0x20 && c != '\t') || c == 0x7f) {
                    throw invalida("Valor de cabecera HTTP no válido.");
                }
            }

            if (CABECERAS_UNICAS.contains(nombre) && cabeceras.containsKey(nombre)) {
                throw invalida(
                    nombre.equals("cf-connecting-ip")
                        ? "Cabecera CF-Connecting-IP duplicada."
                        : "Cabecera " + nombre + " duplicada."
                );
            }

            cabeceras.putIfAbsent(nombre, valor);
        }
    }

    private static SolicitudInvalidaException invalida(String mensaje) {
        return new SolicitudInvalidaException(400, "Solicitud no válida", mensaje);
    }

    public static final class SolicitudInvalidaException extends IOException {
        private static final long serialVersionUID = 1L;

        private final int codigo;
        private final String titulo;

        private SolicitudInvalidaException(int codigo, String titulo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
            this.titulo = titulo;
        }

        public int codigo() {
            return codigo;
        }

        public String titulo() {
            return titulo;
        }
    }

    private static final class LineaDemasiadoLargaException extends IOException {
        private static final long serialVersionUID = 1L;

        private LineaDemasiadoLargaException(String mensaje) {
            super(mensaje);
        }
    }
}