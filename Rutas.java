import java.util.Locale;

public final class Rutas {

    public static final String INICIO = "/";
    public static final String PRODUCTOS = "/productos";
    public static final String TECNOLOGIA = "/tecnologia";
    public static final String GAMING = "/gaming";
    public static final String ACCESORIOS = "/accesorios";
    public static final String MUSICA = "/musica";
    public static final String SERVICIOS = "/servicios";
    public static final String NOSOTROS = "/nosotros";

    public static final String SOLICITUDES = "/solicitudes";
    public static final String HILOS = "/hilos";
    public static final String CLIENTES = "/clientes";

    public static final String RAZER = "/productorazer";
    public static final String MIKU = "/productomiku";
    public static final String GAREN = "/productogaren";
    public static final String LOGITECH_PRO = "/productologitechpro";
    public static final String LOGITECH_COMBO = "/productologitechcombo";
    public static final String TECLADO = "/productotecladomecanico";

    public static final String ALBUM_NOTHING = "/albumnothingbutthebest";
    public static final String ALBUM_VOZ = "/albumlavozdeoro";

    public static final String IMAGENES = "/imagenes";
    public static final String BLOQUEADO = "/bloqueado";
    public static final String NO_ENCONTRADO = "/no-encontrado";

    private Rutas() {
    }

    public static boolean esRutaProtegida(String ruta) {

        if (ruta == null || !ruta.startsWith("/")) {
            return true;
        }

        String rutaLimpia = ruta.toLowerCase(Locale.ROOT);

        if (rutaLimpia.contains("\\") || rutaLimpia.contains("..")) {
            return true;
        }

        String[] segmentos = rutaLimpia.split("/");

        for (String segmento : segmentos) {

            if (segmento.startsWith(".")) {
                return true;
            }
        }

        return false;
    }

    public static String rutaProducto(int id) {

        return switch (id) {
            case 1 -> RAZER;
            case 2 -> MIKU;
            case 3 -> GAREN;
            case 4 -> LOGITECH_PRO;
            case 5 -> LOGITECH_COMBO;
            case 6 -> TECLADO;
            default -> null;
        };
    }

    public static boolean esRutaProducto(String ruta) {

        return ruta.equals(RAZER)
            || ruta.equals(MIKU)
            || ruta.equals(GAREN)
            || ruta.equals(LOGITECH_PRO)
            || ruta.equals(LOGITECH_COMBO)
            || ruta.equals(TECLADO);
    }

    public static boolean esRutaAlbum(String ruta) {

        return ruta.equals(ALBUM_NOTHING)
            || ruta.equals(ALBUM_VOZ);
    }

    public static boolean esImagen(String ruta) {

        return ruta != null && ruta.startsWith(IMAGENES + "/");
    }

    public static boolean esPaginaValida(String ruta) {

        return ruta.equals(INICIO)
            || ruta.equals(PRODUCTOS)
            || ruta.equals(TECNOLOGIA)
            || ruta.equals(GAMING)
            || ruta.equals(ACCESORIOS)
            || ruta.equals(MUSICA)
            || ruta.equals(SERVICIOS)
            || ruta.equals(NOSOTROS)
            || ruta.equals(SOLICITUDES)
            || ruta.equals(HILOS)
            || ruta.equals(CLIENTES)
            || esRutaProducto(ruta)
            || esRutaAlbum(ruta);
    }

    public static String rutaEstadistica(String ruta) {

        if (esRutaProtegida(ruta)) {
            return BLOQUEADO;
        }

        if (esImagen(ruta)) {
            return IMAGENES;
        }

        if (esPaginaValida(ruta)) {
            return ruta;
        }

        return NO_ENCONTRADO;
    }
}