public class PaginaWeb {

    public static String generar(String pagina, String hilo) {

        String contenido;

        if (pagina.equals("/") || pagina.equals("/productos")) {

            String productos = String.join("\n",

                producto(
                    "Audífonos Razer",
                    "Consultar precio",
                    "Audifonos_Razer.webp",
                    "Audífonos para disfrutar tus partidas, música y sesiones largas.",
                    "AUDIO"
                ),

                producto(
                    "Hatsune Miku",
                    "Consultar precio",
                    "Figura_Miku.png",
                    "Figura de colección para darle personalidad a tu espacio.",
                    "COLECCIÓN"
                ),

                producto(
                    "Garen Pingu",
                    "Consultar precio",
                    "Figura_Garen_Pingu.webp",
                    "Una figura coleccionable para los fans de League of Legends.",
                    "COLECCIÓN"
                ),

                producto(
                    "Logitech PRO",
                    "Consultar precio",
                    "Logitech_Pro.png",
                    "Periférico Logitech PRO para completar tu estación de juego.",
                    "PERIFÉRICOS"
                ),

                producto(
                    "Combo Logitech",
                    "Consultar precio",
                    "Logitech_Basico_Teclado.png",
                    "Teclado y mouse para trabajar, estudiar y utilizar todos los días.",
                    "OFICINA"
                ),

                producto(
                    "Teclado mecánico",
                    "Q425.00",
                    "teclado.png",
                    "Teclado para programar, jugar y completar tu setup.",
                    "GAMING"
                )
            );

            String albums = String.join("\n",

                album(
                    "Nothing But the Best",
                    "Album_Nothingbutthebest.webp",
                    "Una referencia musical para acompañar el viaje."
                ),

                album(
                    "La Voz de Oro",
                    "Album_Lavozdeoro.jpg",
                    "Una colección sonora que también forma parte de Nexus UMG."
                )
            );

            contenido = """
                <section class='banner'>

                    <div class='banner-contenido'>

                        <p class='banner-etiqueta'>
                            NEXUS UMG · TECNOLOGÍA Y GAMING
                        </p>

                        <h1>
                            Tecnología para otros mundos
                        </h1>

                        <p class='banner-descripcion'>
                            Tecnología, gaming y accesorios
                            elegidos para acompañarte
                            en cada expedición.
                        </p>

                        <a class='boton-banner' href='#productos'>
                            Explorar tienda <span>→</span>
                        </a>

                    </div>

                    <div class='banner-pie'>
                        UNA NUEVA EXPEDICIÓN COMIENZA AQUÍ
                    </div>

                </section>

                <section class='catalogo' id='productos'>

                    <div class='encabezado-seccion'>

                        <div>
                            <p class='seccion-etiqueta'>
                                EQUIPO SELECCIONADO
                            </p>

                            <h2>Selección de la expedición</h2>

                            <p class='subtitulo-seccion'>
                                Seis piezas para tu próxima aventura.
                            </p>
                        </div>

                        <span class='contador-productos'>
                            06 PRODUCTOS
                        </span>

                    </div>

                    <div class='productos'>
                        {{PRODUCTOS}}
                    </div>

                </section>

                <section class='franja-promocional' id='gaming'>

                    <div>

                        <p class='seccion-etiqueta'>
                            EQUIPO QUE VA CONTIGO
                        </p>

                        <h2>Encuentra tu próxima pieza</h2>

                        <p>
                            Herramientas para crear, jugar
                            y explorar más lejos.
                        </p>

                    </div>

                    <a href='#productos' class='enlace-promocional'>
                        Explorar selección →
                    </a>

                </section>

                <section class='archivo-sonoro' id='musica'>

                    <div class='encabezado-seccion'>

                        <div>
                            <p class='seccion-etiqueta'>
                                OTRAS FORMAS DE EXPLORAR
                            </p>

                            <h2>Archivo sonoro</h2>

                            <p class='subtitulo-seccion'>
                                Álbumes que también forman parte
                                de la identidad de Nexus UMG.
                            </p>
                        </div>

                        <span class='contador-productos'>
                            02 REFERENCIAS
                        </span>

                    </div>

                    <div class='albums'>
                        {{ALBUMS}}
                    </div>

                </section>

                <section class='beneficios' id='servicios'>

                    <div class='beneficio'>
                        <span class='beneficio-icono'>◇</span>

                        <div>
                            <strong>Selección cuidada</strong>
                            <p>Productos para tu día a día.</p>
                        </div>
                    </div>

                    <div class='beneficio'>
                        <span class='beneficio-icono'>⌂</span>

                        <div>
                            <strong>Atención cercana</strong>
                            <p>Estamos para ayudarte.</p>
                        </div>
                    </div>

                    <div class='beneficio'>
                        <span class='beneficio-icono'>✧</span>

                        <div>
                            <strong>Tu próxima aventura</strong>
                            <p>Empieza con el equipo adecuado.</p>
                        </div>
                    </div>

                </section>
                """;

            contenido = contenido
                .replace("{{PRODUCTOS}}", productos)
                .replace("{{ALBUMS}}", albums);

        } else if (pagina.equals("/solicitudes")) {

            contenido = """
                <section class='pagina-interna'>

                    <p class='seccion-etiqueta'>SISTEMA</p>

                    <h1>Solicitudes HTTP</h1>

                    <p>
                        Aquí se mostrarán las solicitudes
                        recibidas por el servidor.
                    </p>

                    <div class='panel-interno'>
                        <h3>Solicitud actual</h3>
                        <p>GET /solicitudes</p>
                        <p>Atendida por: %s</p>
                    </div>

                </section>
                """.formatted(hilo);

        } else if (pagina.equals("/hilos")) {

            contenido = """
                <section class='pagina-interna'>

                    <p class='seccion-etiqueta'>SISTEMA</p>

                    <h1>Hilos</h1>

                    <p>
                        Información sobre los hilos
                        de ejecución del servidor.
                    </p>

                    <div class='panel-interno'>
                        <h3>Thread Pool</h3>
                        <p>Hilos configurados: 10</p>
                        <p>Solicitud atendida por: %s</p>
                    </div>

                </section>
                """.formatted(hilo);

        } else if (pagina.equals("/clientes")) {

            contenido = """
                <section class='pagina-interna'>

                    <p class='seccion-etiqueta'>SISTEMA</p>

                    <h1>Clientes</h1>

                    <p>
                        Información de los clientes
                        conectados al servidor.
                    </p>

                    <div class='panel-interno'>
                        <h3>Estado</h3>
                        <p>Servidor preparado para recibir clientes.</p>
                    </div>

                </section>
                """;

        } else if (pagina.startsWith("/producto/")) {

            contenido = """
                <section class='pagina-interna'>

                    <p class='seccion-etiqueta'>NEXUS UMG</p>

                    <h1>Detalle del producto</h1>

                    <p>
                        La ficha individual estará disponible
                        en una siguiente versión.
                    </p>

                    <a class='boton-banner' href='/productos'>
                        Volver a la tienda →
                    </a>

                </section>
                """;

        } else {

            contenido = """
                <section class='pagina-interna'>

                    <p class='seccion-etiqueta'>NEXUS UMG</p>

                    <h1>Página no encontrada</h1>

                    <p>El recurso solicitado no está disponible.</p>

                    <a class='boton-banner' href='/'>
                        Volver al inicio →
                    </a>

                </section>
                """;
        }

        String html = """
            <!DOCTYPE html>
            <html lang='es'>

            <head>

                <meta charset='UTF-8'>

                <meta name='viewport'
                      content='width=device-width, initial-scale=1.0'>

                <meta name='theme-color' content='#10171d'>

                <title>Nexus UMG | Tecnología para otros mundos</title>

                <link rel='preconnect'
                      href='https://fonts.googleapis.com'>

                <link rel='preconnect'
                      href='https://fonts.gstatic.com' crossorigin>

                <link href='https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap'
                      rel='stylesheet'>

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

                    html {
                        scroll-behavior: smooth;
                        scroll-padding-top: 20px;
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
                    }

                    /* ENCABEZADO */

                    .site-header {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 24px;
                        padding: 16px clamp(20px, 5vw, 72px);
                        background: var(--profundo);
                        border-bottom: 1px solid rgba(201,164,106,.3);
                    }

                    .marca {
                        display: inline-flex;
                        align-items: center;
                        gap: 10px;
                        text-decoration: none;
                        white-space: nowrap;
                    }

                    .marca-simbolo {
                        color: var(--oro);
                        font-size: 30px;
                    }

                    .marca-texto {
                        color: #f8f1e7;
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: 30px;
                        letter-spacing: 1.4px;
                        line-height: 1;
                    }

                    .marca-texto span {
                        color: var(--oro);
                    }

                    .nav-links {
                        display: flex;
                        align-items: center;
                        gap: clamp(12px, 2vw, 28px);
                    }

                    .nav-links a {
                        color: #d2d0cd;
                        font-size: 12px;
                        text-decoration: none;
                        white-space: nowrap;
                    }

                    .nav-links a:hover {
                        color: var(--oro);
                    }

                    .header-accion {
                        flex-shrink: 0;
                        padding: 9px 13px;
                        border: 1px solid var(--oro);
                        color: var(--marfil);
                        font-size: 11px;
                        text-decoration: none;
                    }

                    .header-accion:hover {
                        background: var(--oro);
                        color: var(--profundo);
                    }

                    /* BANNER */

                    .banner {
                        min-height: clamp(430px, 49vw, 630px);
                        display: flex;
                        align-items: center;
                        position: relative;
                        padding: clamp(30px, 7vw, 100px);
                        overflow: hidden;
                        background-image:
                            linear-gradient(
                                90deg,
                                rgba(8,13,17,.93),
                                rgba(8,13,17,.65) 42%,
                                rgba(8,13,17,.12)
                            ),
                            linear-gradient(
                                0deg,
                                rgba(8,13,17,.5),
                                transparent 50%
                            ),
                            url('/imagenes/encabezado.jpeg');
                        background-position: center, center, center;
                        background-size: cover;
                        border-bottom: 1px solid rgba(201,164,106,.3);
                    }

                    .banner-contenido {
                        max-width: 580px;
                    }

                    .banner-etiqueta,
                    .seccion-etiqueta {
                        margin: 0 0 12px;
                        color: var(--oro);
                        font-size: 10px;
                        font-weight: 600;
                        letter-spacing: 2px;
                    }

                    .banner h1,
                    .encabezado-seccion h2,
                    .franja-promocional h2,
                    .pagina-interna h1 {
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-weight: 500;
                    }

                    .banner h1 {
                        margin: 0;
                        color: #f6eee2;
                        font-size: clamp(44px, 6vw, 76px);
                        line-height: .98;
                    }

                    .banner-descripcion {
                        max-width: 420px;
                        margin: 22px 0 26px;
                        color: #ddd7ce;
                        font-size: 14px;
                    }

                    .boton-banner {
                        display: inline-flex;
                        align-items: center;
                        gap: 20px;
                        padding: 12px 18px;
                        border: 1px solid var(--marfil);
                        background: var(--marfil);
                        color: #171b1e;
                        font-size: 12px;
                        font-weight: 600;
                        text-decoration: none;
                        transition: .2s;
                    }

                    .boton-banner:hover {
                        background: transparent;
                        color: var(--marfil);
                    }

                    .banner-pie {
                        position: absolute;
                        right: 25px;
                        bottom: 18px;
                        color: #e0d8cc;
                        font-size: 9px;
                        letter-spacing: 1.5px;
                    }

                    /* CATÁLOGO */

                    .catalogo,
                    .archivo-sonoro {
                        max-width: 1550px;
                        margin: 0 auto;
                        padding: 50px clamp(20px, 5vw, 72px) 60px;
                    }

                    .encabezado-seccion {
                        display: flex;
                        align-items: end;
                        justify-content: space-between;
                        gap: 20px;
                        margin-bottom: 25px;
                    }

                    .encabezado-seccion h2 {
                        margin: 0;
                        color: #f5ecdf;
                        font-size: clamp(32px, 4vw, 47px);
                        line-height: 1.05;
                    }

                    .subtitulo-seccion {
                        margin: 9px 0 0;
                        color: var(--muted);
                        font-size: 12px;
                    }

                    .contador-productos {
                        color: #d2b783;
                        font-size: 10px;
                        letter-spacing: 1.3px;
                        white-space: nowrap;
                    }

                    .productos {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(0, 1fr));
                        gap: 20px;
                    }

                    .producto {
                        min-width: 0;
                        display: flex;
                        flex-direction: column;
                        overflow: hidden;
                        border: 1px solid rgba(201,164,106,.27);
                        background: #eee5d7;
                        color: #202328;
                        transition: transform .2s, box-shadow .2s;
                    }

                    .producto:hover {
                        transform: translateY(-4px);
                        box-shadow: 0 12px 25px rgba(0,0,0,.25);
                    }

                    .producto-imagen {
                        height: 230px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        position: relative;
                        padding: 18px;
                        overflow: hidden;
                        background:
                            radial-gradient(
                                ellipse at center,
                                #fffaf1,
                                #e5d9c7 75%,
                                #c9b99e
                            );
                    }

                    .producto-imagen img {
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                        display: block;
                    }

                    .etiqueta-producto {
                        position: absolute;
                        top: 12px;
                        left: 12px;
                        padding: 5px 7px;
                        background: #151d23;
                        color: var(--marfil);
                        font-size: 8px;
                        font-weight: 600;
                        letter-spacing: 1px;
                    }

                    .producto-info {
                        display: flex;
                        flex-direction: column;
                        flex: 1;
                        padding: 16px;
                    }

                    .producto-nombre-precio {
                        display: flex;
                        justify-content: space-between;
                        align-items: flex-start;
                        gap: 10px;
                    }

                    .producto-nombre-precio h3 {
                        margin: 0;
                        font-size: 14px;
                        font-weight: 600;
                    }

                    .producto-nombre-precio strong {
                        max-width: 50%;
                        color: #171d22;
                        font-size: 11px;
                        text-align: right;
                    }

                    .descripcion {
                        min-height: 48px;
                        margin: 12px 0 18px;
                        color: #64615d;
                        font-size: 11px;
                    }

                    .producto button {
                        width: 100%;
                        margin-top: auto;
                        padding: 11px;
                        border: 1px solid #171d22;
                        background: #171d22;
                        color: #f5eee4;
                        font-family: inherit;
                        font-size: 11px;
                        cursor: pointer;
                    }

                    .producto button:hover {
                        background: transparent;
                        color: #171d22;
                    }

                    /* FRANJA GAMING */

                    .franja-promocional {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 25px;
                        padding: 40px clamp(20px, 7vw, 110px);
                        border-top: 1px solid rgba(201,164,106,.25);
                        border-bottom: 1px solid rgba(201,164,106,.25);
                        background: linear-gradient(100deg, #172128, #10171d);
                    }

                    .franja-promocional h2 {
                        margin: 0;
                        color: #f5ecdf;
                        font-size: clamp(30px, 4vw, 42px);
                        line-height: 1.05;
                    }

                    .franja-promocional p:not(.seccion-etiqueta) {
                        color: var(--muted);
                        font-size: 12px;
                    }

                    .enlace-promocional {
                        flex-shrink: 0;
                        color: var(--marfil);
                        font-size: 12px;
                        text-decoration: none;
                        border-bottom: 1px solid var(--oro);
                        padding: 9px 0;
                    }

                    /* MÚSICA */

                    .archivo-sonoro {
                        border-top: 1px solid rgba(201,164,106,.2);
                    }

                    .albums {
                        display: grid;
                        grid-template-columns: repeat(2, minmax(0, 1fr));
                        gap: 22px;
                    }

                    .album {
                        display: grid;
                        grid-template-columns: minmax(110px, 180px) minmax(0, 1fr);
                        align-items: center;
                        gap: 20px;
                        padding: 16px;
                        background: #151e24;
                        border: 1px solid rgba(201,164,106,.24);
                    }

                    .album-imagen {
                        aspect-ratio: 1;
                        overflow: hidden;
                        background: #0b0f12;
                    }

                    .album-imagen img {
                        display: block;
                        width: 100%;
                        height: 100%;
                        object-fit: cover;
                    }

                    .album-etiqueta {
                        margin: 0 0 8px;
                        color: var(--oro);
                        font-size: 9px;
                        letter-spacing: 1.4px;
                    }

                    .album h3 {
                        margin: 0 0 8px;
                        color: #f2e9dc;
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: clamp(24px, 3vw, 34px);
                        line-height: 1.05;
                    }

                    .album-descripcion {
                        margin: 0;
                        color: var(--muted);
                        font-size: 11px;
                    }

                    /* BENEFICIOS Y PIE */

                    .beneficios {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(0, 1fr));
                        gap: 25px;
                        padding: 28px clamp(20px, 7vw, 110px);
                        border-top: 1px solid rgba(201,164,106,.2);
                        border-bottom: 1px solid rgba(201,164,106,.2);
                    }

                    .beneficio {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                    }

                    .beneficio-icono {
                        color: var(--oro);
                        font-size: 27px;
                    }

                    .beneficio strong {
                        color: var(--marfil);
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: 19px;
                    }

                    .beneficio p {
                        margin: 2px 0 0;
                        color: var(--muted);
                        font-size: 10px;
                    }

                    .site-footer {
                        display: flex;
                        justify-content: space-between;
                        gap: 28px;
                        padding: 30px clamp(20px, 5vw, 72px);
                        background: var(--profundo);
                    }

                    .footer-marca {
                        max-width: 300px;
                    }

                    .footer-marca p {
                        color: var(--muted);
                        font-size: 11px;
                    }

                    .footer-columna strong {
                        color: #e8d7bc;
                        font-size: 11px;
                    }

                    .footer-columna a {
                        display: block;
                        margin-top: 7px;
                        color: #9da4a8;
                        font-size: 10px;
                        text-decoration: none;
                    }

                    .footer-columna a:hover {
                        color: var(--oro);
                    }

                    /* PÁGINAS INTERNAS */

                    .pagina-interna {
                        min-height: 65vh;
                        max-width: 1100px;
                        margin: 0 auto;
                        padding: 70px 25px;
                    }

                    .pagina-interna h1 {
                        margin: 0 0 10px;
                        font-size: clamp(38px, 5vw, 58px);
                    }

                    .pagina-interna > p:not(.seccion-etiqueta) {
                        color: var(--muted);
                    }

                    .panel-interno {
                        margin-top: 24px;
                        padding: 24px;
                        background: #171f25;
                        border: 1px solid rgba(201,164,106,.25);
                    }

                    .panel-interno h3 {
                        margin-top: 0;
                        color: var(--marfil);
                    }

                    .panel-interno p {
                        color: var(--muted);
                    }

                    /* TABLET */

                    @media (max-width: 1000px) {

                        .site-header {
                            flex-wrap: wrap;
                        }

                        .nav-links {
                            order: 3;
                            width: 100%;
                            justify-content: flex-start;
                            overflow-x: auto;
                            padding: 5px 0;
                        }

                        .productos {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                        }

                        .albums {
                            grid-template-columns: 1fr;
                        }

                        .beneficios {
                            grid-template-columns: 1fr;
                        }
                    }

                    /* MÓVIL */

                    @media (max-width: 600px) {

                        .site-header {
                            gap: 14px;
                            padding: 15px 18px;
                        }

                        .marca-texto {
                            font-size: 26px;
                        }

                        .nav-links {
                            gap: 18px;
                        }

                        .nav-links a {
                            font-size: 11px;
                        }

                        .header-accion {
                            display: none;
                        }

                        .banner {
                            min-height: 440px;
                            padding: 35px 24px;
                            background-position: center, center, 58% center;
                        }

                        .banner h1 {
                            font-size: clamp(42px, 10vw, 58px);
                        }

                        .banner-descripcion {
                            font-size: 13px;
                        }

                        .catalogo,
                        .archivo-sonoro {
                            padding: 36px 18px 42px;
                        }

                        .encabezado-seccion {
                            align-items: flex-start;
                            flex-direction: column;
                            gap: 10px;
                        }

                        .productos {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                            gap: 12px;
                        }

                        .producto-imagen {
                            height: 170px;
                            padding: 10px;
                        }

                        .producto-info {
                            padding: 12px;
                        }

                        .producto-nombre-precio {
                            flex-direction: column;
                            gap: 6px;
                        }

                        .producto-nombre-precio strong {
                            max-width: 100%;
                            text-align: left;
                        }

                        .descripcion {
                            min-height: 65px;
                        }

                        .franja-promocional {
                            align-items: flex-start;
                            flex-direction: column;
                            padding: 32px 22px;
                        }

                        .album {
                            grid-template-columns: 100px minmax(0, 1fr);
                            gap: 12px;
                            padding: 12px;
                        }

                        .album h3 {
                            font-size: 24px;
                        }

                        .site-footer {
                            flex-wrap: wrap;
                        }
                    }

                    @media (max-width: 380px) {

                        .productos {
                            grid-template-columns: 1fr;
                        }

                        .producto-imagen {
                            height: 230px;
                        }

                        .producto-nombre-precio {
                            flex-direction: row;
                        }

                        .producto-nombre-precio strong {
                            text-align: right;
                        }

                        .descripcion {
                            min-height: 0;
                        }

                        .album {
                            grid-template-columns: 85px minmax(0, 1fr);
                        }

                        .site-footer {
                            display: grid;
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                        }

                        .footer-marca {
                            grid-column: 1 / -1;
                        }
                    }

                </style>

            </head>

            <body>

                <header class='site-header'>

                    <a class='marca' href='/'>
                        <span class='marca-simbolo'>✧</span>

                        <span class='marca-texto'>
                            NEXUS <span>UMG</span>
                        </span>
                    </a>

                    <nav class='nav-links'>

                        <a href='/'>Inicio</a>
                        <a href='/productos#productos'>Tecnología</a>
                        <a href='/productos#gaming'>Gaming</a>
                        <a href='/productos#productos'>Accesorios</a>
                        <a href='/productos#musica'>Archivo sonoro</a>

                    </nav>

                    <a class='header-accion' href='/productos#productos'>
                        Explorar tienda ↗
                    </a>

                </header>

                <main>
                    {{CONTENIDO}}
                </main>

                <footer class='site-footer'>

                    <div class='footer-marca'>

                        <a class='marca' href='/'>
                            <span class='marca-texto'>
                                NEXUS <span>UMG</span>
                            </span>
                        </a>

                        <p>
                            Tecnología elegida con intención.
                            Hecho para tu próxima expedición.
                        </p>

                    </div>

                    <div class='footer-columna'>

                        <strong>Tienda</strong>

                        <a href='/'>Inicio</a>
                        <a href='/productos#productos'>Productos</a>
                        <a href='/productos#gaming'>Gaming</a>

                    </div>

                    <div class='footer-columna'>

                        <strong>Explora</strong>

                        <a href='/productos#musica'>Archivo sonoro</a>
                        <a href='/productos#servicios'>Servicios</a>

                    </div>

                    <div class='footer-columna'>

                        <strong>Nexus UMG</strong>

                        <a href='/'>Sobre la tienda</a>
                        <a href='/productos'>Catálogo</a>

                    </div>

                </footer>

            </body>
            </html>
            """;

        return html.replace("{{CONTENIDO}}", contenido);
    }

