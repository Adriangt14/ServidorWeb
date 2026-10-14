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

        if (rutaLimpia.contains("\\")
                || rutaLimpia.contains("..")
                || rutaLimpia.contains("\0")) {
            return true;
        }

        for (String segmento : rutaLimpia.split("/")) {

            if (segmento.startsWith(".")) {
                return true;
            }
        }

        return false;
    }

    public static boolean esRutaProducto(String ruta) {

        return RAZER.equals(ruta)
            || MIKU.equals(ruta)
            || GAREN.equals(ruta)
            || LOGITECH_PRO.equals(ruta)
            || LOGITECH_COMBO.equals(ruta)
            || TECLADO.equals(ruta);
    }

    public static boolean esRutaAlbum(String ruta) {

        return ALBUM_NOTHING.equals(ruta)
            || ALBUM_VOZ.equals(ruta);
    }

    public static boolean esImagen(String ruta) {

        return ruta != null && ruta.startsWith(IMAGENES + "/");
    }

    public static boolean esPaginaValida(String ruta) {

        return INICIO.equals(ruta)
            || PRODUCTOS.equals(ruta)
            || TECNOLOGIA.equals(ruta)
            || GAMING.equals(ruta)
            || ACCESORIOS.equals(ruta)
            || MUSICA.equals(ruta)
            || SERVICIOS.equals(ruta)
            || NOSOTROS.equals(ruta)
            || SOLICITUDES.equals(ruta)
            || HILOS.equals(ruta)
            || CLIENTES.equals(ruta)
            || esRutaProducto(ruta)
            || esRutaAlbum(ruta);
    }

    public static String rutaEstadistica(String ruta) {

        if (BLOQUEADO.equals(ruta)) {
            return BLOQUEADO;
        }

        if (NO_ENCONTRADO.equals(ruta)) {
            return NO_ENCONTRADO;
        }

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