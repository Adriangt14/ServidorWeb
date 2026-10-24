import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SesionesAdmin {

    public static final long INACTIVIDAD_MILLIS = TimeUnit.MINUTES.toMillis(30);
    public static final long DURACION_MAXIMA_MILLIS = TimeUnit.HOURS.toMillis(8);
    public static final int MAXIMO_SESIONES = 50;

    private static final Pattern PATRON_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

    public record Sesion(String usuario, String tokenCsrf, long creadaMillis) {
    }

    private static final class Entrada {
        private final Sesion sesion;
        private final long orden;
        private volatile long ultimoUsoMillis;

        private Entrada(Sesion sesion, long orden, long ahora) {
            this.sesion = sesion;
            this.orden = orden;
            this.ultimoUsoMillis = ahora;
        }
    }

    private final ConcurrentHashMap<String, Entrada> sesiones = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final long inactividadMillis;
    private final long duracionMaximaMillis;
    private final int maximoSesiones;
    private long siguienteOrden;

    public SesionesAdmin() {
        this(INACTIVIDAD_MILLIS, DURACION_MAXIMA_MILLIS, MAXIMO_SESIONES);
    }

    public SesionesAdmin(long inactividadMillis, long duracionMaximaMillis, int maximoSesiones) {
        this.inactividadMillis = inactividadMillis;
        this.duracionMaximaMillis = duracionMaximaMillis;
        this.maximoSesiones = maximoSesiones;
    }

    public long duracionMaximaSegundos() {
        return duracionMaximaMillis / 1000;
    }

    public synchronized String crear(String usuario) {
        long ahora = System.currentTimeMillis();

        if (sesiones.size() >= maximoSesiones) {
            limpiarExpiradas(ahora);
        }

        while (sesiones.size() >= maximoSesiones) {
            String masAntigua = null;
            long menorOrden = Long.MAX_VALUE;
            for (Map.Entry<String, Entrada> entrada : sesiones.entrySet()) {
                if (entrada.getValue().orden < menorOrden) {
                    menorOrden = entrada.getValue().orden;
                    masAntigua = entrada.getKey();
                }
            }
            if (masAntigua == null) {
                break;
            }
            sesiones.remove(masAntigua);
        }

        String token = generarToken();
        Sesion sesion = new Sesion(usuario, generarToken(), ahora);
        sesiones.put(huella(token), new Entrada(sesion, siguienteOrden++, ahora));
        return token;
    }

    public Sesion obtener(String token) {
        if (token == null || !PATRON_TOKEN.matcher(token).matches()) {
            return null;
        }

        String clave = huella(token);
        Entrada entrada = sesiones.get(clave);
        if (entrada == null) {
            return null;
        }

        long ahora = System.currentTimeMillis();
        if (expirada(entrada, ahora)) {
            sesiones.remove(clave, entrada);
            return null;
        }

        entrada.ultimoUsoMillis = ahora;
        return entrada.sesion;
    }

    public void cerrar(String token) {
        if (token != null && PATRON_TOKEN.matcher(token).matches()) {
            sesiones.remove(huella(token));
        }
    }

    public int cerrarDeUsuario(String usuario) {
        int antes = sesiones.size();
        sesiones.entrySet().removeIf(entrada -> entrada.getValue().sesion.usuario().equals(usuario));
        return antes - sesiones.size();
    }

    public int activas() {
        limpiarExpiradas(System.currentTimeMillis());
        return sesiones.size();
    }

    public static boolean tokensIguales(String esperado, String recibido) {
        if (esperado == null || recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
            esperado.getBytes(StandardCharsets.UTF_8),
            recibido.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean expirada(Entrada entrada, long ahora) {
        return ahora - entrada.ultimoUsoMillis > inactividadMillis
            || ahora - entrada.sesion.creadaMillis() > duracionMaximaMillis;
    }

    private void limpiarExpiradas(long ahora) {
        sesiones.entrySet().removeIf(entrada -> expirada(entrada.getValue(), ahora));
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String huella(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible.", e);
        }
    }
}