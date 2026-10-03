public class PaginaWeb {

    public static String generar(String pagina, String hilo) {

        String contenido;

        if (pagina.equals("/productos")) {

            contenido = """
                <div class='encabezado'>
                    <span class='etiqueta'>CATÁLOGO</span>
                    <h1>Productos</h1>
                </div>

                <div class='productos'>

                    <div class='producto'>
                        <div class='icono'>L</div>
                        <h3>Laptop Pro</h3>
                        <strong>Q4,500</strong>
                        <a href='/producto/1'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>H</div>
                        <h3>Headphones X</h3>
                        <strong>Q350</strong>
                        <a href='/producto/2'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>K</div>
                        <h3>Keyboard Pro</h3>
                        <strong>Q450</strong>
                        <a href='/producto/3'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>M</div>
                        <h3>Mouse Pro</h3>
                        <strong>Q250</strong>
                        <a href='/producto/4'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>D</div>
                        <h3>Monitor 24"</h3>
                        <strong>Q1,200</strong>
                        <a href='/producto/5'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>G</div>
                        <h3>Gamepad X</h3>
                        <strong>Q500</strong>
                        <a href='/producto/6'>Ver producto</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>S</div>
                        <h3>SSD 1TB</h3>
                        <strong>Q750</strong>
                        <a href='/producto/7'>Ver producto</a>
                    </div>

                    <div class='producto error'>
                        <div class='icono'>!</div>
                        <h3>Error 404</h3>
                        <strong>Prueba</strong>
                        <a href='/producto/error'>Abrir</a>
                    </div>

                </div>
                """;

        } else if (pagina.startsWith("/producto/")) {

            String id = pagina.substring("/producto/".length());

            String nombre = switch (id) {
                case "1" -> "Laptop Pro";
                case "2" -> "Headphones X";
                case "3" -> "Keyboard Pro";
                case "4" -> "Mouse Pro";
                case "5" -> "Monitor 24\"";
                case "6" -> "Gamepad X";
                case "7" -> "SSD 1TB";
                default -> "Producto";
            };

            String precio = switch (id) {
                case "1" -> "Q4,500";
                case "2" -> "Q350";
                case "3" -> "Q450";
                case "4" -> "Q250";
                case "5" -> "Q1,200";
                case "6" -> "Q500";
                case "7" -> "Q750";
                default -> "";
            };

            contenido = """
                <div class='detalle'>
                    <span class='etiqueta'>PRODUCTO %s</span>
                    <h1>%s</h1>
                    <div class='precio'>%s</div>

                    <div class='datos'>
                        <p><b>Ruta:</b> /producto/%s</p>
                        <p><b>Hilo:</b> %s</p>
                    </div>

                    <a class='boton' href='/productos'>Volver a productos</a>
                </div>
                """.formatted(id, nombre, precio, id, hilo);

        } else if (pagina.equals("/solicitudes")) {

            contenido = """
                <div class='detalle'>
                    <span class='etiqueta'>SERVIDOR</span>
                    <h1>Solicitudes HTTP</h1>
                    <p>GET /solicitudes</p>

                    <div class='datos'>
                        <p><b>Recibidas:</b> %s</p>
                        <p><b>Procesadas:</b> %s</p>
                        <p><b>En proceso:</b> %s</p>
                        <p><b>En cola:</b> %s</p>
                    </div>
                </div>
                """.formatted(
                    Estadisticas.getRecibidas(),
                    Estadisticas.getProcesadas(),
                    Estadisticas.getActivas(),
                    Estadisticas.getEnCola()
                );

        } else if (pagina.equals("/hilos")) {

            contenido = """
                <div class='detalle'>
                    <span class='etiqueta'>SERVIDOR</span>
                    <h1>Thread Pool</h1>

                    <div class='datos'>
                        <p><b>Hilos configurados:</b> 10</p>
                        <p><b>Hilos activos:</b> %s</p>
                        <p><b>Hilo actual:</b> %s</p>
                    </div>
                </div>
                """.formatted(
                    Estadisticas.getActivas(),
                    hilo
                );

        } else if (pagina.equals("/clientes")) {

            contenido = """
                <div class='detalle'>
                    <span class='etiqueta'>SERVIDOR</span>
                    <h1>Clientes</h1>

                    <div class='datos'>
                        <p><b>Conexiones activas:</b> %s</p>
                    </div>
                </div>
                """.formatted(
                    Estadisticas.getActivas()
                );

        } else {

            contenido = """
                <div class='hero'>
                    <span class='etiqueta'>NEXUS / UMG</span>
                    <h1>Servidor Concurrente</h1>
                    <p>Panel principal del servidor web.</p>
                    <a class='boton' href='/productos'>Ver productos</a>
                </div>

                <h2>Estado del servidor</h2>

                <div class='cards'>

                    <div class='card'>
                        <span class='etiqueta'>ESTADO</span>
                        <h3 class='online'>● ONLINE</h3>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>THREAD POOL</span>
                        <h3>10 hilos</h3>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>HILO ACTUAL</span>
                        <h3>%s</h3>
                    </div>

                </div>

                <h2>Actividad</h2>

                <div class='cards'>

                    <div class='card'>
                        <span class='etiqueta'>SOLICITUDES</span>
                        <h3>%s</h3>
                        <p>Recibidas</p>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>PROCESADAS</span>
                        <h3>%s</h3>
                        <p>Completadas</p>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>EN COLA</span>
                        <h3>%s</h3>
                        <p>Esperando un hilo</p>
                    </div>

                </div>

                <div class='card monitor'>

                    <div class='seccion-titulo'>
                        <h2>Cola de solicitudes</h2>
                        <span>%s pendientes</span>
                    </div>

                    <div class='cola'>
                        %s
                    </div>

                </div>

                <div class='seccion'>

                    <div class='seccion-titulo'>
                        <h2>Destacados</h2>
                        <a href='/productos'>Ver todos</a>
                    </div>

                    <div class='productos'>

                        <div class='producto'>
                            <div class='icono'>L</div>
                            <h3>Laptop Pro</h3>
                            <strong>Q4,500</strong>
                            <a href='/producto/1'>Ver producto</a>
                        </div>

                        <div class='producto'>
                            <div class='icono'>H</div>
                            <h3>Headphones X</h3>
                            <strong>Q350</strong>
                            <a href='/producto/2'>Ver producto</a>
                        </div>

                        <div class='producto'>
                            <div class='icono'>K</div>
                            <h3>Keyboard Pro</h3>
                            <strong>Q450</strong>
                            <a href='/producto/3'>Ver producto</a>
                        </div>

                    </div>
                </div>
                """.formatted(
                    hilo,
                    Estadisticas.getRecibidas(),
                    Estadisticas.getProcesadas(),
                    Estadisticas.getEnCola(),
                    Estadisticas.getEnCola(),
                    Estadisticas.getColaHtml()
                );
        }

        return plantilla(contenido);
    }

    public static String error404(String ruta, String hilo) {

        String contenido = """
            <div class='detalle error404'>
                <span class='etiqueta'>ERROR</span>
                <div class='codigo'>404</div>
                <h1>Página no encontrada</h1>
                <p>La ruta solicitada no existe.</p>

                <div class='datos'>
                    <p><b>Ruta:</b> %s</p>
                    <p><b>Hilo:</b> %s</p>
                </div>

                <a class='boton' href='/productos'>Volver a productos</a>
            </div>
            """.formatted(ruta, hilo);

        return plantilla(contenido);
    }

    private static String plantilla(String contenido) {

        return """
            <!DOCTYPE html>
            <html>

            <head>
                <meta charset='UTF-8'>
                <meta name='viewport'
                      content='width=device-width, initial-scale=1.0'>

                <title>NEXUS | UMG</title>

                <style>
                    * {
                        box-sizing: border-box;
                    }

                    body {
                        margin: 0;
                        font-family: Arial, sans-serif;
                        background: #f4f6f8;
                        color: #111827;
                    }

                    .contenedor {
                        display: flex;
                        min-height: 100vh;
                    }

                    .menu {
                        width: 235px;
                        background: #0f172a;
                        color: white;
                        padding: 28px 18px;
                    }

                    .logo {
                        font-size: 21px;
                        font-weight: bold;
                        padding: 8px;
                        margin-bottom: 40px;
                        letter-spacing: 1px;
                    }

                    .menu h4 {
                        color: #94a3b8;
                        font-size: 11px;
                        margin: 0 8px 10px;
                        text-transform: uppercase;
                    }

                    .menu a {
                        display: block;
                        color: #cbd5e1;
                        text-decoration: none;
                        padding: 12px;
                        border-radius: 7px;
                        margin: 4px 0;
                    }

                    .menu a:hover {
                        background: #1e293b;
                        color: white;
                    }

                    .principal {
                        flex: 1;
                        padding: 34px;
                        max-width: 1250px;
                    }

                    .barra {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 32px;
                    }

                    .barra h2 {
                        margin: 0;
                    }

                    h1 {
                        font-size: 40px;
                        margin: 8px 0 16px;
                    }

                    h2 {
                        margin-top: 35px;
                    }

                    p {
                        color: #64748b;
                    }

                    .estado,
                    .online {
                        color: #16a34a;
                        font-weight: bold;
                    }

                    .hero,
                    .detalle {
                        background: white;
                        border: 1px solid #e2e8f0;
                        border-radius: 12px;
                        padding: 40px;
                    }

                    .hero {
                        margin-bottom: 35px;
                    }

                    .etiqueta {
                        color: #64748b;
                        font-size: 11px;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }

                    .boton,
                    .producto a,
                    .seccion-titulo a {
                        display: inline-block;
                        text-decoration: none;
                        border-radius: 6px;
                    }

                    .boton {
                        background: #1d4ed8;
                        color: white;
                        padding: 11px 16px;
                    }

                    .boton:hover {
                        background: #1e40af;
                    }

                    .cards,
                    .productos {
                        display: grid;
                        grid-template-columns: repeat(3, 1fr);
                        gap: 18px;
                        margin-top: 18px;
                    }

                    .card,
                    .producto {
                        background: white;
                        border: 1px solid #e2e8f0;
                        border-radius: 10px;
                        padding: 22px;
                    }

                    .card h3 {
                        margin: 12px 0 0;
                    }

                    .card p {
                        margin-bottom: 0;
                    }

                    .producto:hover {
                        border-color: #cbd5e1;
                    }

                    .producto h3 {
                        margin: 18px 0 8px;
                    }

                    .producto strong {
                        display: block;
                        font-size: 21px;
                        margin: 12px 0;
                    }

                    .producto a {
                        background: #111827;
                        color: white;
                        padding: 9px 13px;
                    }

                    .producto a:hover {
                        background: #334155;
                    }

                    .icono {
                        width: 42px;
                        height: 42px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #eef2f7;
                        border-radius: 8px;
                        font-weight: bold;
                        font-size: 18px;
                        color: #334155;
                    }

                    .error .icono {
                        background: #fee2e2;
                        color: #b91c1c;
                    }

                    .error a {
                        background: #b91c1c;
                    }

                    .error a:hover {
                        background: #991b1b;
                    }

                    .seccion {
                        margin-top: 38px;
                    }

                    .seccion-titulo {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }

                    .seccion-titulo h2 {
                        margin: 0;
                    }

                    .seccion-titulo a {
                        color: #1d4ed8;
                        font-weight: bold;
                    }

                    .seccion-titulo span {
                        color: #64748b;
                        font-size: 14px;
                    }

                    .monitor {
                        margin-top: 25px;
                    }

                    .cola {
                        margin-top: 18px;
                        border-top: 1px solid #e2e8f0;
                    }

                    .cola-item {
                        display: flex;
                        justify-content: space-between;
                        gap: 12px;
                        padding: 12px 4px;
                        border-bottom: 1px solid #e2e8f0;
                        color: #334155;
                    }

                    .cola-item span {
                        color: #94a3b8;
                        font-size: 13px;
                    }

                    .vacio {
                        color: #64748b;
                    }

                    .datos {
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 14px 18px;
                        margin: 24px 0;
                        max-width: 650px;
                    }

                    .datos p {
                        margin: 8px 0;
                    }

                    .precio {
                        font-size: 30px;
                        font-weight: bold;
                        margin: 15px 0;
                    }

                    .codigo {
                        color: #b91c1c;
                        font-size: 72px;
                        font-weight: bold;
                    }

                    .error404 {
                        border-color: #fecaca;
                    }

                    @media (max-width: 800px) {

                        .contenedor {
                            flex-direction: column;
                        }

                        .menu {
                            width: 100%%;
                            padding: 15px;
                        }

                        .logo {
                            text-align: center;
                            margin-bottom: 15px;
                        }

                        .menu h4 {
                            display: none;
                        }

                        .menu a {
                            display: inline-block;
                            margin: 2px;
                        }

                        .principal {
                            width: 100%%;
                            padding: 22px;
                        }

                        .cards,
                        .productos {
                            grid-template-columns: 1fr;
                        }

                        .barra {
                            align-items: flex-start;
                            flex-direction: column;
                            gap: 10px;
                        }

                        h1 {
                            font-size: 30px;
                        }

                        .hero,
                        .detalle {
                            padding: 28px;
                        }

                        .cola-item {
                            align-items: flex-start;
                            flex-direction: column;
                        }
                    }
                </style>
            </head>

            <body>

                <div class='contenedor'>

                    <aside class='menu'>

                        <div class='logo'>
                            NEXUS / UMG
                        </div>

                        <h4>Servidor</h4>
                        <a href='/'>Dashboard</a>

                        <h4 style='margin-top:25px;'>Tienda</h4>
                        <a href='/productos'>Productos</a>

                    </aside>

                    <main class='principal'>

                        <div class='barra'>
                            <h2>Panel del servidor</h2>

                            <span class='estado'>
                                ● SERVIDOR ONLINE
                            </span>
                        </div>

                        %s

                    </main>

                </div>

            </body>
            </html>
            """.formatted(contenido);
    }
}