    private static String producto(
        String nombre,
        String precio,
        String imagen,
        String descripcion,
        String etiqueta
    ) {

        return """
            <article class='producto'>

                <div class='producto-imagen'>

                    <span class='etiqueta-producto'>%s</span>

                    <img src='/imagenes/%s'
                         alt='%s'
                         loading='lazy'>

                </div>

                <div class='producto-info'>

                    <div class='producto-nombre-precio'>

                        <h3>%s</h3>

                        <strong>%s</strong>

                    </div>

                    <p class='descripcion'>%s</p>

                    <button type='button'>
                        Consultar producto
                    </button>

                </div>

            </article>
            """.formatted(
                etiqueta,
                imagen,
                nombre,
                nombre,
                precio,
                descripcion
            );
    }

    private static String album(
        String titulo,
        String imagen,
        String descripcion
    ) {

        return """
            <article class='album'>

                <div class='album-imagen'>
                    <img src='/imagenes/%s'
                         alt='Portada de %s'
                         loading='lazy'>
                </div>

                <div>

                    <p class='album-etiqueta'>
                        REFERENCIA MUSICAL
                    </p>

                    <h3>%s</h3>

                    <p class='album-descripcion'>%s</p>

                </div>

            </article>
            """.formatted(
                imagen,
                titulo,
                titulo,
                descripcion
            );
    }
}