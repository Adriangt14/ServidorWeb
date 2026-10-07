public class PaginaWeb {

    public static String generar(String pagina, String hilo) {

        String contenido;

        if (pagina.equals("/solicitudes")) {

            contenido = """
                <h1>Solicitudes HTTP</h1>
                <p>Aquí se mostrarán las solicitudes recibidas por el servidor.</p>

                <div class='card'>
                    <h3>Solicitud actual</h3>
                    <p>GET /solicitudes</p>
                    <p>Atendida por: %s</p>
                </div>
                """.formatted(hilo);

        } else if (pagina.equals("/hilos")) {

            contenido = """
                <h1>Hilos</h1>
                <p>Información sobre los hilos del servidor.</p>

                <div class='card'>
                    <h3>Thread Pool</h3>
                    <p>Hilos configurados: 10</p>
                    <p>Solicitud atendida por: %s</p>
                </div>
                """.formatted(hilo);

        } else if (pagina.equals("/clientes")) {

            contenido = """
                <h1>Clientes</h1>
                <p>Información de los clientes conectados al servidor.</p>

                <div class='card'>
                    <h3>Estado</h3>
                    <p>Servidor preparado para recibir clientes.</p>
                </div>
                """;

        } else if (pagina.equals("/productos")) {

            contenido = """
                <div class='encabezado-productos'>
                    <h1>Productos</h1>
                    <p>Productos disponibles actualmente.</p>
                </div>

                <h2 class='titulo-seccion'>Productos destacados</h2>

                <div class='productos'>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/Audifonos.png' alt='AirPods'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>AirPods</h3>
                                <strong>Q350.00</strong>
                            </div>

                            <p class='descripcion'>
                                Audífonos inalámbricos para escuchar música y fingir que no escuchaste cuando te llamaron.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(24)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/laptop.png' alt='Laptop'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Laptop</h3>
                                <strong>Q5,999.00</strong>
                            </div>

                            <p class='descripcion'>
                                Laptop para estudiar, programar y tener 37 pestañas abiertas como si nada pasara.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(18)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/Columbina.png' alt='Columbina'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Columbina</h3>
                                <strong>Q5,999.00</strong>
                            </div>

                            <p class='descripcion'>
                                Figura de colección de Columbina para decorar el escritorio y justificar gastos totalmente responsables.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(18)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/Ahri.png' alt='Ahri'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Ahri</h3>
                                <strong>Q5,999.00</strong>
                            </div>

                            <p class='descripcion'>
                                Figura de colección de Ahri para subir el ánimo cuando la ranked ya te bajó suficiente LP.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(18)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/Rin.png' alt='Rin'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Rin</h3>
                                <strong>Q5,999.00</strong>
                            </div>

                            <p class='descripcion'>
                                Figura de colección de Rin para quien jura que solo verá un capítulo y termina viendo toda la temporada.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(18)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/teclado.png' alt='Teclado'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Teclado Mecánico</h3>
                                <strong>Q425.00</strong>
                            </div>

                            <p class='descripcion'>
                                Teclado mecánico para programar, jugar y hacer suficiente ruido para que toda la casa lo sepa.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(31)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                    <div class='producto'>

                        <div class='producto-imagen'>
                            <img src='/imagenes/mouse.png' alt='Mouse'>
                            <span class='favorito'>♡</span>
                        </div>

                        <div class='producto-info'>

                            <div class='producto-nombre-precio'>
                                <h3>Mouse Gamer</h3>
                                <strong>Q225.00</strong>
                            </div>

                            <p class='descripcion'>
                                Mouse gamer de alta precisión para apuntar fino y culpar al ping cuando algo salga mal.
                            </p>

                            <div class='estrellas'>
                                ★★★★★
                                <span>(15)</span>
                            </div>

                            <button>Agregar al carrito</button>

                        </div>

                    </div>

                </div>
                """;

        } else {

            contenido = """
                <h1>Servidor Concurrente</h1>
                <p>Bienvenido al servidor web concurrente de la UMG.</p>

                <div class='cards'>

                    <div class='card'>
                        <h3>Servidor</h3>
                        <p class='estado'>● ONLINE</p>
                    </div>

                    <div class='card'>
                        <h3>Thread Pool</h3>
                        <p>10 hilos disponibles</p>
                    </div>

                    <div class='card'>
                        <h3>Solicitud</h3>
                        <p>GET /</p>
                    </div>

                </div>

                <div class='card'>
                    <h3>Actividad</h3>
                    <p>Solicitud atendida por: %s</p>
                </div>
                """.formatted(hilo);
        }

        String html = """
            <!DOCTYPE html>
            <html>

            <head>

                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>

                <title>Servidor UMG</title>

                <style>

                    * {
                        box-sizing: border-box;
                    }

                    body {
                        margin: 0;
                        font-family: Arial, sans-serif;
                        background: #f5f7fb;
                        color: #1f2937;
                    }

                    .contenedor {
                        display: flex;
                        min-height: 100vh;
                    }

                    .menu {
                        width: 230px;
                        background: #111827;
                        color: white;
                        padding: 25px 15px;
                        flex-shrink: 0;
                    }

                    .logo {
                        font-size: 22px;
                        font-weight: bold;
                        padding: 10px;
                        margin-bottom: 30px;
                    }

                    .menu h4 {
                        color: #9ca3af;
                        font-size: 12px;
                        margin-left: 10px;
                        text-transform: uppercase;
                    }

                    .menu a {
                        display: block;
                        color: #d1d5db;
                        text-decoration: none;
                        padding: 12px 10px;
                        border-radius: 6px;
                        margin: 5px 0;
                    }

                    .menu a:hover {
                        background: #1f2937;
                        color: white;
                    }

                    .principal {
                        flex: 1;
                        padding: 30px;
                        min-width: 0;
                    }

                    .barra {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        gap: 20px;
                        margin-bottom: 30px;
                    }

                    .barra h2 {
                        margin: 0;
                    }

                    .estado {
                        color: #16a34a;
                        font-weight: bold;
                    }

                    .cards {
                        display: flex;
                        gap: 20px;
                        margin: 25px 0;
                    }

                    .card {
                        background: white;
                        border-radius: 10px;
                        padding: 20px;
                        margin-bottom: 20px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                    }

                    .cards .card {
                        flex: 1;
                    }

                    .card h3 {
                        margin-top: 0;
                    }

                    .encabezado-productos h1 {
                        margin-bottom: 8px;
                    }

                    .encabezado-productos p {
                        color: #6b7280;
                        margin-top: 0;
                    }

                    .titulo-seccion {
                        margin-top: 35px;
                        margin-bottom: 20px;
                    }

                    .productos {
                        display: flex;
                        gap: 20px;
                        overflow-x: auto;
                        padding-bottom: 20px;
                        scroll-behavior: smooth;
                    }

                    .producto {
                        flex: 0 0 260px;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        border: 1px solid #e5e7eb;
                        transition: transform 0.2s, box-shadow 0.2s;
                        display: flex;
                        flex-direction: column;
                    }

                    .producto:hover {
                        transform: translateY(-4px);
                        box-shadow: 0 8px 20px rgba(0,0,0,0.10);
                    }

                    .producto-imagen {
                        height: 220px;
                        background: #f3f4f6;
                        position: relative;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }

                    .producto-imagen img {
                        width: 100%;
                        height: 100%;
                        object-fit: contain;
                        display: block;
                    }

                    .favorito {
                        position: absolute;
                        top: 12px;
                        right: 12px;
                        background: white;
                        width: 36px;
                        height: 36px;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 22px;
                        cursor: pointer;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.10);
                    }

                    .producto-info {
                        padding: 17px;
                        display: flex;
                        flex-direction: column;
                        flex: 1;
                    }

                    .producto-nombre-precio {
                        display: flex;
                        justify-content: space-between;
                        align-items: flex-start;
                        gap: 10px;
                    }

                    .producto-nombre-precio h3 {
                        margin: 0;
                        font-size: 17px;
                    }

                    .producto-nombre-precio strong {
                        white-space: nowrap;
                        font-size: 16px;
                    }

                    .descripcion {
                        color: #6b7280;
                        font-size: 13px;
                        min-height: 48px;
                        margin: 10px 0;
                    }

                    .estrellas {
                        color: #16a34a;
                        font-size: 15px;
                        margin-bottom: 15px;
                    }

                    .estrellas span {
                        color: #6b7280;
                        font-size: 12px;
                    }

                    .producto button {
                        background: white;
                        border: 1px solid #1f2937;
                        border-radius: 20px;
                        padding: 9px 16px;
                        cursor: pointer;
                        font-size: 13px;
                        margin-top: auto;
                        align-self: flex-start;
                    }

                    .producto button:hover {
                        background: #111827;
                        color: white;
                    }

                    @media (max-width: 768px) {

                        .contenedor {
                            flex-direction: column;
                        }

                        .menu {
                            width: 100%;
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
                            margin: 3px;
                            padding: 10px 12px;
                            font-size: 14px;
                        }

                        .principal {
                            padding: 20px;
                        }

                        .barra {
                            flex-direction: column;
                            align-items: flex-start;
                            gap: 10px;
                        }

                        .cards {
                            flex-direction: column;
                            gap: 0;
                        }

                        .producto {
                            flex: 0 0 240px;
                        }

                        .producto-imagen {
                            height: 200px;
                        }
                    }

                    @media (max-width: 480px) {

                        .principal {
                            padding: 16px;
                        }

                        .principal h1 {
                            font-size: 25px;
                        }

                        .barra h2 {
                            font-size: 20px;
                        }

                        .menu a {
                            font-size: 13px;
                            padding: 9px;
                        }

                        .producto {
                            flex: 0 0 225px;
                        }

                        .producto-imagen {
                            height: 185px;
                        }
                    }

                </style>

            </head>

            <body>

                <div class='contenedor'>

                    <aside class='menu'>

                        <div class='logo'>
                            SERVIDOR UMG
                        </div>

                        <h4>Servidor</h4>

                        <a href='/'>
                            Dashboard
                        </a>

                        <a href='/productos'>
                            Productos
                        </a>

                        <a href='/solicitudes'>
                            Solicitudes
                        </a>

                        <a href='/hilos'>
                            Hilos
                        </a>

                        <a href='/clientes'>
                            Clientes
                        </a>

                    </aside>

                    <main class='principal'>

                        <div class='barra'>

                            <h2>Panel del servidor</h2>

                            <span class='estado'>
                                ● SERVIDOR ONLINE
                            </span>

                        </div>

                        {{CONTENIDO}}

                    </main>

                </div>

            </body>

            </html>
            """;

        return html.replace("{{CONTENIDO}}", contenido);
    }
}