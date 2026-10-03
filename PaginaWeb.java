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

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='UTF-8'>
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
                    }

                    .barra {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 30px;
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

                        %s

                    </main>

                </div>

            </body>
            </html>
            """.formatted(contenido);
    }
}