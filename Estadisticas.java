import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class Estadisticas {

    public enum TipoEvento {
        LIMITE_GLOBAL,
        LIMITE_PAGINAS,
        LIMITE_IMAGENES,
        CAPACIDAD_IPS,
        SATURACION,
        RUTA_PROTEGIDA,
        SOLICITUD_INVALIDA,
        ORIGEN_NO_VERIFICADO,
        LOGIN_EXITOSO,
        LOGIN_FALLIDO,
        LOGIN_BLOQUEADO,
        ACCESO_ADMIN_RECHAZADO
    }

    public enum MotivoSinRespuesta {
        CONEXION_VACIA,
        TIEMPO_AGOTADO,
        ERROR_IO,
        CANCELADA_AL_CERRAR
    }

    public record EventoReciente(
        long primeroMillis,
        long ultimoMillis,
        TipoEvento tipo,
        String ip,
        String detalle,
        int repeticiones
    ) {
    }

    public record RutaVisitada(String ruta, long total) {
    }

    public record Instantanea(
        long inicioMillis,
        long momentoMillis,
        long recibidas,
        long atendidas,
        long aceptadas,
        long rechazadasPorCapacidad,
        long bloqueadasPorLimitador,
        int activas,
        int enCola,
        Map<Integer, Long> respuestasPorCodigo,
        Map<TipoEvento, Long> eventosPorTipo,
        Map<MotivoSinRespuesta, Long> sinRespuesta
    ) {
    }

    private static final int MAX_EVENTOS_RECIENTES = 100;
    private static final int MAX_LONGITUD_TEXTO = 120;
    private static final long VENTANA_AGRUPACION_MILLIS = 10_000;
    private static final long INICIO_MILLIS = System.currentTimeMillis();

    private static final AtomicInteger recibidas = new AtomicInteger();
    private static final AtomicInteger procesadas = new AtomicInteger();
    private static final AtomicInteger rechazadas = new AtomicInteger();
    private static final AtomicInteger activas = new AtomicInteger();
    private static final AtomicLong consecutivo = new AtomicLong();
    private static final AtomicInteger enCola = new AtomicInteger();

    private static final ConcurrentSkipListMap<Long, Solicitud> cola =
        new ConcurrentSkipListMap<>();

    private static final ConcurrentHashMap<String, Solicitud> solicitudes =
        new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, LongAdder> topRutas =
        new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, LongAdder> respuestasPorCodigo =
        new ConcurrentHashMap<>();

    private static final EnumMap<TipoEvento, LongAdder> eventosPorTipo =
        crearContadores(TipoEvento.class);

    private static final EnumMap<MotivoSinRespuesta, LongAdder> sinRespuesta =
        crearContadores(MotivoSinRespuesta.class);

    private static final ArrayDeque<Evento> eventosRecientes = new ArrayDeque<>();
    private static final Object BLOQUEO_EVENTOS = new Object();

    private static final boolean LOG_DETALLADO = false;

    public static String registrarSolicitud(String ip) {
        long numero = consecutivo.incrementAndGet();
        String id = Long.toString(numero);
        Solicitud solicitud = new Solicitud(id, numero, ip);

        solicitudes.put(id, solicitud);
        cola.put(numero, solicitud);
        enCola.incrementAndGet();
        recibidas.incrementAndGet();

        if (LOG_DETALLADO) {
            System.out.println("[Solicitud #" + id + "] Encolada");
        }

        return id;
    }

    public static void rechazarSolicitud(String id) {
        Solicitud solicitud = solicitudes.remove(id);
        if (solicitud != null && cola.remove(solicitud.orden, solicitud)) {
            enCola.decrementAndGet();
        }
        rechazadas.incrementAndGet();
    }

    public static void actualizarIp(String id, String ip) {
        if (id == null || ip == null || ip.isBlank()) {
            return;
        }

        Solicitud solicitud = solicitudes.get(id);
        if (solicitud != null) {
            solicitud.ip = ip;
        }
    }

    public static void actualizarRuta(String id, String ruta) {
        String rutaLimpia = Rutas.rutaEstadistica(ruta);
        Solicitud solicitud = solicitudes.get(id);
        if (solicitud != null) {
            solicitud.ruta = rutaLimpia;
        }

        topRutas.computeIfAbsent(rutaLimpia, clave -> new LongAdder()).increment();
    }

    public static void iniciarSolicitud(String id) {
        Solicitud solicitud = solicitudes.get(id);
        if (solicitud != null && cola.remove(solicitud.orden, solicitud)) {
            enCola.decrementAndGet();
        }
        activas.incrementAndGet();
    }

    public static void finalizarSolicitud(
        String id,
        String ip,
        String ruta,
        String hilo
    ) {
        activas.updateAndGet(valor -> Math.max(0, valor - 1));
        procesadas.incrementAndGet();
        solicitudes.remove(id);
    }

    public static void registrarRespuesta(int codigo) {
        if (codigo < 100 || codigo > 599) {
            return;
        }

        respuestasPorCodigo.computeIfAbsent(codigo, clave -> new LongAdder()).increment();
    }

    public static void registrarSinRespuesta(MotivoSinRespuesta motivo) {
        if (motivo != null) {
            sinRespuesta.get(motivo).increment();
        }
    }

    public static void registrarEvento(TipoEvento tipo, String ip, String detalle) {
        if (tipo == null) {
            return;
        }

        eventosPorTipo.get(tipo).increment();

        String ipLimpia = limitarTexto(ip == null || ip.isBlank() ? "desconocida" : ip);
        String detalleLimpio = limitarTexto(detalle == null ? "" : detalle);
        long ahora = System.currentTimeMillis();

        synchronized (BLOQUEO_EVENTOS) {
            Iterator<Evento> iterador = eventosRecientes.descendingIterator();
            while (iterador.hasNext()) {
                Evento existente = iterador.next();
                if (ahora - existente.ultimoMillis > VENTANA_AGRUPACION_MILLIS) {
                    break;
                }
                if (
                    existente.tipo == tipo &&
                    existente.ip.equals(ipLimpia) &&
                    existente.detalle.equals(detalleLimpio)
                ) {
                    existente.ultimoMillis = ahora;
                    if (existente.repeticiones < Integer.MAX_VALUE) {
                        existente.repeticiones++;
                    }
                    return;
                }
            }

            if (eventosRecientes.size() >= MAX_EVENTOS_RECIENTES) {
                eventosRecientes.pollFirst();
            }
            eventosRecientes.addLast(new Evento(ahora, tipo, ipLimpia, detalleLimpio));
        }
    }

    public static int getRecibidas() {
        return recibidas.get();
    }

    public static int getProcesadas() {
        return procesadas.get();
    }

    public static int getRechazadas() {
        return rechazadas.get();
    }

    public static int getActivas() {
        return activas.get();
    }

    public static int getEnCola() {
        return Math.max(0, enCola.get());
    }

    public static Instantanea instantanea() {
        Map<Integer, Long> codigos = new TreeMap<>();
        long aceptadas = 0;
        for (Map.Entry<Integer, LongAdder> entrada : respuestasPorCodigo.entrySet()) {
            long valor = entrada.getValue().sum();
            codigos.put(entrada.getKey(), valor);
            if (entrada.getKey() >= 200 && entrada.getKey() < 300) {
                aceptadas += valor;
            }
        }

        Map<TipoEvento, Long> eventos = new EnumMap<>(TipoEvento.class);
        for (Map.Entry<TipoEvento, LongAdder> entrada : eventosPorTipo.entrySet()) {
            eventos.put(entrada.getKey(), entrada.getValue().sum());
        }

        Map<MotivoSinRespuesta, Long> sin = new EnumMap<>(MotivoSinRespuesta.class);
        for (Map.Entry<MotivoSinRespuesta, LongAdder> entrada : sinRespuesta.entrySet()) {
            sin.put(entrada.getKey(), entrada.getValue().sum());
        }

        long bloqueadas =
            eventos.get(TipoEvento.LIMITE_GLOBAL) +
            eventos.get(TipoEvento.LIMITE_PAGINAS) +
            eventos.get(TipoEvento.LIMITE_IMAGENES) +
            eventos.get(TipoEvento.CAPACIDAD_IPS);

        return new Instantanea(
            INICIO_MILLIS,
            System.currentTimeMillis(),
            recibidas.get(),
            procesadas.get(),
            aceptadas,
            rechazadas.get(),
            bloqueadas,
            getActivas(),
            getEnCola(),
            Collections.unmodifiableMap(codigos),
            Collections.unmodifiableMap(eventos),
            Collections.unmodifiableMap(sin)
        );
    }

    public static List<EventoReciente> getEventosRecientes(int maximo) {
        List<EventoReciente> lista = new ArrayList<>();
        synchronized (BLOQUEO_EVENTOS) {
            Iterator<Evento> iterador = eventosRecientes.descendingIterator();
            while (iterador.hasNext() && lista.size() < maximo) {
                Evento evento = iterador.next();
                lista.add(new EventoReciente(
                    evento.primeroMillis,
                    evento.ultimoMillis,
                    evento.tipo,
                    evento.ip,
                    evento.detalle,
                    evento.repeticiones
                ));
            }
        }
        return lista;
    }

    public static List<RutaVisitada> getTopRutas(int maximo) {
        List<RutaVisitada> lista = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entrada : topRutas.entrySet()) {
            lista.add(new RutaVisitada(entrada.getKey(), entrada.getValue().sum()));
        }

        lista.sort(Comparator.comparingLong(RutaVisitada::total).reversed());

        if (lista.size() > maximo) {
            return new ArrayList<>(lista.subList(0, maximo));
        }
        return lista;
    }

    public static String getTopRutasHtml() {
        List<RutaVisitada> lista = getTopRutas(10);

        StringBuilder html = new StringBuilder();

        for (RutaVisitada ruta : lista) {
            html.append("""
                <div class="ruta-estadistica">
                    <span>%s</span>
                    <strong>%d</strong>
                </div>
                """.formatted(
                    escaparHtml(ruta.ruta()),
                    ruta.total()
                ));
        }

        if (html.isEmpty()) {
            return "<p>No hay estadísticas disponibles.</p>";
        }
        return html.toString();
    }

    public static String getColaHtml() {
        int totalCola = getEnCola();
        if (totalCola == 0) {
            return "<p class='vacio'>No hay solicitudes en cola.</p>";
        }

        StringBuilder html = new StringBuilder();
        int cantidad = 0;

        for (Solicitud solicitud : cola.values()) {
            html.append(
                "<div class='cola-item'>" +
                "<b>#" + escaparHtml(solicitud.id) + "</b> " +
                "GET " + escaparHtml(solicitud.ruta) +
                "<span>" + escaparHtml(solicitud.ip) + "</span>" +
                "</div>"
            );

            cantidad++;
            if (cantidad == 10) {
                break;
            }
        }

        totalCola = getEnCola();
        if (totalCola > cantidad) {
            html.append(
                "<p class='vacio'>+ " +
                (totalCola - cantidad) +
                " solicitudes más...</p>"
            );
        }

        return html.toString();
    }

    private static <E extends Enum<E>> EnumMap<E, LongAdder> crearContadores(Class<E> tipo) {
        EnumMap<E, LongAdder> mapa = new EnumMap<>(tipo);
        for (E valor : tipo.getEnumConstants()) {
            mapa.put(valor, new LongAdder());
        }
        return mapa;
    }

    private static String limitarTexto(String texto) {
        StringBuilder limpio = new StringBuilder(Math.min(texto.length(), MAX_LONGITUD_TEXTO));
        for (int i = 0; i < texto.length() && limpio.length() < MAX_LONGITUD_TEXTO; i++) {
            char c = texto.charAt(i);
            limpio.append(c < 0x20 || c == 0x7f ? '?' : c);
        }
        return limpio.toString();
    }

    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static class Solicitud {
        final String id;
        final long orden;
        volatile String ip;
        volatile String ruta = "pendiente";

        Solicitud(String id, long orden, String ip) {
            this.id = id;
            this.orden = orden;
            this.ip = ip;
        }
    }

    private static final class Evento {
        final long primeroMillis;
        final TipoEvento tipo;
        final String ip;
        final String detalle;
        long ultimoMillis;
        int repeticiones = 1;

        Evento(long ahora, TipoEvento tipo, String ip, String detalle) {
            this.primeroMillis = ahora;
            this.ultimoMillis = ahora;
            this.tipo = tipo;
            this.ip = ip;
            this.detalle = detalle;
        }
    }
}