import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AdminAuth {

    public static final String VARIABLE_ARCHIVO = "NEXUS_ADMINS_ARCHIVO";
    public static final String PROPIEDAD_ARCHIVO = "nexus.admins.archivo";

    public static final int ITERACIONES = 600_000;
    public static final int MIN_LONGITUD_CLAVE = 12;
    public static final int MAX_LONGITUD_CLAVE = 128;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2-sha256";
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final Pattern PATRON_USUARIO = Pattern.compile("[a-z0-9_.-]{3,32}");

    private static final int MAX_FALLOS_POR_IP = 5;
    private static final int MAX_FALLOS_GLOBALES = 30;
    private static final long VENTANA_FALLOS_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static final long BLOQUEO_IP_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static final long BLOQUEO_GLOBAL_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_IPS_SEGUIDAS = 10_000;

    private static final Semaphore HASHES_SIMULTANEOS = new Semaphore(2, true);
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final ConcurrentHashMap<String, Intentos> FALLOS_POR_IP = new ConcurrentHashMap<>();
    private static final Intentos FALLOS_GLOBALES = new Intentos();
    private static final String HASH_FICTICIO = generarHash("nexus-hash-ficticio-tiempo-constante".toCharArray());

    private static final Object BLOQUEO_CARGA = new Object();
    private static volatile Map<String, String> usuarios = Collections.emptyMap();
    private static volatile FileTime ultimaModificacion;

    public enum Verificacion {
        CORRECTA,
        INCORRECTA,
        OCUPADO
    }

    private AdminAuth() {
    }

    public static void inicializar() {
        Path archivo = archivoUsuarios();
        recargarSiCambio();
        int total = usuarios.size();

        if (total == 0) {
            System.out.println(
                "[ADMIN] No hay usuarios en " + archivo + ". El panel no permitirá entrar. " +
                "Crea uno con: java -cp build AdminAuth agregar <usuario>"
            );
        } else {
            System.out.println("[ADMIN] " + total + " usuario(s) cargado(s) desde " + archivo);
        }

        advertirPermisos(archivo);
    }

    public static Path archivoUsuarios() {
        String valor = System.getProperty(PROPIEDAD_ARCHIVO);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(VARIABLE_ARCHIVO);
        }
        if (valor != null && !valor.isBlank()) {
            return Paths.get(valor.trim()).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home"), ".config", "nexusumg", "admins.properties");
    }

    public static boolean usuarioValido(String usuario) {
        return usuario != null && PATRON_USUARIO.matcher(usuario).matches();
    }

    public static long segundosBloqueo(String ip) {
        long ahora = System.currentTimeMillis();
        long restante = FALLOS_GLOBALES.restanteBloqueo(ahora);

        Intentos intentos = FALLOS_POR_IP.get(ip == null ? "desconocida" : ip);
        if (intentos != null) {
            restante = Math.max(restante, intentos.restanteBloqueo(ahora));
        }

        return restante <= 0 ? 0 : Math.max(1, (restante + 999) / 1000);
    }

    public static Verificacion verificar(String usuario, String clave, String ip) {
        boolean permiso;
        try {
            permiso = HASHES_SIMULTANEOS.tryAcquire(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Verificacion.OCUPADO;
        }

        if (!permiso) {
            return Verificacion.OCUPADO;
        }

        boolean correcta;
        try {
            recargarSiCambio();

            String guardado = usuarioValido(usuario) ? usuarios.get(usuario) : null;
            boolean claveAceptable = clave != null && clave.length() <= MAX_LONGITUD_CLAVE;
            char[] caracteres = claveAceptable ? clave.toCharArray() : new char[] {'x'};

            boolean coincide = comprobarHash(caracteres, guardado != null ? guardado : HASH_FICTICIO);
            Arrays.fill(caracteres, '\0');

            correcta = coincide && guardado != null && claveAceptable;
        } finally {
            HASHES_SIMULTANEOS.release();
        }

        if (correcta) {
            FALLOS_POR_IP.remove(ip == null ? "desconocida" : ip);
            return Verificacion.CORRECTA;
        }

        registrarFallo(ip);
        return Verificacion.INCORRECTA;
    }

    public static String generarHash(char[] clave) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(clave, sal, ITERACIONES);
        Base64.Encoder codificador = Base64.getEncoder().withoutPadding();
        return PREFIJO + "$" + ITERACIONES + "$" + codificador.encodeToString(sal) + "$" + codificador.encodeToString(hash);
    }

    public static boolean comprobarHash(char[] clave, String almacenado) {
        if (almacenado == null) {
            return false;
        }

        String[] partes = almacenado.split("\\$");
        if (partes.length != 4 || !partes[0].equals(PREFIJO)) {
            return false;
        }

        int iteraciones;
        byte[] sal;
        byte[] esperado;
        try {
            iteraciones = Integer.parseInt(partes[1]);
            sal = Base64.getDecoder().decode(partes[2]);
            esperado = Base64.getDecoder().decode(partes[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (iteraciones < 100_000 || iteraciones > 10_000_000 || sal.length < 16 || esperado.length != BITS_HASH / 8) {
            return false;
        }

        return MessageDigest.isEqual(esperado, derivar(clave, sal, iteraciones));
    }

    public static void guardarUsuario(Path archivo, String usuario, char[] clave) throws IOException {
        if (!usuarioValido(usuario)) {
            throw new IllegalArgumentException("Usuario no válido: usa de 3 a 32 caracteres a-z, 0-9, '_', '.' o '-'.");
        }
        if (clave.length < MIN_LONGITUD_CLAVE || clave.length > MAX_LONGITUD_CLAVE) {
            throw new IllegalArgumentException(
                "La contraseña debe tener entre " + MIN_LONGITUD_CLAVE + " y " + MAX_LONGITUD_CLAVE + " caracteres."
            );
        }

        Map<String, String> actuales = new TreeMap<>(leerArchivo(archivo));
        actuales.put(usuario, generarHash(clave));
        escribirArchivo(archivo, actuales);
    }

    public static boolean eliminarUsuario(Path archivo, String usuario) throws IOException {
        Map<String, String> actuales = new TreeMap<>(leerArchivo(archivo));
        if (actuales.remove(usuario) == null) {
            return false;
        }
        escribirArchivo(archivo, actuales);
        return true;
    }

    public static Map<String, String> leerArchivo(Path archivo) throws IOException {
        if (!Files.isRegularFile(archivo)) {
            return Collections.emptyMap();
        }

        Map<String, String> leidos = new TreeMap<>();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            String limpia = linea.trim();
            if (limpia.isEmpty() || limpia.startsWith("#")) {
                continue;
            }
            int igual = limpia.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            String usuario = limpia.substring(0, igual).trim();
            String hash = limpia.substring(igual + 1).trim();
            if (usuarioValido(usuario) && hash.startsWith(PREFIJO + "$")) {
                leidos.put(usuario, hash);
            }
        }
        return leidos;
    }

    public static void main(String[] args) throws IOException {
        Path archivo = archivoUsuarios();

        if (args.length == 1 && args[0].equals("listar")) {
            Map<String, String> actuales = leerArchivo(archivo);
            System.out.println("Archivo: " + archivo);
            if (actuales.isEmpty()) {
                System.out.println("No hay usuarios.");
            }
            for (String usuario : actuales.keySet()) {
                System.out.println("- " + usuario);
            }
            return;
        }

        if (args.length == 2 && args[0].equals("eliminar")) {
            boolean eliminado = eliminarUsuario(archivo, args[1].toLowerCase());
            System.out.println(eliminado ? "Usuario eliminado: " + args[1] : "No existe el usuario: " + args[1]);
            return;
        }

        if (args.length == 2 && args[0].equals("agregar")) {
            String usuario = args[1].toLowerCase();
            if (!usuarioValido(usuario)) {
                System.err.println("Usuario no válido: usa de 3 a 32 caracteres a-z, 0-9, '_', '.' o '-'.");
                System.exit(2);
            }

            Console consola = System.console();
            if (consola == null) {
                System.err.println("Ejecuta este comando en una terminal interactiva para escribir la contraseña de forma oculta.");
                System.exit(2);
            }

            char[] primera = consola.readPassword("Contraseña para %s (mínimo %d caracteres): ", usuario, MIN_LONGITUD_CLAVE);
            char[] segunda = consola.readPassword("Repite la contraseña: ");

            try {
                if (primera == null || segunda == null || !Arrays.equals(primera, segunda)) {
                    System.err.println("Las contraseñas no coinciden. No se guardó nada.");
                    System.exit(2);
                }
                guardarUsuario(archivo, usuario, primera);
                System.out.println("Usuario guardado: " + usuario + " en " + archivo);
                System.out.println("Si el servidor está en marcha, usará el cambio en el siguiente intento de inicio de sesión.");
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            } finally {
                if (primera != null) Arrays.fill(primera, '\0');
                if (segunda != null) Arrays.fill(segunda, '\0');
            }
            return;
        }

        System.out.println("Uso:");
        System.out.println("  java -cp build AdminAuth agregar <usuario>");
        System.out.println("  java -cp build AdminAuth eliminar <usuario>");
        System.out.println("  java -cp build AdminAuth listar");
        System.out.println("Archivo actual: " + archivo);
    }

    private static void registrarFallo(String ipOriginal) {
        long ahora = System.currentTimeMillis();
        String ip = ipOriginal == null ? "desconocida" : ipOriginal;

        FALLOS_GLOBALES.registrar(ahora, MAX_FALLOS_GLOBALES, BLOQUEO_GLOBAL_MILLIS);

        Intentos intentos = FALLOS_POR_IP.get(ip);
        if (intentos == null) {
            if (FALLOS_POR_IP.size() >= MAX_IPS_SEGUIDAS) {
                FALLOS_POR_IP.entrySet().removeIf(entrada -> entrada.getValue().expirado(ahora));
            }
            if (FALLOS_POR_IP.size() >= MAX_IPS_SEGUIDAS) {
                return;
            }
            intentos = FALLOS_POR_IP.computeIfAbsent(ip, clave -> new Intentos());
        }

        intentos.registrar(ahora, MAX_FALLOS_POR_IP, BLOQUEO_IP_MILLIS);
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        KeySpec especificacion = new PBEKeySpec(clave, sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 no disponible en esta JVM.", e);
        } finally {
            ((PBEKeySpec) especificacion).clearPassword();
        }
    }

    private static void recargarSiCambio() {
        Path archivo = archivoUsuarios();
        try {
            FileTime modificacion = Files.isRegularFile(archivo) ? Files.getLastModifiedTime(archivo) : null;
            if (modificacion != null && modificacion.equals(ultimaModificacion)) {
                return;
            }
            synchronized (BLOQUEO_CARGA) {
                if (modificacion != null && modificacion.equals(ultimaModificacion)) {
                    return;
                }
                usuarios = Collections.unmodifiableMap(leerArchivo(archivo));
                ultimaModificacion = modificacion;
            }
        } catch (IOException e) {
            System.err.println("[ADMIN] No se pudo leer el archivo de usuarios: " + e.getMessage());
        }
    }

    private static void escribirArchivo(Path archivo, Map<String, String> datos) throws IOException {
        Path carpeta = archivo.toAbsolutePath().getParent();
        boolean posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

        if (!Files.isDirectory(carpeta)) {
            if (posix) {
                Files.createDirectories(carpeta, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            } else {
                Files.createDirectories(carpeta);
            }
        }

        StringBuilder contenido = new StringBuilder();
        for (Map.Entry<String, String> entrada : datos.entrySet()) {
            contenido.append(entrada.getKey()).append('=').append(entrada.getValue()).append('\n');
        }

        Path temporal = Files.createTempFile(carpeta, ".admins", ".tmp");
        try {
            if (posix) {
                Files.setPosixFilePermissions(temporal, PosixFilePermissions.fromString("rw-------"));
            }
            Files.writeString(temporal, contenido.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static void advertirPermisos(Path archivo) {
        try {
            if (!Files.isRegularFile(archivo)) {
                return;
            }
            if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                return;
            }
            Set<PosixFilePermission> permisos = Files.getPosixFilePermissions(archivo);
            if (
                permisos.contains(PosixFilePermission.GROUP_READ) ||
                permisos.contains(PosixFilePermission.OTHERS_READ)
            ) {
                System.err.println("[ADMIN] Advertencia: otros usuarios pueden leer " + archivo + ". Ejecuta: chmod 600 " + archivo);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class Intentos {
        private int fallos;
        private long inicioVentana;
        private long bloqueadoHasta;

        private synchronized void registrar(long ahora, int maximo, long duracionBloqueo) {
            if (ahora - inicioVentana > VENTANA_FALLOS_MILLIS) {
                inicioVentana = ahora;
                fallos = 0;
            }
            fallos++;
            if (fallos >= maximo) {
                bloqueadoHasta = ahora + duracionBloqueo;
                fallos = 0;
                inicioVentana = ahora;
            }
        }

        private synchronized long restanteBloqueo(long ahora) {
            return bloqueadoHasta - ahora;
        }

        private synchronized boolean expirado(long ahora) {
            return bloqueadoHasta <= ahora && ahora - inicioVentana > VENTANA_FALLOS_MILLIS;
        }
    }
}