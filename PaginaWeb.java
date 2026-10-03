public class PaginaWeb {

    public static String generar(String pagina, String hilo) {

        String contenido;

        if (pagina.equals("/solicitudes")) {

            contenido = """
                <h1>Solicitudes HTTP</h1>
                <p class='subtitulo'>Registro de las solicitudes recibidas por el servidor.</p>

                <div class='card'>
                    <span class='etiqueta'>SOLICITUD ACTUAL</span>
                    <h3>GET /solicitudes</h3>
                    <p>Atendida por: <b>%s</b></p>
                </div>
                """.formatted(hilo);

        } else if (pagina.equals("/hilos")) {

            contenido = """
                <h1>Hilos</h1>
                <p class='subtitulo'>Estado del Thread Pool utilizado por el servidor.</p>

                <div class='cards'>
                    <div class='card'>
                        <span class='etiqueta'>THREAD POOL</span>
                        <h2>10</h2>
                        <p>Hilos configurados</p>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>HILO ACTUAL</span>
                        <h3>%s</h3>
                        <p>Atendiendo esta solicitud</p>
                    </div>
                </div>
                """.formatted(hilo);

        } else if (pagina.equals("/clientes")) {

            contenido = """
                <h1>Clientes</h1>
                <p class='subtitulo'>Información de los clientes conectados al servidor.</p>

                <div class='card'>
                    <span class='etiqueta'>ESTADO</span>
                    <h3>Servidor preparado</h3>
                    <p>El servidor puede recibir nuevas conexiones.</p>
                </div>
                """;

        } else {

            contenido = """
                <div class='hero'>
                    <span class='etiqueta'>SERVIDOR WEB CONCURRENTE</span>
                    <h1>Servidor Concurrente</h1>
                    <p>Una tienda tecnológica desarrollada para demostrar
                       el funcionamiento de múltiples clientes y hilos.</p>
                    <a class='boton' href='/productos'>Ver productos</a>
                </div>

                <h2>Estado del servidor</h2>

                <div class='cards'>
                    <div class='card'>
                        <span class='etiqueta'>SERVIDOR</span>
                        <h3 class='online'>● ONLINE</h3>
                        <p>Funcionando correctamente</p>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>THREAD POOL</span>
                        <h3>10 hilos</h3>
                        <p>Solicitudes procesadas</p>
                    </div>

                    <div class='card'>
                        <span class='etiqueta'>SOLICITUD</span>
                        <h3>GET /</h3>
                        <p>Atendida por %s</p>
                    </div>
                </div>

                <h2>Productos destacados</h2>

                <div class='productos'>
                    <div class='producto'>
                        <div class='icono'>L</div>
                        <h3>Laptop Pro</h3>
                        <p>Alto rendimiento para trabajo y estudio.</p>
                        <strong>Q4,500</strong>
                        <a href='/comprar/1'>Comprar</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>H</div>
                        <h3>Headphones X</h3>
                        <p>Audio inalámbrico para todos los días.</p>
                        <strong>Q350</strong>
                        <a href='/comprar/2'>Comprar</a>
                    </div>

                    <div class='producto'>
                        <div class='icono'>K</div>
                        <h3>Keyboard Pro</h3>
                        <p>Teclado mecánico para productividad.</p>
                        <strong>Q450</strong>
                        <a href='/comprar/3'>Comprar</a>
                    </div>
                </div>
                """.formatted(hilo);
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                <title>NEXUS | UMG</title>

                <style>
                    * { box-sizing: border-box; }

                    body {
                        margin: 0;
                        font-family: Arial, sans-serif;
                        background: #f5f6f8;
                        color: #171717;
                    }

                    .contenedor {
                        display: flex;
                        min-height: 100vh;
                    }

                    .menu {
                        width: 235px;
                        background: #111827;
                        color: white;
                        padding: 28px 18px;
                    }

                    .logo {
                        font-size: 21px;
                        font-weight: bold;
                        padding: 8px;
                        margin-bottom: 35px;
                        letter-spacing: 1px;
                    }

                    .menu h4 {
                        color: #9ca3af;
                        font-size: 11px;
                        margin: 0 8px 10px;
                        text-transform: uppercase;
                    }

                    .menu a {
                        display: block;
                        color: #d1d5db;
                        text-decoration: none;
                        padding: 12px;
                        border-radius: 7px;
                        margin: 4px 0;
                    }

                    .menu a:hover {
                        background: #1f2937;
                        color: white;
                    }

                    .principal {
                        flex: 1;
                        padding: 35px;
                        max-width: 1250px;
                    }

                    .barra {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 35px;
                    }

                    .barra h2 {
                        margin: 0;
                    }

                    h1 {
                        font-size: 38px;
                        margin: 8px 0;
                    }

                    h2 {
                        margin-top: 35px;
                    }

                    .subtitulo,
                    .hero p,
                    .card p,
                    .producto p {
                        color: #6b7280;
                    }

                    .estado,
                    .online {
                        color: #16a34a;
                        font-weight: bold;
                    }

                    .hero {
                        background: white;
                        border: 1px solid #e5e7eb;
                        border-radius: 12px;
                        padding: 40px;
                        margin-bottom: 35px;
                    }

                    .hero p {
                        max-width: 600px;
                        line-height: 1.6;
                    }

                    .etiqueta {
                        color: #6b7280;
                        font-size: 11px;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }

                    .boton,
                    .producto a {
                        display: inline-block;
                        background: #171717;
                        color: white;
                        text-decoration: none;
                        padding: 10px 15px;
                        border-radius: 6px;
                        margin-top: 12px;
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
                        border: 1px solid #e5e7eb;
                        border-radius: 10px;
                        padding: 23px;
                    }

                    .card h2,
                    .card h3 {
                        margin: 10px 0 5px;
                    }

                    .producto {
                        transition: transform 0.15s;
                    }

                    .producto:hover {
                        transform: translateY(-3px);
                    }

                    .producto h3 {
                        margin-bottom: 8px;
                    }

                    .producto strong {
                        display: block;
                        font-size: 21px;
                        margin-top: 18px;
                    }

                    .icono {
                        width: 42px;
                        height: 42px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #f0f1f3;
                        border-radius: 8px;
                        font-weight: bold;
                        font-size: 18px;
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

                        .hero {
                            padding: 28px;
                        }
                    }
                </style>
            </head>

            <body>
                <div class='contenedor'>

                    <aside class='menu'>
                        <div class='logo'>NEXUS / UMG</div>

                        <h4>Servidor</h4>

                        <a href='/'>Dashboard</a>
                        <a href='/solicitudes'>Solicitudes</a>
                        <a href='/hilos'>Hilos</a>
                        <a href='/clientes'>Clientes</a>

                        <h4 style='margin-top:25px;'>Tienda</h4>
                        <a href='/productos'>Productos</a>
                    </aside>

                    <main class='principal'>
                        <div class='barra'>
                            <h2>Panel del servidor</h2>
                            <span class='estado'>● SERVIDOR ONLINE</span>
                        </div>

                        %s
                    </main>
                </div>
            </body>
            </html>
            """.formatted(contenido);
    }
}