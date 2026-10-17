import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PaginaWeb {

    private static final List<Miembro> MIEMBROS = List.of(
        new Miembro("El que Carreo el Proyecto y su mujer de LOL", "Qiyana_FigueroaGOAT.png"),
        new Miembro("Enfermeria una noche del viernes 9 de Octubre", "Engelbert.png"),
        new Miembro("No me subo a motos", "Jose_Dictador.png"),
        new Miembro("La gran muralla china", "Hector.png")
    );

    private static final List<Producto> PRODUCTOS = List.of(

        new Producto(
            Rutas.RAZER,
            "Audífonos Razer",
            "Consultar precio",
            "Audifonos_Razer.webp",
            "Audífonos para tus partidas, música y sesiones largas.",
            "AUDIO",
            "accesorios"
        ),

        new Producto(
            Rutas.EMILIA,
            "Emilia",
            "Consultar precio",
            "Emilia_rezero.png",
            "Figura de accion de Emilia",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.TECLADO_K_DA,
            "Teclado K/DA",
            "Consultar precio",
            "Teclado_K_DA.png",
            "Teclado gamer con iluminación RGB y diseño K/DA.",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.MOUSE_GAMER,
            "Mouse gamer K/DA",
            "Consultar precio",
            "Mouse_gamer.png",
            "Mouse gamer con botones adicionales y diseño K/DA.",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.MOUSEPAD,
            "Mousepad K/DA",
            "Consultar precio",
            "Mousepad.png",
            "Mousepad con ilustración de K/DA para tu setup.",
            "ACCESORIOS",
            "accesorios"
        ),

        new Producto(
            Rutas.NICO_ROBIN,
            "Nico Robin",
            "Consultar precio",
            "Nico_Robin.png",
            "Figura de accion de Nico Robin",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.MIKU,
            "Hatsune Miku",
            "Consultar precio",
            "Figura_Miku.png",
            "Figura de colección para darle personalidad a tu espacio.",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.GAREN,
            "Garen Pingu",
            "Consultar precio",
            "Figura_Garen_Pingu.webp",
            "Una figura coleccionable para los fans de League of Legends.",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.LOGITECH_PRO,
            "Logitech PRO",
            "Consultar precio",
            "Logitech_Pro.png",
            "Periférico Logitech PRO para completar tu estación de juego.",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.LOGITECH_COMBO,
            "Combo Logitech",
            "Consultar precio",
            "Logitech_Basico_Teclado.png",
            "Teclado y mouse para trabajar, estudiar y utilizar todos los días.",
            "OFICINA",
            "accesorios"
        ),

        new Producto(
            Rutas.NERO,
            "Figura Nero",
            "Q150.00",
            "Nero_Figura.png",
            "Figura coleccionable Nero Black Clover",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.ASTA,
            "Figura Accion Asta",
            "Q200.00",
            "Asta_Figura.png",
            "Figura coleccionable Asta Black Clover",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.CUADRO,
            "Cuadro de Asta",
            "Q75.00",
            "Cuadro_Asta.png",
            "Cuadro decorativo de Asta de Black Clover",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.RIYO,
            "Figura de Riyo",
            "Q187.00",
            "Riyo_Figura.png",
            "Figura coleccionable de Riyo Gachiakuta",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.HITSUKI,
            "Poster Hitsuki",
            "Q50.00",
            "Hitsuki_Poster.png",
            "Poster de Hitsuki de Gachiakuta",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.RUDO,
            "Figura de accion de Rudo",
            "Q250.00",
            "Rudo_Figura.png",
            "Figura Gigante de Rudo",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.ASUNA,
            "Figura de Asuna SAO",
            "Q500.00",
            "Asuna_Figura.png",
            "Figura accion Asuna en SAO",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.ASUNA2,
            "Figura Asuna Maid",
            "Q1000.00",
            "Asuna_Figura2.png",
            "Fugura de Asuna en traje maid",
            "COLECCIÓN",
            "coleccion"
        ),

        new Producto(
            Rutas.REDTECLADO,
            "Teclado gaming redragon",
            "Q300.00",
            "Teclado_Redragon.png",
            "Teclado para juegos competitivos",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.REDAUDIFONOS,
            "Audifonos gaming redragon",
            "Q250.00",
            "Audifonos_Redragon.png",
            "Audifonos de alto perfomance gaming",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.REDMOUSE,
            "Mouse optico redragon",
            "Q175.00",
            "Mouse_Redragon.png",
            "Mause 1,000,000 dpi",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.XBOX,
            "Control Xbox",
            "Q500.00",
            "Control_Xbox.png",
            "Control compatible con pc Xbox",
            "GAMING",
            "gaming"
        ),

        new Producto(
            Rutas.PS5,
            "Control PS5",
            "Q750.00",
            "Control_PS5.png",
            "Control gaming PS5",
            "GAMING",
            "gaming"
        )
    );

    private static final List<Album> ALBUMS = List.of(

        new Album(
            Rutas.ALBUM_NOTHING,
            "Nothing But the Best",
            "Album_Nothingbutthebest.webp",
            "Una referencia musical para acompañar el viaje."
        ),

        new Album(
            Rutas.ALBUM_VOZ,
            "La Voz de Oro",
            "Album_Lavozdeoro.jpg",
            "Una colección sonora que forma parte de Nexus UMG."
        ),

        new Album(
            Rutas.ALBUM_COSANUESTRA,
            "Cosa Nuestra",
            "CosaNuestra.png",
            "Una referencia musical incorporada al archivo sonoro de Nexus UMG."
        )
    );

    private static final ConcurrentHashMap<String, String> CACHE_PAGINAS =
        new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, byte[]> CACHE_PAGINAS_BYTES =
        new ConcurrentHashMap<>();

    public static String generar(String ruta, String hilo) {
        if (ruta == null || esRutaDinamica(ruta) || !Rutas.esPaginaValida(ruta)) {
            return generarSinCache(ruta, hilo);
        }

        return CACHE_PAGINAS.computeIfAbsent(
            ruta,
            clave -> generarSinCache(clave, hilo)
        );
    }

    public static byte[] generarBytes(String ruta, String hilo) {
        if (ruta == null || esRutaDinamica(ruta) || !Rutas.esPaginaValida(ruta)) {
            return generarSinCache(ruta, hilo).getBytes(StandardCharsets.UTF_8);
        }

        return CACHE_PAGINAS_BYTES.computeIfAbsent(
            ruta,
            clave -> generar(clave, hilo).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static boolean esRutaDinamica(String ruta) {
        return Rutas.SOLICITUDES.equals(ruta) || Rutas.HILOS.equals(ruta);
    }

    private static String generarSinCache(String ruta, String hilo) {
        String contenido;

        if (ruta.equals(Rutas.EQUIPO)) {
            contenido = paginaEquipo();

        } else if (ruta.equals(Rutas.INICIO) ||
            ruta.equals(Rutas.PRODUCTOS)) {
            contenido = inicio();

        } else if (
            ruta.equals(Rutas.TECNOLOGIA) ||
            ruta.equals(Rutas.GAMING) ||
            ruta.equals(Rutas.ACCESORIOS)
        ) {
            contenido = paginaCategoria(ruta);

        } else if (ruta.equals(Rutas.MUSICA)) {
            contenido = paginaMusica();

        } else if (ruta.equals(Rutas.SERVICIOS)) {
            contenido = paginaInformativa(
                "Servicios",
                "Una experiencia de atención cercana para acompañar tu próxima expedición."
            );

        } else if (ruta.equals(Rutas.NOSOTROS)) {
            contenido = paginaInformativa(
                "Sobre Nexus UMG",
                "Tecnología, gaming y cultura reunidos en un solo lugar."
            );

        } else if (ruta.equals(Rutas.SOLICITUDES)) {
            contenido = paginaInformativa(
                "Solicitudes HTTP",
                "Solicitud atendida por el hilo: " + hilo
            );

        } else if (ruta.equals(Rutas.HILOS)) {
            contenido = paginaInformativa(
                "Hilos del servidor",
                "Solicitud atendida por el hilo: " + hilo
            );

        } else if (ruta.equals(Rutas.CLIENTES)) {
            contenido = paginaInformativa(
                "Clientes",
                "El servidor está preparado para recibir clientes."
            );

        } else {
            Producto producto = buscarProducto(ruta);
            Album album = buscarAlbum(ruta);

            if (producto != null) {
                contenido = detalleProducto(producto);
            } else if (album != null) {
                contenido = detalleAlbum(album);
            } else {
                contenido = paginaInformativa(
                    "Página no encontrada",
                    "El recurso solicitado no está disponible."
                );
            }
        }

        return plantilla(contenido);
    }

    private static String inicio() {
        String productos = PRODUCTOS.stream()
            .map(PaginaWeb::tarjetaProducto)
            .collect(Collectors.joining("\n"));

        String albums = ALBUMS.stream()
            .map(PaginaWeb::tarjetaAlbum)
            .collect(Collectors.joining("\n"));

        return """
            <section class="banner">
                <div class="banner-contenido">
                    <p class="etiqueta">NEXUS UMG · TECNOLOGÍA Y GAMING</p>
                    <h1>Tecnología para otros mundos</h1>
                    <p>
                        Tecnología, gaming, accesorios y música
                        para acompañarte en cada expedición.
                    </p>
                    <a class="boton" href="/tecnologia">Explorar tienda →</a>
                </div>
            </section>

            <section class="seccion">
                <div class="encabezado-seccion">
                    <div>
                        <p class="etiqueta">EQUIPO SELECCIONADO</p>
                        <h2>Selección de la expedición</h2>
                        <p>Explora nuestro stock actual.</p>
                    </div>
                    <span class="contador">%d PRODUCTOS</span>
                </div>

                <div class="productos">
                    %s
                </div>
            </section>

            <section class="franja">
                <div>
                    <p class="etiqueta">EQUIPO QUE VA CONTIGO</p>
                    <h2>Tu próxima aventura comienza aquí</h2>
                </div>
                <a class="enlace" href="/gaming">Explorar gaming →</a>
            </section>

            <section class="seccion">
                <div class="encabezado-seccion">
                    <div>
                        <p class="etiqueta">OTRAS FORMAS DE EXPLORAR</p>
                        <h2>Archivo sonoro</h2>
                        <p>Álbumes que forman parte de la identidad de Nexus UMG.</p>
                    </div>
                    <a class="enlace" href="/musica">Ver música →</a>
                </div>

                <div class="albums">
                    %s
                </div>
            </section>
            """.formatted(PRODUCTOS.size(), productos, albums);
    }

    private static String paginaCategoria(String ruta) {
        List<Producto> encontrados;
        String titulo;
        String descripcion;

        if (ruta.equals(Rutas.GAMING)) {
            titulo = "Gaming";
            descripcion = "Equipo para jugar y completar tu setup.";

            encontrados = PRODUCTOS.stream()
                .filter(p -> p.grupo.equals("gaming"))
                .collect(Collectors.toList());

        } else if (ruta.equals(Rutas.ACCESORIOS)) {
            titulo = "Accesorios";
            descripcion = "Accesorios para trabajar y disfrutar cada día.";

            encontrados = PRODUCTOS.stream()
                .filter(p -> p.grupo.equals("accesorios"))
                .collect(Collectors.toList());

        } else {
            titulo = "Tecnología";
            descripcion = "Explora el catálogo completo de Nexus UMG.";
            encontrados = PRODUCTOS;
        }

        String tarjetas = encontrados.stream()
            .map(PaginaWeb::tarjetaProducto)
            .collect(Collectors.joining("\n"));

        return """
            <section class="seccion">
                <p class="etiqueta">NEXUS UMG</p>
                <h1>%s</h1>
                <p>%s</p>
                <div class="productos">%s</div>
            </section>
            """.formatted(titulo, descripcion, tarjetas);
    }

    private static String paginaMusica() {
        String tarjetas = ALBUMS.stream()
            .map(PaginaWeb::tarjetaAlbum)
            .collect(Collectors.joining("\n"));

        return """
            <section class="seccion">
                <p class="etiqueta">ARCHIVO SONORO</p>
                <h1>La música de la expedición</h1>
                <p>Descubre nuestras referencias musicales.</p>
                <div class="albums">%s</div>
            </section>
            """.formatted(tarjetas);
    }

    private static String paginaEquipo() {
        String miembros = MIEMBROS.stream()
            .map(PaginaWeb::tarjetaMiembro)
            .collect(Collectors.joining("\n"));

        return """
            <section class="seccion">
                <h1>Miembros</h1>
                <div class="miembros">
                    %s
                </div>
            </section>
            """.formatted(miembros);
    }

    private static String tarjetaMiembro(Miembro miembro) {
        return """
            <article class="miembro">
                <div class="miembro-imagen">
                    <img src="/imagenes/%s" alt="%s" loading="lazy">
                </div>
                <div class="miembro-info">
                    <h3>%s</h3>
                </div>
            </article>
            """.formatted(miembro.imagen, miembro.nombre, miembro.nombre);
    }

    private static String tarjetaProducto(Producto producto) {
        return """
            <article class="producto">
                <a class="producto-imagen" href="%s">
                    <span class="etiqueta-producto">%s</span>
                    <img src="/imagenes/%s" alt="%s" loading="lazy">
                </a>

                <div class="producto-info">
                    <div class="producto-titulo">
                        <h3>%s</h3>
                        <strong>%s</strong>
                    </div>

                    <p class="descripcion">%s</p>

                    <a class="boton-producto" href="%s">
                        Ver producto →
                    </a>
                </div>
            </article>
            """.formatted(
                producto.ruta,
                producto.categoria.toUpperCase(),
                producto.imagen,
                producto.nombre,
                producto.nombre,
                producto.precio,
                producto.descripcion,
                producto.ruta
            );
    }

    private static String tarjetaAlbum(Album album) {
        return """
            <article class="album">
                <a class="album-imagen" href="%s">
                    <img src="/imagenes/%s" alt="%s" loading="lazy">
                </a>

                <div class="album-info">
                    <p class="etiqueta">REFERENCIA MUSICAL</p>
                    <h3>%s</h3>
                    <p>%s</p>
                    <a class="enlace" href="%s">Ver álbum →</a>
                </div>
            </article>
            """.formatted(
                album.ruta,
                album.imagen,
                album.nombre,
                album.nombre,
                album.descripcion,
                album.ruta
            );
    }

    private static String detalleProducto(Producto producto) {
        return """
            <section class="detalle">
                <a class="enlace" href="/productos">← Volver al catálogo</a>

                <div class="detalle-contenido">
                    <div class="detalle-imagen">
                        <img src="/imagenes/%s" alt="%s">
                    </div>

                    <div>
                        <p class="etiqueta">%s</p>
                        <h1>%s</h1>
                        <h2>%s</h2>
                        <p>%s</p>
                        <a class="boton" href="/servicios">Consultar disponibilidad →</a>
                    </div>
                </div>
            </section>
            """.formatted(
                producto.imagen,
                producto.nombre,
                producto.categoria,
                producto.nombre,
                producto.precio,
                producto.descripcion
            );
    }

    private static String detalleAlbum(Album album) {
        return """
            <section class="detalle">
                <a class="enlace" href="/musica">← Volver al archivo sonoro</a>

                <div class="detalle-contenido">
                    <div class="detalle-imagen">
                        <img src="/imagenes/%s" alt="%s">
                    </div>

                    <div>
                        <p class="etiqueta">ARCHIVO SONORO</p>
                        <h1>%s</h1>
                        <p>%s</p>
                        <a class="boton" href="/musica">Explorar álbumes →</a>
                    </div>
                </div>
            </section>
            """.formatted(
                album.imagen,
                album.nombre,
                album.nombre,
                album.descripcion
            );
    }

    private static String paginaInformativa(
        String titulo,
        String descripcion
    ) {
        return """
            <section class="seccion informativa">
                <p class="etiqueta">NEXUS UMG</p>
                <h1>%s</h1>
                <p>%s</p>
                <a class="boton" href="/">Volver al inicio →</a>
            </section>
            """.formatted(titulo, descripcion);
    }

    private static Producto buscarProducto(String ruta) {
        return PRODUCTOS.stream()
            .filter(p -> p.ruta.equals(ruta))
            .findFirst()
            .orElse(null);
    }

    private static Album buscarAlbum(String ruta) {
        return ALBUMS.stream()
            .filter(a -> a.ruta.equals(ruta))
            .findFirst()
            .orElse(null);
    }

    private static String plantilla(String contenido) {
        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="theme-color" content="#10171d">

                <title>Nexus UMG | Tecnología para otros mundos</title>

                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

                <style>
                    * {
                        box-sizing: border-box;
                    }

                    :root {
                        --noche: #10171d;
                        --profundo: #0a0f13;
                        --marfil: #f0e6d6;
                        --oro: #c9a46a;
                        --texto: #e8e0d4;
                        --muted: #aab0b5;
                    }

                    body {
                        margin: 0;
                        background: var(--noche);
                        color: var(--texto);
                        font-family: 'Inter', Arial, sans-serif;
                        line-height: 1.5;
                    }

                    a {
                        color: inherit;
                        text-decoration: none;
                    }

                    .site-header {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        flex-wrap: wrap;
                        gap: 18px;
                        padding: 18px clamp(20px, 5vw, 72px);
                        background: var(--profundo);
                        border-bottom: 1px solid #514532;
                    }

                    .marca {
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: 30px;
                        letter-spacing: 1.5px;
                    }

                    .marca span {
                        color: var(--oro);
                    }

                    .nav-links {
                        display: flex;
                        gap: 22px;
                        flex-wrap: wrap;
                    }

                    .nav-links a,
                    .enlace {
                        color: #ded6c9;
                        font-size: 12px;
                    }

                    a:hover {
                        color: var(--oro);
                    }

                    .banner {
                        display: flex;
                        align-items: center;
                        min-height: clamp(400px, 48vw, 620px);
                        padding: clamp(30px, 7vw, 100px);
                        background-image:
                            linear-gradient(90deg, rgba(8,13,17,.93), rgba(8,13,17,.58) 55%, rgba(8,13,17,.16)),
                            url('/imagenes/encabezado.jpeg');
                        background-size: cover;
                        background-position: center;
                        border-bottom: 1px solid #514532;
                    }

                    .banner-contenido {
                        max-width: 580px;
                    }

                    .banner h1,
                    h1,
                    h2,
                    .album h3 {
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-weight: 500;
                    }

                    .banner h1 {
                        margin: 0;
                        font-size: clamp(46px, 7vw, 78px);
                        line-height: 1;
                        color: #f7eee0;
                    }

                    .banner p:not(.etiqueta) {
                        max-width: 440px;
                        margin: 22px 0;
                        color: #e1d9cd;
                        font-size: 14px;
                    }

                    .etiqueta {
                        color: var(--oro);
                        font-size: 10px;
                        font-weight: 600;
                        letter-spacing: 2px;
                        text-transform: uppercase;
                    }

                    .boton,
                    .boton-producto {
                        display: inline-block;
                        padding: 11px 15px;
                        border: 1px solid var(--oro);
                        background: var(--oro);
                        color: #171b1e;
                        font-size: 11px;
                        font-weight: 600;
                    }

                    .boton:hover,
                    .boton-producto:hover {
                        background: transparent;
                        color: var(--marfil);
                    }

                    .seccion {
                        max-width: 1500px;
                        margin: auto;
                        padding: 50px clamp(20px, 5vw, 72px);
                    }

                    .encabezado-seccion {
                        display: flex;
                        align-items: end;
                        justify-content: space-between;
                        gap: 20px;
                        margin-bottom: 25px;
                    }

                    h1,
                    h2 {
                        margin-top: 0;
                        color: #f5ecdf;
                    }

                    h2 {
                        font-size: clamp(32px, 4vw, 46px);
                        line-height: 1.1;
                        margin-bottom: 8px;
                    }

                    .contador {
                        color: var(--oro);
                        font-size: 10px;
                        letter-spacing: 1px;
                    }

                    .productos {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(0, 1fr));
                        gap: 20px;
                        margin-top: 25px;
                    }

                    .miembros {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(0, 1fr));
                        gap: 20px;
                        margin-top: 25px;
                    }

                    .miembro {
                        display: grid;
                        grid-template-columns: minmax(105px, 38%) minmax(0, 1fr);
                        min-width: 0;
                        overflow: hidden;
                        background: #eee5d7;
                        color: #202328;
                        border: 1px solid #b7a78e;
                    }

                    .miembro-imagen {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 210px;
                        padding: 12px;
                        background: radial-gradient(ellipse, #fffaf1, #e5d9c7 75%, #c9b99e);
                    }

                    .miembro-imagen img {
                        display: block;
                        width: 100%;
                        height: 100%;
                        max-height: 260px;
                        object-fit: contain;
                    }

                    .miembro-info {
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        min-width: 0;
                        padding: 14px;
                    }

                    .miembro h3 {
                        margin: 0;
                        font-size: 14px;
                        line-height: 1.35;
                    }

                    .producto {
                        display: flex;
                        flex-direction: column;
                        min-width: 0;
                        overflow: hidden;
                        background: #eee5d7;
                        color: #202328;
                        border: 1px solid #b7a78e;
                    }

                    .producto-imagen {
                        height: 235px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        position: relative;
                        padding: 18px;
                        background: radial-gradient(ellipse, #fffaf1, #e5d9c7 75%, #c9b99e);
                    }

                    .producto-imagen img {
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                    }

                    .etiqueta-producto {
                        position: absolute;
                        top: 12px;
                        left: 12px;
                        padding: 5px 7px;
                        background: #151d23;
                        color: var(--marfil);
                        font-size: 8px;
                        letter-spacing: 1px;
                    }

                    .producto-info {
                        display: flex;
                        flex-direction: column;
                        flex: 1;
                        padding: 16px;
                    }

                    .producto-titulo {
                        display: flex;
                        justify-content: space-between;
                        gap: 10px;
                    }

                    .producto h3 {
                        margin: 0;
                        font-size: 14px;
                    }

                    .producto-titulo strong {
                        font-size: 11px;
                        text-align: right;
                    }

                    .descripcion {
                        min-height: 50px;
                        color: #64615d;
                        font-size: 11px;
                    }

                    .boton-producto {
                        margin-top: auto;
                        text-align: center;
                        background: #171d22;
                        color: var(--marfil);
                        border-color: #171d22;
                    }

                    .franja {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        gap: 22px;
                        padding: 35px clamp(20px, 7vw, 110px);
                        background: #172128;
                        border-block: 1px solid #514532;
                    }

                    .franja h2 {
                        margin-bottom: 0;
                    }

                    .albums {
                        display: grid;
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                        gap: 22px;
                    }

                    .album {
                        display: grid;
                        grid-template-columns: minmax(100px, 170px) minmax(0, 1fr);
                        align-items: center;
                        gap: 18px;
                        padding: 16px;
                        background: #151e24;
                        border: 1px solid #514532;
                    }

                    .album-imagen {
                        aspect-ratio: 1;
                        display: block;
                        overflow: hidden;
                        background: #0b0f12;
                    }

                    .album-imagen img {
                        width: 100%;
                        height: 100%;
                        object-fit: cover;
                    }

                    .album h3 {
                        margin: 0 0 8px;
                        font-size: clamp(24px, 3vw, 34px);
                        line-height: 1.05;
                    }

                    .album p:not(.etiqueta) {
                        color: var(--muted);
                        font-size: 11px;
                    }

                    .detalle {
                        max-width: 1250px;
                        min-height: 65vh;
                        margin: auto;
                        padding: 50px clamp(20px, 5vw, 72px);
                    }

                    .detalle-contenido {
                        display: grid;
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                        align-items: center;
                        gap: 45px;
                        margin-top: 25px;
                    }

                    .detalle-imagen {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 320px;
                        padding: 25px;
                        background: radial-gradient(ellipse, #fffaf1, #e5d9c7 75%, #c9b99e);
                    }

                    .detalle-imagen img {
                        display: block;
                        max-width: 100%;
                        max-height: 420px;
                        object-fit: contain;
                    }

                    .detalle h1 {
                        font-size: clamp(38px, 5vw, 60px);
                        margin-bottom: 10px;
                    }

                    .detalle h2 {
                        font-family: 'Inter', Arial, sans-serif;
                        font-size: 18px;
                    }

                    .detalle p:not(.etiqueta) {
                        color: var(--muted);
                    }

                    .informativa {
                        min-height: 65vh;
                    }

                    .site-footer {
                        display: flex;
                        flex-wrap: wrap;
                        justify-content: space-between;
                        gap: 26px;
                        padding: 30px clamp(20px, 5vw, 72px);
                        background: var(--profundo);
                        border-top: 1px solid #514532;
                    }

                    .site-footer p,
                    .site-footer a:not(.marca) {
                        color: var(--muted);
                        font-size: 11px;
                    }

                    .site-footer a {
                        display: block;
                        margin-top: 7px;
                    }

                    @media (max-width: 900px) {
                        .productos {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                        }

                        .miembros {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                        }

                        .albums {
                            grid-template-columns: 1fr;
                        }
                    }

                    @media (max-width: 600px) {
                        .site-header {
                            padding: 16px 18px;
                        }

                        .nav-links {
                            width: 100%;
                            gap: 16px;
                            overflow-x: auto;
                            flex-wrap: nowrap;
                        }

                        .nav-links a {
                            white-space: nowrap;
                        }

                        .banner {
                            min-height: 420px;
                            padding: 35px 22px;
                            background-position: center;
                        }

                        .seccion {
                            padding: 35px 18px;
                        }

                        .productos {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                            gap: 12px;
                        }

                        .miembros {
                            grid-template-columns: 1fr;
                        }

                        .miembro {
                            grid-template-columns: minmax(100px, 32%) minmax(0, 1fr);
                        }

                        .miembro-imagen {
                            min-height: 190px;
                            padding: 8px;
                        }

                        .miembro-info {
                            padding: 12px;
                        }

                        .producto-imagen {
                            height: 165px;
                            padding: 10px;
                        }

                        .producto-info {
                            padding: 12px;
                        }

                        .producto-titulo {
                            flex-direction: column;
                            gap: 5px;
                        }

                        .producto-titulo strong {
                            text-align: left;
                        }

                        .descripcion {
                            min-height: 78px;
                        }

                        .franja {
                            align-items: flex-start;
                            flex-direction: column;
                        }

                        .album {
                            grid-template-columns: 100px minmax(0, 1fr);
                            gap: 12px;
                            padding: 12px;
                        }

                        .detalle {
                            padding: 35px 18px;
                        }

                        .detalle-contenido {
                            grid-template-columns: 1fr;
                            gap: 25px;
                        }

                        .detalle-imagen {
                            min-height: 250px;
                        }
                    }

                    @media (max-width: 380px) {
                        .productos {
                            grid-template-columns: 1fr;
                        }

                        .producto-imagen {
                            height: 230px;
                        }
                    }
                </style>
            </head>

            <body>
                <header class="site-header">
                    <a class="marca" href="/">NEXUS <span>UMG</span></a>

                    <nav class="nav-links">
                        <a href="/">Inicio</a>
                        <a href="/tecnologia">Tecnología</a>
                        <a href="/gaming">Gaming</a>
                        <a href="/accesorios">Accesorios</a>
                        <a href="/musica">Archivo sonoro</a>
                        <a href="/equipo">Equipo</a>
                    </nav>
                </header>

                <main>
                    {{CONTENIDO}}
                </main>

                <footer class="site-footer">
                    <div>
                        <a class="marca" href="/">NEXUS <span>UMG</span></a>
                        <p>Tecnología elegida con intención.</p>
                    </div>

                    <div>
                        <strong>Tienda</strong>
                        <a href="/">Inicio</a>
                        <a href="/productos">Productos</a>
                    </div>

                    <div>
                        <strong>Explora</strong>
                        <a href="/gaming">Gaming</a>
                        <a href="/accesorios">Accesorios</a>
                        <a href="/musica">Archivo sonoro</a>
                    </div>

                    <div>
                        <strong>Nexus UMG</strong>
                        <a href="/nosotros">Sobre nosotros</a>
                        <a href="/servicios">Servicios</a>
                    </div>
                </footer>
            </body>
            </html>
            """;

        return html.replace("{{CONTENIDO}}", contenido);
    }

    private record Miembro(
        String nombre,
        String imagen
    ) {
    }

    private record Producto(
        String ruta,
        String nombre,
        String precio,
        String imagen,
        String descripcion,
        String categoria,
        String grupo
    ) {
    }

    private record Album(
        String ruta,
        String nombre,
        String imagen,
        String descripcion
    ) {
    }
}