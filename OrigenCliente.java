import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

public final class OrigenCliente {

    public static final String CABECERA_SECRETO = "x-nexus-origen";
    public static final String VARIABLE_SECRETO = "NEXUS_SECRETO_ORIGEN";
    public static final String PROPIEDAD_SECRETO = "nexus.secreto.origen";
    public static final int LONGITUD_MINIMA_SECRETO = 32;

    private static final Pattern PATRON_IPV4 =
        Pattern.compile("[0-9]{1,3}(\\.[0-9]{1,3}){3}");
    private static final Pattern PATRON_PUNTOS = Pattern.compile("\\.");
    private static final Pattern PATRON_IPV6 =
        Pattern.compile("[0-9a-fA-F:.]+");
    private static final Pattern PATRON_SECRETO =
        Pattern.compile("[A-Za-z0-9_-]+");

    public enum Tipo {
        REMOTO_DIRECTO,
        LOCAL,
        CLOUDFLARE_VERIFICADO,
        CLOUDFLARE_SIN_VERIFICACION,
        CLOUDFLARE_RECHAZADO
    }

    public record Origen(String ip, Tipo tipo) {
        public boolean esConfiable() {
            return tipo == Tipo.CLOUDFLARE_VERIFICADO || tipo == Tipo.CLOUDFLARE_SIN_VERIFICACION;
        }
    }

    private static final byte[] SECRETO = cargarSecreto();

    private OrigenCliente() {
    }

    public static boolean validarConfiguracion() {
        String valor = leerValorConfigurado();

        if (valor == null) {
            System.out.println(
                "[ORIGEN] Modo compatible: se acepta CF-Connecting-IP de conexiones locales sin verificar. " +
                "Configura " + VARIABLE_SECRETO + " para activar la verificación."
            );
            return true;
        }

        if (SECRETO == null) {
            System.err.println(
                "[ORIGEN] " + VARIABLE_SECRETO + " no es válido: debe tener al menos " +
                LONGITUD_MINIMA_SECRETO + " caracteres y solo letras, números, '-' o '_'. " +
                "El servidor no se iniciará."
            );
            return false;
        }

        System.out.println(
            "[ORIGEN] Verificación activa: solo se acepta CF-Connecting-IP con la cabecera secreta correcta."
        );
        return true;
    }

    public static boolean verificacionActiva() {
        return SECRETO != null;
    }

    public static Origen resolver(Socket socket, SolicitudHttp solicitud) {
        String ipConexion = socket.getInetAddress().getHostAddress();

        if (!socket.getInetAddress().isLoopbackAddress()) {
            return new Origen(ipConexion, Tipo.REMOTO_DIRECTO);
        }

        String ipCloudflare = normalizarIp(solicitud.cabecera("cf-connecting-ip"));

        if (ipCloudflare == null) {
            return new Origen(ipConexion, Tipo.LOCAL);
        }

        if (SECRETO == null) {
            return new Origen(ipCloudflare, Tipo.CLOUDFLARE_SIN_VERIFICACION);
        }

        String recibido = solicitud.cabecera(CABECERA_SECRETO);
        if (recibido != null && MessageDigest.isEqual(
                SECRETO,
                recibido.getBytes(StandardCharsets.UTF_8))) {
            return new Origen(ipCloudflare, Tipo.CLOUDFLARE_VERIFICADO);
        }

        return new Origen(ipConexion, Tipo.CLOUDFLARE_RECHAZADO);
    }

    public static String normalizarIp(String candidata) {
        if (candidata == null || candidata.isBlank() || candidata.length() > 45) {
            return null;
        }

        String ip = candidata.trim();

        if (PATRON_IPV4.matcher(ip).matches()) {
            String[] octetos = PATRON_PUNTOS.split(ip, -1);
            StringBuilder canonica = new StringBuilder();
            for (int i = 0; i < octetos.length; i++) {
                int valor;
                try {
                    valor = Integer.parseInt(octetos[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (valor < 0 || valor > 255) {
                    return null;
                }
                if (i > 0) canonica.append('.');
                canonica.append(valor);
            }
            return canonica.toString();
        }

        if (ip.indexOf(':') >= 0 && PATRON_IPV6.matcher(ip).matches()) {
            try {
                InetAddress direccion = InetAddress.getByName(ip);
                return direccion.getHostAddress().toLowerCase(Locale.ROOT);
            } catch (UnknownHostException ignored) {
                return null;
            }
        }

        return null;
    }

    private static String leerValorConfigurado() {
        String valor = System.getProperty(PROPIEDAD_SECRETO);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(VARIABLE_SECRETO);
        }
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static byte[] cargarSecreto() {
        String valor = leerValorConfigurado();
        if (
            valor == null ||
            valor.length() < LONGITUD_MINIMA_SECRETO ||
            !PATRON_SECRETO.matcher(valor).matches()
        ) {
            return null;
        }
        return valor.getBytes(StandardCharsets.UTF_8);
    }
}