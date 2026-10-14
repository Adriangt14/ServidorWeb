public class PaginaWeb {

    public static String generar(String pagina, String hilo) {

        String contenido;

        if (pagina.equals("/") || pagina.equals("/productos")) {

            String productos = String.join("\n",
                producto("AirPods", "Q350.00", "Audifonos.png",
                    "Audífonos inalámbricos para escuchar música y fingir que no escuchaste cuando te llamaron.",
                    "MÁS VENDIDO", "24"),

                producto("Laptop", "Q5,999.00", "laptop.png",
                    "Laptop para estudiar, programar y tener 37 pestañas abiertas como si nada pasara.",
                    "TECNOLOGÍA", "18"),

                producto("Columbina", "Q5,999.00", "Columbina.png",
                    "Figura de colección de Columbina para los amantes del coleccionismo.",
                    "COLECCIÓN", "18"),

                producto("Ahri", "Q5,999.00", "Ahri.png",
                    "Figura de colección de Ahri: una pieza para el setup y para el corazón.",
                    "COLECCIÓN", "18"),

                producto("Rin", "Q5,999.00", "Rin.png",
                    "Figura de colección de Rin para los exploradores que también aman el anime.",
                    "COLECCIÓN", "18"),

                producto("Teclado mecánico", "Q425.00", "teclado.png",
                    "Teclado mecánico para programar, jugar y hacer suficiente ruido para que toda la casa lo sepa.",
                    "GAMING", "31"),

                producto("Mouse gamer", "Q225.00", "mouse.png",
                    "Mouse gamer de alta precisión para apuntar fino y culpar al ping cuando algo salga mal.",
                    "GAMING", "15")
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
                            Tecnología, gaming y accesorios elegidos
                            para acompañarte en cada expedición.
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
                                Tecnología, gaming y piezas
                                para tu próximo viaje.
                            </p>
                        </div>

                        <span class='contador-productos'>
                            07 PRODUCTOS
                        </span>

                    </div>

                    <div class='productos'>
                        %s
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
                        Explorar selección <span>→</span>
                    </a>

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
                """.formatted(productos);

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
                        Esta sección está preparada para incorporar
                        las fichas individuales de los productos.
                    </p>

                    <a class='boton-banner boton-interno'
                       href='/productos'>
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

                    <a class='boton-banner boton-interno' href='/'>
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
                      href='https://fonts.gstatic.com'
                      crossorigin>

                <link href='https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap'
                      rel='stylesheet'>

                <style>

                    * {
                        box-sizing: border-box;
                    }

                    :root {
                        --noche: #10171d;
                        --noche-profunda: #0a0f13;
                        --marfil: #f0e6d6;
                        --oro: #c9a46a;
                        --texto: #e8e0d4;
                        --muted: #aab0b5;
                    }

                    html {
                        scroll-behavior: smooth;
                        scroll-padding-top: 90px;
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
                        min-height: 76px;
                        padding: 14px clamp(20px, 4.5vw, 72px);
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 26px;
                        background: rgba(10, 15, 19, .96);
                        border-bottom: 1px solid rgba(201, 164, 106, .28);
                        position: relative;
                        z-index: 2;
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
                        line-height: 1;
                    }

                    .marca-texto {
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: 30px;
                        line-height: 1;
                        letter-spacing: 1.4px;
                        color: #f8f1e7;
                    }

                    .marca-texto span {
                        color: var(--oro);
                    }

                    .nav-links {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: clamp(14px, 2.2vw, 32px);
                    }

                    .nav-links a {
                        text-decoration: none;
                        color: #d2d0cd;
                        font-size: 13px;
                        transition: color .2s ease;
                    }

                    .nav-links a:hover,
                    .nav-links a.activo {
                        color: var(--oro);
                    }

                    .header-accion {
                        flex-shrink: 0;
                        text-decoration: none;
                        color: var(--marfil);
                        border: 1px solid rgba(201, 164, 106, .65);
                        padding: 9px 14px;
                        border-radius: 3px;
                        font-size: 12px;
                        letter-spacing: .4px;
                    }

                    .header-accion:hover {
                        background: var(--oro);
                        color: var(--noche-profunda);
                    }

                    /* BANNER PRINCIPAL */

                    .banner {
                        min-height: clamp(430px, 49vw, 630px);
                        display: flex;
                        align-items: center;
                        position: relative;
                        padding: clamp(34px, 7vw, 105px);
                        overflow: hidden;

                        background-image:
                            linear-gradient(
                                90deg,
                                rgba(8, 13, 17, .93) 0%,
                                rgba(8, 13, 17, .72) 34%,
                                rgba(8, 13, 17, .12) 75%,
                                rgba(8, 13, 17, .06) 100%
                            ),
                            linear-gradient(
                                0deg,
                                rgba(8, 13, 17, .55),
                                transparent 45%
                            ),
                            url('/imagenes/encabezado.jpeg');

                        background-position:
                            center,
                            center,
                            center 48%;

                        background-size: cover;

                        border-bottom:
                            1px solid rgba(201, 164, 106, .32);
                    }

                    .banner-contenido {
                        max-width: 600px;
                        position: relative;
                        z-index: 1;
                    }

                    .banner-etiqueta,
                    .seccion-etiqueta {
                        margin: 0 0 12px;
                        color: var(--oro);
                        font-size: 10px;
                        font-weight: 600;
                        letter-spacing: 2.3px;
                    }

                    .banner h1,
                    .encabezado-seccion h2,
                    .franja-promocional h2,
                    .pagina-interna h1 {
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-weight: 500;
                        letter-spacing: -.5px;
                    }

                    .banner h1 {
                        max-width: 600px;
                        margin: 0;
                        font-size: clamp(46px, 5.8vw, 76px);
                        line-height: .98;
                        color: #f6eee2;
                        text-wrap: balance;
                    }

                    .banner-descripcion {
                        max-width: 440px;
                        margin: 22px 0 26px;
                        color: #ddd7ce;
                        font-size: 15px;
                    }

                    .boton-banner {
                        display: inline-flex;
                        align-items: center;
                        gap: 25px;
                        padding: 13px 19px;
                        background: var(--marfil);
                        color: #171b1e;
                        text-decoration: none;
                        font-size: 12px;
                        font-weight: 600;
                        border: 1px solid var(--marfil);
                        border-radius: 2px;
                        transition: background .2s, color .2s;
                    }

                    .boton-banner:hover {
                        background: transparent;
                        color: var(--marfil);
                    }

                    .boton-banner span {
                        font-size: 18px;
                    }

                    .banner-pie {
                        position: absolute;
                        bottom: 18px;
                        right: clamp(20px, 4.5vw, 72px);
                        font-size: 9px;
                        letter-spacing: 2px;
                        color: rgba(246, 238, 226, .72);
                    }

                    /* CATÁLOGO */

                    .catalogo {
                        max-width: 1550px;
                        margin: 0 auto;
                        padding: 54px clamp(20px, 4.5vw, 72px) 68px;
                    }

                    .encabezado-seccion {
                        display: flex;
                        align-items: end;
                        justify-content: space-between;
                        gap: 20px;
                        margin-bottom: 25px;
                    }

                    .encabezado-seccion .seccion-etiqueta {
                        margin-bottom: 5px;
                    }

                    .encabezado-seccion h2 {
                        margin: 0;
                        font-size: clamp(34px, 3.7vw, 48px);
                        color: #f5ecdf;
                        line-height: 1.06;
                    }

                    .subtitulo-seccion {
                        margin: 8px 0 0;
                        color: var(--muted);
                        font-size: 13px;
                    }

                    .contador-productos {
                        padding-bottom: 5px;
                        color: #d2b783;
                        font-size: 10px;
                        letter-spacing: 1.4px;
                        white-space: nowrap;
                    }

                    .productos {
                        display: grid;
                        grid-template-columns: repeat(4, minmax(0, 1fr));
                        gap: 20px;
                    }

                    .producto {
                        min-width: 0;
                        display: flex;
                        flex-direction: column;
                        overflow: hidden;
                        background: #eee5d7;
                        color: #202328;
                        border: 1px solid rgba(201, 164, 106, .27);
                        border-radius: 5px;
                        transition: transform .25s ease, box-shadow .25s ease;
                    }

                    .producto:hover {
                        transform: translateY(-4px);
                        box-shadow: 0 14px 30px rgba(0, 0, 0, .28);
                    }

                    .producto-imagen {
                        height: 215px;
                        position: relative;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 18px;
                        overflow: hidden;

                        background:
                            radial-gradient(
                                ellipse at center,
                                #fffaf1 0%,
                                #e5d9c7 76%,
                                #c9b99e 100%
                            );
                    }

                    .producto-imagen::after {
                        content: '';
                        position: absolute;
                        left: 12%;
                        right: 12%;
                        bottom: 15px;
                        height: 1px;
                        background:
                            linear-gradient(
                                90deg,
                                transparent,
                                rgba(107, 81, 49, .34),
                                transparent
                            );
                    }

                    .producto-imagen img {
                        position: relative;
                        z-index: 1;
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                        display: block;
                    }

                    .etiqueta-producto {
                        position: absolute;
                        top: 12px;
                        left: 12px;
                        z-index: 1;
                        padding: 5px 7px;
                        background: rgba(17, 23, 28, .9);
                        color: #f2e7d5;
                        font-size: 8px;
                        font-weight: 600;
                        letter-spacing: 1px;
                    }

                    .favorito {
                        position: absolute;
                        top: 10px;
                        right: 10px;
                        z-index: 1;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        width: 31px;
                        height: 31px;
                        border: 1px solid rgba(32, 35, 40, .13);
                        border-radius: 50%;
                        background: rgba(255, 250, 241, .92);
                        color: #30343a;
                        font-size: 19px;
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
                        align-items: start;
                        gap: 10px;
                    }

                    .producto-nombre-precio h3 {
                        margin: 0;
                        font-size: 14px;
                        font-weight: 600;
                        line-height: 1.35;
                    }

                    .producto-nombre-precio strong {
                        white-space: nowrap;
                        font-size: 13px;
                        color: #11171b;
                    }

                    .descripcion {
                        min-height: 58px;
                        margin: 10px 0 12px;
                        color: #64615d;
                        font-size: 11px;
                        line-height: 1.5;
                    }

                    .estrellas {
                        margin: auto 0 13px;
                        color: #b48642;
                        font-size: 13px;
                        letter-spacing: 1px;
                    }

                    .estrellas span {
                        margin-left: 5px;
                        color: #77716a;
                        font-size: 10px;
                        letter-spacing: 0;
                    }

                    .producto button {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        gap: 8px;
                        width: 100%;
                        margin-top: 0;
                        padding: 11px 12px;
                        border: 1px solid #171d22;
                        border-radius: 2px;
                        background: #171d22;
                        color: #f5eee4;
                        cursor: pointer;
                        font-family: inherit;
                        font-size: 11px;
                        transition: background .2s, color .2s;
                    }

                    .producto button:hover {
                        background: transparent;
                        color: #171d22;
                    }

                    /* FRANJA PROMOCIONAL */

                    .franja-promocional {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        gap: 28px;
                        padding: 42px clamp(20px, 7vw, 110px);
                        border-top: 1px solid rgba(201, 164, 106, .28);
                        border-bottom: 1px solid rgba(201, 164, 106, .28);
                        background: linear-gradient(100deg, #172128, #10171d 70%);
                    }

                    .franja-promocional .seccion-etiqueta {
                        margin-bottom: 6px;
                    }

                    .franja-promocional h2 {
                        margin: 0;
                        color: #f5ecdf;
                        font-size: clamp(30px, 3vw, 42px);
                        line-height: 1.05;
                    }

                    .franja-promocional p:not(.seccion-etiqueta) {
                        margin: 9px 0 0;
                        color: var(--muted);
                        font-size: 12px;
                    }

                    .enlace-promocional {
                        flex-shrink: 0;
                        text-decoration: none;
                        color: var(--marfil);
                        border-bottom: 1px solid var(--oro);
                        padding: 9px 0;
                        font-size: 12px;
                    }

                    .enlace-promocional span {
                        color: var(--oro);
                        margin-left: 10px;
                    }

                    /* BENEFICIOS */

                    .beneficios {
                        display: grid;
                        grid-template-columns: repeat(3, minmax(0, 1fr));
                        gap: 25px;
                        padding: 28px clamp(20px, 7vw, 110px);
                        border-bottom: 1px solid rgba(201, 164, 106, .2);
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
                        display: block;
                        color: #f0e6d6;
                        font-family: 'Cormorant Garamond', Georgia, serif;
                        font-size: 19px;
                        font-weight: 600;
                    }

                    .beneficio p {
                        margin: 2px 0 0;
                        color: var(--muted);
                        font-size: 10px;
                    }

                    /* PIE DE PÁGINA */

                    .site-footer {
                        display: flex;
                        align-items: start;
                        justify-content: space-between;
                        gap: 28px;
                        padding: 30px clamp(20px, 4.5vw, 72px) 36px;
                        background: var(--noche-profunda);
                    }

                    .footer-marca {
                        max-width: 330px;
                    }

                    .footer-marca .marca-texto {
                        font-size: 26px;
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
                        text-decoration: none;
                        color: #9da4a8;
                        font-size: 10px;
                    }

                    .footer-columna a:hover {
                        color: var(--oro);
                    }

                    /* PÁGINAS INTERNAS */

                    .pagina-interna {
                        min-height: 65vh;
                        max-width: 1100px;
                        margin: 0 auto;
                        padding: 70px clamp(22px, 6vw, 80px);
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
                        border: 1px solid rgba(201, 164, 106, .25);
                    }

                    .panel-interno h3 {
                        margin-top: 0;
                        color: var(--marfil);
                    }

                    .panel-interno p {
                        color: var(--muted);
                    }

                    .boton-interno {
                        margin-top: 15px;
                    }

                    /* TABLETS */

                    @media (max-width: 1100px) {

                        .site-header {
                            flex-wrap: wrap;
                        }

                        .nav-links {
                            order: 3;
                            width: 100%;
                            justify-content: flex-start;
                            padding-top: 5px;
                        }

                        .productos {
                            grid-template-columns: repeat(3, minmax(0, 1fr));
                        }
                    }

                    /* MÓVILES */

                    @media (max-width: 760px) {

                        .site-header {
                            gap: 14px;
                            padding: 15px 20px;
                        }

                        .marca-texto {
                            font-size: 26px;
                        }

                        .nav-links {
                            gap: 17px;
                            overflow-x: auto;
                            padding-bottom: 4px;
                        }

                        .nav-links a {
                            white-space: nowrap;
                            font-size: 12px;
                        }

                        .header-accion {
                            font-size: 11px;
                            padding: 8px 10px;
                        }

                        .banner {
                            min-height: 470px;
                            padding: 40px 26px;
                            background-position: center, center, 58% center;
                        }

                        .banner h1 {
                            max-width: 430px;
                            font-size: clamp(43px, 9vw, 62px);
                        }

                        .banner-descripcion {
                            max-width: 360px;
                            font-size: 13px;
                        }

                        .banner-pie {
                            font-size: 8px;
                        }

                        .catalogo {
                            padding-top: 38px;
                            padding-bottom: 45px;
                        }

                        .productos {
                            grid-template-columns: repeat(2, minmax(0, 1fr));
                            gap: 13px;
                        }

                        .producto-imagen {
                            height: 175px;
                            padding: 12px;
                        }

                        .producto-info {
                            padding: 12px;
                        }

                        .producto-nombre-precio {
                            flex-direction: column;
                            gap: 5px;
                        }

                        .descripcion {
                            min-height: 72px;
                        }

                        .franja-promocional {
                            align-items: flex-start;
                            flex-direction: column;
                            padding-top: 32px;
                            padding-bottom: 32px;
                        }

                        .beneficios {
                            grid-template-columns: 1fr;
                            gap: 18px;
                        }

                        .site-footer {
                            flex-wrap: wrap;
                        }
                    }

                    @media (max-width: 430px) {

                        .header-accion {
                            display: none;
                        }

                        .nav-links {
                            gap: 15px;
                        }

                        .banner {
                            min-height: 440px;
                        }

                        .encabezado-seccion {
                            align-items: flex-start;
                            flex-direction: column;
                            gap: 10px;
                        }

                        .productos {
                            grid-template-columns: 1fr;
                        }

                        .producto-imagen {
                            height: 230px;
                        }

                        .producto-nombre-precio {
                            flex-direction: row;
                        }

                        .descripcion {
                            min-height: 0;
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

                    <a class='marca' href='/' aria-label='Nexus UMG, inicio'>

                        <span class='marca-simbolo'>✧</span>

                        <span class='marca-texto'>
                            NEXUS <span>UMG</span>
                        </span>

                    </a>

                    <nav class='nav-links' aria-label='Navegación principal'>

                        <a class='activo' href='/'>Inicio</a>
                        <a href='/productos#productos'>Tecnología</a>
                        <a href='/productos#gaming'>Gaming</a>
                        <a href='/productos#productos'>Accesorios</a>
                        <a href='/productos#productos'>Ofertas</a>

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

                        <strong>Ayuda</strong>

                        <a href='/productos#servicios'>Atención al cliente</a>
                        <a href='/productos#servicios'>Información de la tienda</a>

                    </div>

                    <div class='footer-columna'>

                        <strong>Nexus UMG</strong>

                        <a href='/'>Sobre la tienda</a>
                        <a href='/productos'>Explorar catálogo</a>

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
        String etiqueta,
        String resenas
    ) {

        return """
            <article class='producto'>

                <div class='producto-imagen'>

                    <span class='etiqueta-producto'>%s</span>

                    <span class='favorito' aria-hidden='true'>♡</span>

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

                    <div class='estrellas'
                         aria-label='Calificación de cinco estrellas'>
                        ★★★★★ <span>(%s)</span>
                    </div>

                    <button type='button'>
                        ＋ Añadir al carrito
                    </button>

                </div>

            </article>
            """.formatted(
                etiqueta,
                imagen,
                nombre,
                nombre,
                precio,
                descripcion,
                resenas
            );
    }
}