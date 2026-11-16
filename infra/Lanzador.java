import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Lanzador del sistema en una, dos o tres máquinas, con Windows, Mac o Linux.
 * Se usa desde la raíz del repo: iniciar.cmd (Windows) o ./iniciar.sh (Mac y Linux). Guía: infra/DISTRIBUIDO.md.
 *
 * Hace solo lo que antes era a mano: detecta la IP de esta máquina, busca en la red la máquina que tiene el núcleo
 * (Eureka), escribe las IP en el .env y levanta con docker-compose.distribuido.yml solo lo que le toca a esta máquina.
 * Además se queda abierto como agente de la máquina: se registra en Eureka (aparece como AGENTE) y sirve el panel
 * oculto en http://localhost:8099, desde donde se enciende, apaga o mueve cualquier servicio en cualquier máquina.
 * Las órdenes entre máquinas van firmadas con el código del equipo, que genera el núcleo y la otra máquina pide una vez.
 *
 * No es uno de los 9 microservicios: es una herramienta del equipo para la demo. Java 17 o superior, sin dependencias.
 * Prueba rápida de la lógica, sin Docker: java infra/Lanzador.java --prueba
 */
public class Lanzador {

    static final String COMPOSE = "infra/docker-compose.distribuido.yml";
    static final int PUERTO_AGENTE = 8099;

    // Los 3 bloques son las 3 máquinas de la sección 5.6 del informe. ms-usuarios va con el núcleo (máquina 1).
    static final Map<String, List<String>> BLOQUES = new LinkedHashMap<>();
    static final Map<String, String> NOMBRE_BLOQUE = Map.of("nucleo", "Núcleo", "datos", "Datos", "modelo", "Modelo y web");
    static {
        BLOQUES.put("nucleo", List.of("postgres", "discovery-server", "api-gateway", "ms-usuarios"));
        BLOQUES.put("datos", List.of("ms-catalogo", "ms-ingesta", "ms-estadisticas"));
        BLOQUES.put("modelo", List.of("ms-cuotas", "ms-predicciones", "ms-apuestas", "web-app"));
    }
    static final List<String> TODOS = BLOQUES.values().stream().flatMap(List::stream).toList();
    static final Map<String, Integer> PUERTOS = Map.ofEntries(Map.entry("postgres", 5441), Map.entry("discovery-server", 8761),
            Map.entry("api-gateway", 8080), Map.entry("ms-usuarios", 8081), Map.entry("ms-catalogo", 8082), Map.entry("ms-ingesta", 8083),
            Map.entry("ms-estadisticas", 8084), Map.entry("ms-cuotas", 8085), Map.entry("ms-predicciones", 8086),
            Map.entry("ms-apuestas", 8087), Map.entry("web-app", 8090));
    // No se registran en Eureka: su estado sale del contenedor.
    static final Set<String> SIN_EUREKA = Set.of("postgres", "discovery-server", "web-app");
    // Todas las máquinas los buscan por la IP del núcleo, así que solo viven allí.
    static final Set<String> INFRA = Set.of("postgres", "discovery-server", "api-gateway");
    // Tienen tareas programadas (ingesta y liquidación): en dos máquinas correrían dos veces. Se mueven, no se replican.
    static final Set<String> UNA_SOLA = Set.of("ms-ingesta", "ms-apuestas");

    static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final String NOMBRE = System.getProperty("user.name", "equipo");
    static final String SO = System.getProperty("os.name", "?");
    static final String EQUIPO = nombreEquipo();
    // Token del panel: un formulario de otra página abierta en el navegador no lo conoce (evita CSRF hacia localhost).
    static final String TOKEN_PANEL = UUID.randomUUID().toString();
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
    static final ExecutorService TRABAJOS = Executors.newSingleThreadExecutor();
    static final ExecutorService RED = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    static final Map<String, String> ENTORNO = new LinkedHashMap<>();

    static String miIp;            // IP de esta máquina en la red, o null sin red
    static String miIpC;           // la que se anuncia en Eureka: miIp, o host.docker.internal sin red
    static String nucleo;          // IP del núcleo vista desde los contenedores
    static String eurekaHost;      // dónde le pregunta este proceso a Eureka (127.0.0.1 si el núcleo está aquí)
    static boolean nucleoAqui;
    static String codigo = "";     // código del equipo: firma las órdenes entre agentes
    static volatile String trabajo = "";
    static volatile boolean registrado;
    static Map<String, String> maquinasVistas = new HashMap<>();
    static Map<String, String> cacheContenedores = Map.of();
    static long cacheContenedoresEn;

    public static void main(String[] argumentos) throws Exception {
        // Un cliente que abre la conexión y no manda nada no puede ocupar un hilo del agente para siempre.
        System.setProperty("sun.net.httpserver.maxReqTime", "15");
        System.setProperty("sun.net.httpserver.maxRspTime", "60");
        List<String> args = new ArrayList<>(List.of(argumentos));
        if (args.remove("--prueba")) {
            pruebas();
            return;
        }
        String ipForzada = opcion(args, "--ip");
        String nucleoForzado = opcion(args, "--nucleo");
        String codigoDado = opcion(args, "--codigo");
        boolean apagar = args.remove("apagar");
        if (!Files.exists(Path.of(COMPOSE))) {
            salir("Ejecuta el lanzador desde la carpeta del repo: iniciar.cmd (Windows) o ./iniciar.sh (Mac y Linux).");
        }
        System.out.println("\n  App Apuestas · lanzador de máquinas\n");
        esperarDocker();
        Map<String, String> env = prepararEnv();
        miIp = ipForzada != null ? ipForzada : detectarIp();
        miIpC = miIp != null ? miIp : "host.docker.internal";
        System.out.println("Esta máquina: " + NOMBRE + " · " + EQUIPO + " · " + SO + " · "
                + (miIp != null ? "IP " + miIp : "sin red (todo quedará en esta máquina)"));
        // docker compose exige las IP aunque solo vaya a listar o detener contenedores.
        for (String k : List.of("IP_ESTA_MAQUINA", "IP_EUREKA", "IP_POSTGRES", "IP_GATEWAY", "IP_WEB")) {
            ENTORNO.put(k, env.getOrDefault(k, "").isBlank() ? miIpC : env.get(k));
        }
        if (apagar) {
            Set<String> corriendo = corriendoAqui();
            if (corriendo.isEmpty()) {
                System.out.println("No hay nada encendido en esta máquina.");
            } else {
                compose(lista("stop", corriendo));
            }
            return;
        }
        // Se reserva el puerto del agente antes de tocar nada: si ya hay otro lanzador abierto, no se cambia nada.
        HttpServer servidor;
        try {
            servidor = HttpServer.create(new InetSocketAddress(PUERTO_AGENTE), 0);
        } catch (BindException e) {
            salir("Ya hay un lanzador abierto en esta máquina (su panel: http://localhost:" + PUERTO_AGENTE
                    + "). Ciérralo con Ctrl+C y vuelve a ejecutar iniciar.");
            return;
        }

        Set<String> corriendo = corriendoAqui();
        String hallado = nucleoForzado != null ? nucleoForzado : buscarNucleo(env.get("IP_EUREKA"), args.isEmpty());
        boolean nucleoEnOtra = hallado != null && !hallado.equals("127.0.0.1") && !hallado.equals(miIp);
        List<Instancia> instancias = List.of();
        if (nucleoEnOtra) {
            eurekaHost = hallado;
            try {
                instancias = instanciasEureka();
            } catch (Exception e) {
                // Sin la lista no se sabe qué falta: el menú ofrece todo.
            }
        }
        List<String> bloques = args.isEmpty() ? menu(nucleoEnOtra, corriendo, instancias) : bloquesDe(args);
        List<String> servicios = serviciosDe(bloques);
        boolean levantaNucleo = servicios.contains("discovery-server");
        if (levantaNucleo && nucleoEnOtra) {
            salir("Ya hay un núcleo en " + hallado + ". Aquí elige Datos o Modelo y web, o apaga primero el núcleo de esa máquina.");
        }
        if (!levantaNucleo && !nucleoEnOtra) {
            salir("No hay ningún núcleo en la red. Levántalo primero en una máquina (Todo o Núcleo) y vuelve a ejecutar iniciar aquí.\n"
                    + "Si ya está encendido, revisa que las dos máquinas estén en la misma red (un hotspot del celular sirve).");
        }
        nucleoAqui = levantaNucleo;
        nucleo = nucleoAqui ? miIpC : hallado;
        eurekaHost = nucleoAqui ? "127.0.0.1" : hallado;
        for (String k : List.of("IP_EUREKA", "IP_POSTGRES", "IP_GATEWAY", "IP_WEB")) {
            ENTORNO.put(k, nucleo);
        }
        ENTORNO.put("IP_ESTA_MAQUINA", miIpC);

        codigo = normalizarCodigo(codigoDado != null ? codigoDado : env.getOrDefault("CODIGO_EQUIPO", ""));
        if (nucleoAqui && codigo.isEmpty()) {
            codigo = nuevoCodigo();
        } else if (!nucleoAqui) {
            acordarCodigo(instancias, args.isEmpty());
        }
        Map<String, String> paraEnv = new LinkedHashMap<>(ENTORNO);
        paraEnv.put("CODIGO_EQUIPO", codigo);
        Files.write(Path.of(".env"), conValores(Files.readAllLines(Path.of(".env"), StandardCharsets.UTF_8), paraEnv), StandardCharsets.UTF_8);

        if (miIp != null) {
            firewall(args.isEmpty());
        }
        agente(servidor);
        List<String> sobran = corriendo.stream().filter(s -> !servicios.contains(s)).toList();
        if (!sobran.isEmpty()) {
            System.out.println("Apago lo que ya no va en esta máquina: " + String.join(", ", sobran));
            compose(lista("stop", sobran));
        }
        if (!levantar(servicios)) {
            salir("docker compose falló: revisa los mensajes de arriba.");
        }
        // Si lo elegido también corre en otra máquina, se mueve: se apaga allá cuando ya está encendido aquí.
        Vista v = vista();
        boolean enOtra = v.maquinas().stream().anyMatch(m -> !m.ip().equals(miIpC) && servicios.stream().anyMatch(s -> v.activo(m, s)));
        if (enOtra) {
            System.out.println("Lo que elegiste también corre en otra máquina: lo apago allá cuando esté encendido aquí (hasta 3 minutos)...");
            if (esperarEncendidos(miIpC, servicios, 180)) {
                quitarDeOtras(miIpC, servicios).forEach(h -> System.out.println("  " + h));
            } else {
                System.out.println("  No terminó de arrancar aquí a tiempo: lo dejo también en la otra máquina. Usa «Mover aquí» en el panel.");
            }
        }
        resumen(servicios);
    }

    // ---------------------------------------------------------------- Red

    // La IP de la tarjeta con la ruta por defecto (wifi o cable). connect() en UDP no envía nada: solo elige la ruta.
    static String detectarIp() {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress a = s.getLocalAddress();
            if (a instanceof Inet4Address && !a.isAnyLocalAddress() && !a.isLoopbackAddress()) {
                return a.getHostAddress();
            }
        } catch (IOException e) {
            // Sin ruta por defecto: se busca a mano abajo.
        }
        Pattern virtuales = Pattern.compile("vethernet|docker|wsl|hyper-v|virtualbox|vmware|vmnet|veth|br-|bridge|utun|awdl|llw|tailscale|zerotier|hamachi|bluetooth|loopback");
        try {
            for (NetworkInterface n : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nombre = (n.getName() + " " + n.getDisplayName()).toLowerCase(Locale.ROOT);
                if (!n.isUp() || n.isLoopback() || n.isVirtual() || virtuales.matcher(nombre).find()) {
                    continue;
                }
                for (InetAddress a : Collections.list(n.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // Sin tarjetas de red: se trabaja sin red.
        }
        return null;
    }

    // Devuelve 127.0.0.1 si el núcleo está en esta máquina, su IP si está en otra, o null si no hay.
    // En un aula puede haber otros Eureka (otros grupos): se prefiere el que tiene nuestro lanzador (AGENTE) en su misma IP.
    static String buscarNucleo(String guardada, boolean preguntar) throws Exception {
        System.out.print("Buscando el núcleo (Eureka) en la red... ");
        if (eurekaEn("127.0.0.1") != null) {
            System.out.println("está en esta máquina.");
            return "127.0.0.1";
        }
        if (guardada != null && IPV4.matcher(guardada).matches() && !guardada.equals(miIp) && eurekaEn(guardada) != null) {
            System.out.println("está en " + guardada + ".");
            return guardada;
        }
        if (miIp == null) {
            System.out.println("sin red.");
            return null;
        }
        // Se prueban las 254 direcciones de la red a la vez: tarda 1 o 2 segundos.
        String base = miIp.substring(0, miIp.lastIndexOf('.') + 1);
        ExecutorService pool = Executors.newFixedThreadPool(64);
        Map<String, List<Instancia>> eurekas = new LinkedHashMap<>();
        try {
            Map<String, Future<List<Instancia>>> pruebas = new LinkedHashMap<>();
            for (int h = 1; h < 255; h++) {
                String ip = base + h;
                if (!ip.equals(miIp)) {
                    pruebas.put(ip, pool.submit(() -> abierto(ip, 8761) ? eurekaEn(ip) : null));
                }
            }
            for (Map.Entry<String, Future<List<Instancia>>> p : pruebas.entrySet()) {
                List<Instancia> ins = p.getValue().get();
                if (ins != null) {
                    eurekas.put(p.getKey(), ins);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        List<String> nuestros = eurekas.keySet().stream()
                .filter(ip -> eurekas.get(ip).stream().anyMatch(i -> i.app().equals("agente") && i.ip().equals(ip))).toList();
        List<String> candidatos = nuestros.isEmpty() ? List.copyOf(eurekas.keySet()) : nuestros;
        if (candidatos.isEmpty()) {
            System.out.println("no hay ninguno.");
            return null;
        }
        if (candidatos.size() == 1 || !preguntar) {
            System.out.println("está en " + candidatos.get(0) + ".");
            return candidatos.get(0);
        }
        System.out.println("hay varios (puede ser el de otro grupo):");
        for (int i = 0; i < candidatos.size(); i++) {
            String ip = candidatos.get(i);
            String quien = eurekas.get(ip).stream().filter(x -> x.app().equals("agente") && x.ip().equals(ip))
                    .map(x -> String.valueOf(x.meta().getOrDefault("nombre", "?"))).findFirst().orElse("sin lanzador");
            System.out.println("  " + (i + 1) + ") " + ip + " (" + quien + ")");
        }
        while (true) {
            System.out.print("¿Cuál es el de tu equipo? ");
            String linea = leerLinea();
            try {
                int n = Integer.parseInt(linea == null ? "1" : linea.trim());
                if (n >= 1 && n <= candidatos.size()) {
                    return candidatos.get(n - 1);
                }
            } catch (NumberFormatException e) {
                // Se vuelve a preguntar.
            }
        }
    }

    static boolean abierto(String ip, int puerto) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, puerto), 400);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Las instancias del Eureka de ese host, o null si ahí no hay un Eureka.
    static List<Instancia> eurekaEn(String host) {
        try {
            HttpResponse<String> r = pedir(HttpRequest.newBuilder(URI.create("http://" + host + ":8761/eureka/apps"))
                    .header("Accept", "application/json").build(), 1500);
            return r.statusCode() == 200 ? leerInstancias(r.body()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Una petición con tiempo máximo de verdad: el timeout del HttpClient no cubre un servidor que manda el cuerpo de a gotas.
    static HttpResponse<String> pedir(HttpRequest peticion, long ms) throws Exception {
        CompletableFuture<HttpResponse<String>> f = HTTP.sendAsync(peticion, HttpResponse.BodyHandlers.ofString());
        try {
            return f.get(ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw new IOException("sin respuesta en " + ms + " ms");
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception causa ? causa : new IOException(e.getCause());
        }
    }

    // En Windows, una red wifi nueva suele quedar como "pública" y bloquea que otras máquinas entren a los puertos.
    static void firewall(boolean preguntar) throws Exception {
        if (!SO.toLowerCase(Locale.ROOT).contains("win") || correr(List.of("netsh", "advfirewall", "firewall", "show", "rule", "name=AppApuestas"), false) == 0) {
            return;
        }
        String regla = "advfirewall firewall add rule name=AppApuestas dir=in action=allow protocol=TCP localport=5441,8761,8080-8099 remoteip=localsubnet profile=any";
        System.out.println("\nWindows no deja que otras máquinas entren a los puertos del sistema (5441, 8761 y 8080-8099).");
        if (!preguntar || !siNo("¿Los abro solo para tu red local? Windows pedirá permiso de administrador. [S/n] ")) {
            System.out.println("Si la otra máquina no te ve, ejecuta como administrador:  netsh " + regla
                    + "\n(para quitarlo después: netsh advfirewall firewall delete rule name=AppApuestas)");
            return;
        }
        correr(List.of("powershell", "-NoProfile", "-Command", "Start-Process netsh -Verb RunAs -Wait -ArgumentList '" + regla + "'"), true);
    }

    // ---------------------------------------------------------------- Código del equipo

    static String nuevoCodigo() {
        SecureRandom azar = new SecureRandom();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            b.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return normalizarCodigo(b.toString());
    }

    // "k7qf m2xd", "K7QFM2XD" y "K7QF-M2XD" son el mismo código.
    static String normalizarCodigo(String texto) {
        String c = texto == null ? "" : texto.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return c.length() == 8 ? c.substring(0, 4) + "-" + c.substring(4) : c;
    }

    // La máquina que se une comprueba su código con el agente del núcleo; si no tiene o no coincide, lo pide.
    static void acordarCodigo(List<Instancia> instancias, boolean preguntar) throws Exception {
        Instancia agente = instancias.stream().filter(i -> i.app().equals("agente") && i.ip().equals(nucleo)).findFirst().orElse(null);
        if (agente == null) {
            System.out.println("El núcleo no tiene el lanzador abierto: desde el panel no se podrá controlar esa máquina.");
            return;
        }
        for (int intento = 0; intento < 4; intento++) {
            if (!codigo.isEmpty()) {
                Respuesta r = enviar(nucleo, agente.puerto(), "ping", List.of());
                if (r.codigo() == 204) {
                    System.out.println("Código del equipo correcto.");
                    return;
                }
                if (r.codigo() != 403) {
                    System.out.println("No pude comprobar el código con el núcleo (" + r.cuerpo() + "). Sigo con el que tengo.");
                    return;
                }
                System.out.println("Ese no es el código del núcleo.");
            }
            if (!preguntar) {
                return;
            }
            System.out.print("Código del equipo (lo muestra el lanzador del núcleo, en su consola y en su panel): ");
            String linea = leerLinea();
            if (linea == null || linea.isBlank()) {
                System.out.println("Sin código: esta máquina funciona, pero los paneles no podrán darle órdenes.");
                return;
            }
            codigo = normalizarCodigo(linea);
        }
    }

    static String firmar(String mensaje) throws Exception {
        if (codigo.isEmpty()) {
            throw new IllegalStateException("esta máquina no tiene el código del equipo");
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(codigo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
    }

    // Lo que se firma: la orden, los servicios, la hora y la IP de destino (una orden capturada no sirve para otra máquina).
    static String mensajeFirmado(String accion, List<String> servicios, long tiempo, String destino) {
        return accion + "|" + String.join(",", servicios) + "|" + tiempo + "|" + destino;
    }

    // ---------------------------------------------------------------- Docker

    static void esperarDocker() throws Exception {
        if (correr(List.of("docker", "info"), false) == 0) {
            return;
        }
        String so = SO.toLowerCase(Locale.ROOT);
        System.out.print("Docker no está encendido. Lo abro y espero (hasta 3 minutos)");
        if (!so.contains("win") && !so.contains("mac")) {
            System.out.print("\n(En Linux, si Docker ya está encendido, a tu usuario le falta permiso: sudo usermod -aG docker $USER y vuelve a entrar)");
        }
        try {
            if (so.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", "C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe").start();
            } else if (so.contains("mac")) {
                new ProcessBuilder("open", "-a", "Docker").start();
            }
        } catch (IOException e) {
            // Si no se pudo abrir solo, se sigue esperando a que el usuario lo abra.
        }
        for (int i = 0; i < 60; i++) {
            TimeUnit.SECONDS.sleep(3);
            System.out.print(".");
            if (correr(List.of("docker", "info"), false) == 0) {
                System.out.println(" listo.");
                return;
            }
        }
        salir("\nDocker no respondió. Abre Docker Desktop a mano y vuelve a ejecutar iniciar.");
    }

    static Map<String, String> prepararEnv() throws IOException {
        Path env = Path.of(".env");
        if (!Files.exists(env)) {
            Files.copy(Path.of(".env.example"), env);
            System.out.println("Creé el .env a partir de .env.example.");
        }
        Map<String, String> valores = new HashMap<>();
        for (String linea : Files.readAllLines(env, StandardCharsets.UTF_8)) {
            linea = linea.replace("\uFEFF", "").trim();
            int igual = linea.indexOf('=');
            if (!linea.startsWith("#") && igual > 0) {
                valores.put(linea.substring(0, igual).trim(), linea.substring(igual + 1).trim());
            }
        }
        return valores;
    }

    // Las líneas del .env con los valores nuevos: quita las anteriores de esas claves y agrega las nuevas al final.
    static List<String> conValores(List<String> lineas, Map<String, String> valores) {
        List<String> r = new ArrayList<>(lineas.stream()
                .map(l -> l.replace("\uFEFF", ""))
                .filter(l -> valores.keySet().stream().noneMatch(k -> l.startsWith(k + "=")))
                .toList());
        valores.forEach((k, v) -> r.add(k + "=" + v));
        return r;
    }

    static int compose(List<String> args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("docker", "compose", "-f", COMPOSE, "--env-file", ".env"));
        cmd.addAll(args);
        return correr(cmd, true);
    }

    // Servicio → estado del contenedor en esta máquina (running, exited, restarting...). Se guarda 2 s.
    static synchronized Map<String, String> contenedores() {
        if (System.currentTimeMillis() - cacheContenedoresEn < 2000) {
            return cacheContenedores;
        }
        Map<String, String> r = new LinkedHashMap<>();
        File salida = null;
        try {
            // A un archivo: si la salida llenara la tubería, esperar al proceso sin leerla lo dejaría colgado.
            salida = File.createTempFile("lanzador-ps", ".json");
            ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", COMPOSE, "--env-file", ".env", "ps", "-a", "--format", "json")
                    .redirectOutput(salida).redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.environment().putAll(ENTORNO);
            Process p = pb.start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return cacheContenedores;
            }
            r.putAll(leerPs(Files.readString(salida.toPath(), StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            // Docker no responde o la salida no se entiende: se ve como si no hubiera contenedores.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (salida != null) {
                salida.delete();
            }
        }
        cacheContenedores = r;
        cacheContenedoresEn = System.currentTimeMillis();
        return r;
    }

    // "docker compose ps --format json": una lista o un objeto por línea, según la versión de Compose.
    static Map<String, String> leerPs(String salida) {
        Map<String, String> r = new LinkedHashMap<>();
        String texto = salida.trim();
        List<Object> filas = new ArrayList<>();
        if (texto.startsWith("[")) {
            filas.addAll(comoLista(json(texto)));
        } else {
            for (String linea : texto.split("\\R")) {
                if (linea.trim().startsWith("{")) {
                    filas.add(json(linea));
                }
            }
        }
        for (Object f : filas) {
            if (f instanceof Map<?, ?> m && TODOS.contains(String.valueOf(m.get("Service")))) {
                r.put(String.valueOf(m.get("Service")), String.valueOf(m.get("State")));
            }
        }
        return r;
    }

    static Set<String> corriendoAqui() {
        Set<String> r = new LinkedHashSet<>();
        contenedores().forEach((s, e) -> {
            if (e.equals("running") || e.equals("restarting")) {
                r.add(s);
            }
        });
        return r;
    }

    // Primero la base y Eureka, y se espera a que Eureka responda; después el resto, así nadie arranca sin dónde registrarse.
    static boolean levantar(List<String> servicios) throws Exception {
        List<String> base = ordenados(servicios).stream().filter(s -> s.equals("postgres") || s.equals("discovery-server")).toList();
        List<String> resto = ordenados(servicios).stream().filter(s -> !base.contains(s)).toList();
        if (!base.isEmpty() && compose(lista("up", "-d", "--build", base)) != 0) {
            return false;
        }
        if (base.contains("discovery-server")) {
            System.out.print("Esperando a que Eureka responda");
            for (int i = 0; i < 60 && eurekaEn("127.0.0.1") == null; i++) {
                System.out.print(".");
                TimeUnit.SECONDS.sleep(3);
            }
            System.out.println();
        }
        boolean ok = resto.isEmpty() || compose(lista("up", "-d", "--build", resto)) == 0;
        cacheContenedoresEn = 0;
        return ok;
    }

    static int correr(List<String> cmd, boolean mostrar) throws InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().putAll(ENTORNO);
            if (mostrar) {
                pb.inheritIO();
            } else {
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
            }
            return pb.start().waitFor();
        } catch (IOException e) {
            if (cmd.get(0).equals("docker")) {
                salir("No encuentro Docker. Instala Docker Desktop (Windows o Mac) o Docker Engine (Linux).");
            }
            return -1;
        }
    }

    // ---------------------------------------------------------------- Menú

    record Opcion(String texto, List<String> bloques) {
    }

    static List<String> menu(boolean nucleoEnOtra, Set<String> corriendo, List<Instancia> instancias) throws IOException {
        List<Opcion> ops = new ArrayList<>();
        List<String> aqui = BLOQUES.keySet().stream().filter(b -> BLOQUES.get(b).stream().anyMatch(corriendo::contains)).toList();
        if (!aqui.isEmpty() && !(nucleoEnOtra && aqui.contains("nucleo"))) {
            ops.add(new Opcion("Lo mismo que ya corre aquí (" + nombres(aqui) + ")", aqui));
        }
        List<String> faltan = List.of();
        if (!nucleoEnOtra) {
            ops.add(new Opcion("Todo en esta máquina", List.of("nucleo", "datos", "modelo")));
            ops.add(new Opcion("Núcleo + Datos          (la otra máquina pone Modelo y web)", List.of("nucleo", "datos")));
            ops.add(new Opcion("Núcleo + Modelo y web   (la otra máquina pone Datos)", List.of("nucleo", "modelo")));
            ops.add(new Opcion("Solo el núcleo          (3 máquinas, como el informe)", List.of("nucleo")));
        } else {
            faltan = Stream.of("datos", "modelo")
                    .filter(b -> BLOQUES.get(b).stream().noneMatch(s -> instancias.stream().anyMatch(i -> i.app().equals(s))))
                    .toList();
            if (!faltan.isEmpty()) {
                ops.add(new Opcion("Lo que falta en la red (" + nombres(faltan) + ")", faltan));
            }
            ops.add(new Opcion("Datos", List.of("datos")));
            ops.add(new Opcion("Modelo y web", List.of("modelo")));
            ops.add(new Opcion("Datos + Modelo y web", List.of("datos", "modelo")));
            ops.add(new Opcion("Nada: solo el panel, para controlar desde esta máquina", List.of()));
        }
        // Sin opciones repetidas: si "lo que falta" es igual a otra, queda la primera.
        Map<List<String>, Opcion> unicas = new LinkedHashMap<>();
        ops.forEach(o -> unicas.putIfAbsent(o.bloques(), o));
        List<Opcion> lista = new ArrayList<>(unicas.values());

        System.out.println(nucleoEnOtra ? "\nEsta máquina se une al núcleo. ¿Qué levanto aquí?" : "\n¿Qué levanto en esta máquina?");
        if (nucleoEnOtra && faltan.isEmpty()) {
            System.out.println("(La otra máquina ya tiene todo: lo que elijas se mueve aquí y se apaga allá cuando esté listo.)");
        }
        for (int i = 0; i < lista.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + lista.get(i).texto() + (i == 0 ? "   ← Enter" : ""));
        }
        while (true) {
            System.out.print("Opción: ");
            String linea = leerLinea();
            if (linea == null || linea.isBlank()) {
                return lista.get(0).bloques();
            }
            try {
                int n = Integer.parseInt(linea.trim());
                if (n >= 1 && n <= lista.size()) {
                    return lista.get(n - 1).bloques();
                }
            } catch (NumberFormatException e) {
                // Se vuelve a preguntar.
            }
            System.out.println("Escribe un número del 1 al " + lista.size() + ".");
        }
    }

    // Argumentos sin menú: "todo", "nada" o bloques separados por coma: iniciar nucleo,datos
    static List<String> bloquesDe(List<String> args) {
        String texto = String.join(",", args).toLowerCase(Locale.ROOT);
        if (texto.equals("todo")) {
            return List.copyOf(BLOQUES.keySet());
        }
        if (texto.equals("nada")) {
            return List.of();
        }
        List<String> r = new ArrayList<>();
        for (String b : texto.split("[,+ ]+")) {
            if (!BLOQUES.containsKey(b)) {
                salir("No conozco «" + b + "». Usa: todo, nada, nucleo, datos, modelo (o varios: nucleo,datos), apagar.");
            }
            r.add(b);
        }
        return r;
    }

    static List<String> serviciosDe(List<String> bloques) {
        return ordenados(bloques.stream().flatMap(b -> BLOQUES.get(b).stream()).toList());
    }

    static List<String> ordenados(Collection<String> servicios) {
        return TODOS.stream().filter(servicios::contains).toList();
    }

    static String nombres(List<String> bloques) {
        return bloques.stream().map(NOMBRE_BLOQUE::get).collect(Collectors.joining(" + "));
    }

    // ---------------------------------------------------------------- Agente

    // Se enciende antes de levantar los servicios: el panel ya sirve mientras arrancan.
    static void agente(HttpServer servidor) {
        servidor.createContext("/", x -> atender(x, Lanzador::panel));
        servidor.createContext("/accion", x -> atender(x, Lanzador::accion));
        servidor.createContext("/estado", x -> atender(x, Lanzador::estado));
        servidor.createContext("/ejecutar", x -> atender(x, Lanzador::ejecutar));
        servidor.setExecutor(Executors.newFixedThreadPool(16));
        servidor.start();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(Lanzador::latido, 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(Lanzador::darDeBaja));
        System.out.println("Panel de máquinas en http://localhost:" + PUERTO_AGENTE + " (ya se puede abrir mientras arranca todo).");
    }

    static void resumen(List<String> servicios) {
        String hostWeb = miIp != null ? miIp : "localhost";
        System.out.println("\n──────────────────────────────────────────────────────────────");
        System.out.println(" Listo. Deja esta ventana abierta: es el agente de esta máquina.");
        System.out.println(" Los servicios tardan 1 o 2 minutos en aparecer en Eureka.");
        if (servicios.contains("web-app")) {
            System.out.println(" Web:     http://localhost:8090   (desde otra máquina: http://" + hostWeb + ":8090)");
        }
        System.out.println(" Panel:   http://localhost:" + PUERTO_AGENTE + "   (o la esquina inferior derecha de la web)");
        System.out.println(" Eureka:  http://" + (nucleoAqui ? "localhost" : nucleo) + ":8761");
        if (!codigo.isEmpty()) {
            System.out.println(" Código del equipo: " + codigo + "   (la otra máquina lo pide una sola vez)");
        }
        System.out.println(" Apagar lo de esta máquina: iniciar apagar   ·   Cerrar el agente: Ctrl+C");
        System.out.println("──────────────────────────────────────────────────────────────\n");
    }

    interface Manejador {
        void manejar(HttpExchange x) throws Exception;
    }

    static void atender(HttpExchange x, Manejador m) {
        try {
            m.manejar(x);
        } catch (Exception e) {
            try {
                responder(x, 500, "text/plain; charset=utf-8", "Error del agente: " + e.getMessage());
            } catch (IOException ignorada) {
                // La conexión ya se cerró.
            }
        } finally {
            x.close();
        }
    }

    // Cada 5 s: latido a Eureka (o registro, si Eureka no lo conoce) y aviso de las máquinas que se unen o se van.
    static void latido() {
        String id = "agente:" + miIpC;
        try {
            HttpResponse<String> r = pedir(HttpRequest.newBuilder(eureka("/apps/AGENTE/" + id))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build(), 3000);
            if (r.statusCode() == 404) {
                String json = """
                        {"instance":{"instanceId":"%s","hostName":"%s","app":"AGENTE","ipAddr":"%s","vipAddress":"agente","status":"UP",
                        "port":{"$":%d,"@enabled":"true"},"securePort":{"$":443,"@enabled":"false"},
                        "dataCenterInfo":{"@class":"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo","name":"MyOwn"},
                        "leaseInfo":{"renewalIntervalInSecs":5,"durationInSecs":15},
                        "homePageUrl":"http://%s:%d/estado","statusPageUrl":"http://%s:%d/estado","healthCheckUrl":"http://%s:%d/estado",
                        "metadata":{"nombre":%s,"equipo":%s,"so":%s}}}"""
                        .formatted(id, miIpC, miIpC, PUERTO_AGENTE, miIpC, PUERTO_AGENTE, miIpC, PUERTO_AGENTE, miIpC, PUERTO_AGENTE,
                                jsonTexto(NOMBRE), jsonTexto(EQUIPO), jsonTexto(SO));
                HttpResponse<String> alta = pedir(HttpRequest.newBuilder(eureka("/apps/AGENTE")).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(), 3000);
                if (alta.statusCode() >= 300) {
                    throw new IOException("Eureka no aceptó el registro del agente (" + alta.statusCode() + ")");
                }
            }
            if (!registrado) {
                System.out.println("[" + hora() + "] Agente registrado en Eureka (" + nucleo + ").");
                registrado = true;
            }
            Map<String, String> ahora = new HashMap<>();
            for (Instancia i : instanciasEureka()) {
                if (i.app().equals("agente") && !i.ip().equals(miIpC)) {
                    ahora.put(i.ip(), String.valueOf(i.meta().getOrDefault("nombre", i.ip())));
                }
            }
            ahora.forEach((ip, n) -> {
                if (!maquinasVistas.containsKey(ip)) {
                    System.out.println("[" + hora() + "] + Se unió " + n + " (" + ip + ").");
                }
            });
            maquinasVistas.forEach((ip, n) -> {
                if (!ahora.containsKey(ip)) {
                    System.out.println("[" + hora() + "] - Se fue " + n + " (" + ip + ").");
                }
            });
            maquinasVistas = ahora;
        } catch (Exception e) {
            if (registrado) {
                System.out.println("[" + hora() + "] Eureka no responde en " + nucleo + " (" + e.getMessage() + "). Sigo intentando.");
            }
            registrado = false;
        }
    }

    static void darDeBaja() {
        try {
            pedir(HttpRequest.newBuilder(eureka("/apps/AGENTE/agente:" + miIpC)).DELETE().build(), 2000);
        } catch (Exception e) {
            // Si Eureka no responde, el agente sale solo del registro a los 15 s.
        }
    }

    static URI eureka(String ruta) {
        return URI.create("http://" + eurekaHost + ":8761/eureka" + ruta);
    }

    // GET /estado (red local, solo lectura): lo usan los paneles de las otras máquinas.
    static void estado(HttpExchange x) throws IOException {
        String contenedores = contenedores().entrySet().stream()
                .map(e -> jsonTexto(e.getKey()) + ":" + jsonTexto(e.getValue())).collect(Collectors.joining(","));
        responder(x, 200, "application/json; charset=utf-8", "{\"nombre\":" + jsonTexto(NOMBRE) + ",\"equipo\":" + jsonTexto(EQUIPO)
                + ",\"so\":" + jsonTexto(SO) + ",\"trabajo\":" + jsonTexto(trabajo) + ",\"contenedores\":{" + contenedores + "}}");
    }

    // POST /ejecutar (red local): una orden de otro agente, firmada con el código del equipo. Solo enciende o apaga servicios conocidos.
    static void ejecutar(HttpExchange x) throws Exception {
        Map<String, String> f = formulario(x);
        String accion = f.getOrDefault("accion", "");
        String lista = f.getOrDefault("servicios", "");
        List<String> servicios = lista.isEmpty() ? List.of() : List.of(lista.split(","));
        long tiempo;
        try {
            tiempo = Long.parseLong(f.getOrDefault("tiempo", ""));
        } catch (NumberFormatException e) {
            tiempo = 0;
        }
        if (!x.getRequestMethod().equals("POST") || !Set.of("encender", "apagar", "ping").contains(accion) || !TODOS.containsAll(servicios)
                || (servicios.isEmpty() != accion.equals("ping"))) {
            responder(x, 400, "text/plain; charset=utf-8", "Orden no válida.");
            return;
        }
        if (codigo.isEmpty()) {
            responder(x, 403, "text/plain; charset=utf-8", "esta máquina no tiene el código del equipo.");
            return;
        }
        boolean vigente = Math.abs(System.currentTimeMillis() / 1000 - tiempo) < 120;
        byte[] esperada = firmar(mensajeFirmado(accion, servicios, tiempo, miIpC)).getBytes(StandardCharsets.UTF_8);
        if (!vigente || !MessageDigest.isEqual(esperada, f.getOrDefault("firma", "").getBytes(StandardCharsets.UTF_8))) {
            responder(x, 403, "text/plain; charset=utf-8", "el código del equipo no coincide (o la hora de las máquinas difiere en más de 2 minutos).");
            return;
        }
        if (accion.equals("ping")) {
            x.sendResponseHeaders(204, -1);
            return;
        }
        if (accion.equals("encender") && !nucleoAqui && servicios.stream().anyMatch(INFRA::contains)) {
            responder(x, 400, "text/plain; charset=utf-8", "postgres, Eureka y el gateway solo van en el núcleo.");
            return;
        }
        encolar(accion, servicios);
        responder(x, 202, "text/plain; charset=utf-8", "En curso.");
    }

    static void encolar(String accion, List<String> servicios) {
        String desc = (accion.equals("encender") ? "encender " : "apagar ") + String.join(", ", servicios);
        System.out.println("[" + hora() + "] Orden: " + desc);
        trabajo = "En curso: " + desc + "…";
        TRABAJOS.submit(() -> {
            boolean ok;
            try {
                ok = accion.equals("encender") ? levantar(servicios) : compose(lista("stop", servicios)) == 0;
            } catch (Exception e) {
                ok = false;
            }
            cacheContenedoresEn = 0;
            trabajo = (ok ? "Listo: " : "Falló: ") + desc + " (" + hora() + ")";
        });
    }

    record Respuesta(int codigo, String cuerpo) {
    }

    // Manda una orden firmada al agente de otra máquina. codigo -1 = no respondió.
    static Respuesta enviar(String ip, int puerto, String accion, List<String> servicios) {
        try {
            long t = System.currentTimeMillis() / 1000;
            String cuerpo = "accion=" + accion + "&servicios=" + String.join(",", servicios) + "&tiempo=" + t
                    + "&firma=" + firmar(mensajeFirmado(accion, servicios, t, ip));
            HttpResponse<String> r = pedir(HttpRequest.newBuilder(URI.create("http://" + ip + ":" + puerto + "/ejecutar"))
                    .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), 4000);
            return new Respuesta(r.statusCode(), r.body());
        } catch (IllegalStateException e) {
            return new Respuesta(-1, e.getMessage());
        } catch (Exception e) {
            return new Respuesta(-1, "no responde (¿se cerró su lanzador o su firewall bloquea el puerto " + puerto + "?)");
        }
    }

    record Resultado(boolean ok, String texto) {
    }

    static Resultado ordenar(Maquina m, String accion, List<String> servicios) {
        String desc = (accion.equals("encender") ? "Encendiendo " : "Apagando ") + String.join(", ", servicios) + " en " + m.nombre();
        if (m.ip().equals(miIpC)) {
            encolar(accion, servicios);
            return new Resultado(true, desc);
        }
        Respuesta r = enviar(m.ip(), m.puerto(), accion, servicios);
        return r.codigo() == 202 ? new Resultado(true, desc) : new Resultado(false, m.nombre() + " no aceptó la orden: " + r.cuerpo());
    }

    // Espera a que esos servicios estén encendidos en esa máquina (en Eureka, o el contenedor para los que no se registran).
    static boolean esperarEncendidos(String ip, List<String> servicios, int segundos) throws InterruptedException {
        long fin = System.currentTimeMillis() + segundos * 1000L;
        while (System.currentTimeMillis() < fin) {
            Vista v = vista();
            Maquina m = v.maquinas().stream().filter(x -> x.ip().equals(ip)).findFirst().orElse(null);
            if (m != null && servicios.stream().allMatch(s -> v.estado(m, s).equals("encendido"))) {
                return true;
            }
            TimeUnit.SECONDS.sleep(3);
        }
        return false;
    }

    // Apaga esos servicios en todas las máquinas menos la de destino. Las que no tienen lanzador solo se avisan.
    static List<String> quitarDeOtras(String destino, List<String> servicios) {
        Vista v = vista();
        List<String> hechos = new ArrayList<>();
        for (Maquina otra : v.maquinas()) {
            List<String> alla = servicios.stream().filter(s -> !otra.ip().equals(destino) && v.activo(otra, s)).toList();
            if (alla.isEmpty()) {
                continue;
            }
            hechos.add(otra.conAgente() && otra.responde() ? ordenar(otra, "apagar", alla).texto()
                    : String.join(", ", alla) + " sigue también en " + otra.nombre() + " (sin lanzador): apágalo allí.");
        }
        return hechos;
    }

    // ---------------------------------------------------------------- Vista del panel

    record Instancia(String app, String ip, int puerto, String estado, Map<String, Object> meta) {
    }

    record Maquina(String ip, String nombre, String equipo, String so, boolean conAgente, boolean responde, String trabajo, int puerto,
            Map<String, String> contenedores) {
    }

    record Vista(boolean eurekaResponde, List<Maquina> maquinas, List<Instancia> instancias) {

        // encendido, arrancando, fallando, apagado o "" (no está en esa máquina).
        // Si se conoce el contenedor, manda él: Eureka tarda unos segundos en enterarse de que algo se apagó.
        String estado(Maquina m, String servicio) {
            String c = m.contenedores().get(servicio);
            if (c != null && !c.equals("running") && !c.equals("restarting")) {
                return "apagado";
            }
            if ("restarting".equals(c)) {
                return "fallando";
            }
            boolean enEureka = instancias.stream().anyMatch(i -> i.app().equals(servicio) && i.ip().equals(m.ip()) && i.estado().equals("UP"));
            if (enEureka || ("running".equals(c) && SIN_EUREKA.contains(servicio))) {
                return "encendido";
            }
            return "running".equals(c) ? "arrancando" : "";
        }

        boolean activo(Maquina m, String servicio) {
            return Set.of("encendido", "arrancando", "fallando").contains(estado(m, servicio));
        }

        List<Maquina> donde(String servicio) {
            return maquinas.stream().filter(m -> activo(m, servicio)).toList();
        }
    }

    static Vista vista() {
        List<Instancia> instancias;
        boolean ok = true;
        try {
            instancias = instanciasEureka();
        } catch (Exception e) {
            instancias = List.of();
            ok = false;
        }
        Map<String, Maquina> maquinas = new LinkedHashMap<>();
        maquinas.put(miIpC, new Maquina(miIpC, NOMBRE, EQUIPO, SO, true, true, trabajo, PUERTO_AGENTE, contenedores()));
        Map<String, CompletableFuture<Maquina>> otras = new LinkedHashMap<>();
        for (Instancia a : instancias) {
            if (a.app().equals("agente") && !maquinas.containsKey(a.ip()) && !otras.containsKey(a.ip())) {
                otras.put(a.ip(), CompletableFuture.supplyAsync(() -> {
                    try {
                        HttpResponse<String> r = pedir(HttpRequest.newBuilder(URI.create("http://" + a.ip() + ":" + a.puerto() + "/estado")).build(), 2500);
                        return maquinaDe(a, (Map<?, ?>) json(r.body()));
                    } catch (Exception e) {
                        return new Maquina(a.ip(), texto(a.meta().get("nombre"), a.ip()), texto(a.meta().get("equipo"), ""),
                                texto(a.meta().get("so"), ""), true, false, "", a.puerto(), Map.of());
                    }
                }, RED));
            }
        }
        otras.forEach((ip, f) -> maquinas.put(ip, f.join()));
        // Máquinas con servicios en Eureka pero sin lanzador (levantadas a mano con docker compose).
        for (Instancia i : instancias) {
            maquinas.putIfAbsent(i.ip(), new Maquina(i.ip(), i.ip(), "", "", false, false, "", 0, Map.of()));
        }
        List<Maquina> orden = new ArrayList<>(maquinas.values());
        orden.sort((a, b) -> Boolean.compare(!a.ip().equals(nucleo), !b.ip().equals(nucleo)));
        return new Vista(ok, orden, instancias);
    }

    static Maquina maquinaDe(Instancia a, Map<?, ?> e) {
        Map<String, String> c = new LinkedHashMap<>();
        if (e.get("contenedores") instanceof Map<?, ?> m) {
            m.forEach((k, v) -> {
                if (TODOS.contains(String.valueOf(k))) {
                    c.put(String.valueOf(k), String.valueOf(v));
                }
            });
        }
        return new Maquina(a.ip(), texto(e.get("nombre"), a.ip()), texto(e.get("equipo"), ""), texto(e.get("so"), ""), true, true,
                texto(e.get("trabajo"), ""), a.puerto(), c);
    }

    static String texto(Object o, String siFalta) {
        return o == null ? siFalta : String.valueOf(o);
    }

    static List<Instancia> instanciasEureka() throws Exception {
        HttpResponse<String> r = pedir(HttpRequest.newBuilder(eureka("/apps")).header("Accept", "application/json").build(), 2500);
        if (r.statusCode() != 200) {
            throw new IOException("Eureka respondió " + r.statusCode());
        }
        return leerInstancias(r.body());
    }

    // Eureka está abierto en la red: solo se aceptan instancias con una IPv4 (o host.docker.internal) y un puerto válidos,
    // así un registro falso no puede hacer que el agente llame a otra dirección ni romper el panel.
    @SuppressWarnings("unchecked")
    static List<Instancia> leerInstancias(String cuerpo) {
        Map<?, ?> apps = (Map<?, ?>) ((Map<?, ?>) json(cuerpo)).get("applications");
        List<Instancia> r = new ArrayList<>();
        for (Object a : comoLista(apps.get("application"))) {
            for (Object o : comoLista(((Map<?, ?>) a).get("instance"))) {
                Map<?, ?> i = (Map<?, ?>) o;
                String ip = String.valueOf(i.get("ipAddr"));
                Object p = i.get("port");
                String puerto = p instanceof Map<?, ?> m ? String.valueOf(m.get("$")) : String.valueOf(p);
                if (!(IPV4.matcher(ip).matches() || ip.equals("host.docker.internal")) || !puerto.trim().matches("\\d{1,5}")) {
                    continue;
                }
                Object meta = i.get("metadata");
                r.add(new Instancia(String.valueOf(i.get("app")).toLowerCase(Locale.ROOT), ip, Integer.parseInt(puerto.trim()),
                        String.valueOf(i.get("status")), meta instanceof Map<?, ?> mm ? (Map<String, Object>) mm : Map.of()));
            }
        }
        return r;
    }

    // ---------------------------------------------------------------- Panel (solo desde esta máquina)

    // Solo desde esta máquina y con Host localhost: otra página no puede leer el panel con DNS rebinding ni un contenedor llegar a él.
    static boolean desdeAqui(HttpExchange x) {
        String host = x.getRequestHeaders().getFirst("Host");
        return x.getRemoteAddress().getAddress().isLoopbackAddress() && host != null
                && host.matches("(localhost|127\\.0\\.0\\.1|\\[::1\\])(:\\d+)?");
    }

    static void panel(HttpExchange x) throws IOException {
        if (!x.getRequestURI().getPath().equals("/")) {
            responder(x, 404, "text/plain; charset=utf-8", "No existe.");
            return;
        }
        if (!desdeAqui(x)) {
            responder(x, 403, "text/plain; charset=utf-8", "El panel solo se abre desde esta misma máquina: http://localhost:" + PUERTO_AGENTE);
            return;
        }
        String q = x.getRequestURI().getRawQuery();
        String aviso = q != null && q.startsWith("m=") ? URLDecoder.decode(q.substring(2), StandardCharsets.UTF_8) : null;
        responder(x, 200, "text/html; charset=utf-8", html(vista(), aviso));
    }

    // POST /accion (solo desde esta máquina): encender, apagar o mover un servicio o un bloque en cualquier máquina.
    static void accion(HttpExchange x) throws Exception {
        Map<String, String> f = formulario(x);
        if (!x.getRequestMethod().equals("POST") || !desdeAqui(x) || !TOKEN_PANEL.equals(f.get("t"))) {
            responder(x, 403, "text/plain; charset=utf-8", "No permitido.");
            return;
        }
        String ip = f.getOrDefault("ip", "");
        String accion = f.getOrDefault("accion", "");
        String que = f.getOrDefault("que", "");
        List<String> servicios = que.startsWith("bloque:") ? BLOQUES.get(que.substring(7)) : List.of(que);
        Vista v = vista();
        Maquina destino = v.maquinas().stream().filter(m -> m.ip().equals(ip) && m.conAgente() && m.responde()).findFirst().orElse(null);
        if (servicios == null || !TODOS.containsAll(servicios) || !Set.of("encender", "apagar", "mover").contains(accion) || destino == null) {
            redirigir(x, "No se pudo: la máquina no tiene el lanzador abierto o la orden no es válida.");
            return;
        }
        if (!ip.equals(nucleo) && servicios.stream().anyMatch(INFRA::contains)) {
            redirigir(x, "postgres, Eureka y el gateway solo van en el núcleo (" + nucleo + ").");
            return;
        }
        if (accion.equals("apagar")) {
            redirigir(x, ordenar(destino, "apagar", servicios).texto());
            return;
        }
        Resultado encendido = ordenar(destino, "encender", servicios);
        // Mover = encender aquí y, cuando ya responde, apagar en las demás. ms-ingesta y ms-apuestas siempre se mueven.
        List<String> mover = servicios.stream().filter(s -> accion.equals("mover") || UNA_SOLA.contains(s)).toList();
        boolean hayAlla = v.maquinas().stream().anyMatch(m -> !m.ip().equals(ip) && mover.stream().anyMatch(s -> v.activo(m, s)));
        String texto = encendido.texto();
        if (encendido.ok() && hayAlla) {
            texto += ". Cuando esté encendido allí, se apaga en la otra máquina.";
            RED.submit(() -> {
                try {
                    if (esperarEncendidos(ip, mover, 180)) {
                        quitarDeOtras(ip, mover).forEach(h -> System.out.println("[" + hora() + "] " + h));
                    } else {
                        System.out.println("[" + hora() + "] " + String.join(", ", mover) + " no terminó de arrancar en " + destino.nombre()
                                + ": no se apagó en la otra máquina.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        redirigir(x, texto);
    }

    static String html(Vista v, String aviso) {
        StringBuilder h = new StringBuilder("""
                <!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <meta http-equiv="refresh" content="5;url=/"><title>Panel de máquinas</title><style>
                :root{--tinta:#1f2937;--verde:#1b7f4b;--ambar:#a15c07;--rojo:#b42318;--gris:#6b7280;--linea:#e5e7eb;--fondo:#f8f8f6;--caja:#fff}
                body{margin:0;font:15px/1.45 system-ui,-apple-system,Segoe UI,Roboto,sans-serif;color:var(--tinta);background:var(--fondo)}
                main{max-width:980px;margin:0 auto;padding:16px}h1{font-size:1.5rem;margin:.2rem 0}h2{font-size:1.1rem;margin:0}
                .nota{color:var(--gris);margin:.2rem 0 1rem}.caja{background:var(--caja);border:1px solid var(--linea);border-radius:10px;padding:14px;margin-bottom:14px}
                .aviso{border-color:#93c5fd}.mal{border-color:var(--rojo)}code{font-size:.95em}
                .chip{display:inline-block;border:1px solid var(--linea);border-radius:99px;padding:0 8px;font-size:.8rem;margin-left:4px;color:var(--gris)}
                .chip.n{border-color:var(--tinta);color:var(--tinta)}table{width:100%;border-collapse:collapse;margin-top:8px}
                td,th{text-align:left;padding:5px 6px;border-top:1px solid var(--linea);vertical-align:middle}th{font-size:.8rem;color:var(--gris);font-weight:600}
                .encendido{color:var(--verde)}.arrancando{color:var(--ambar)}.fallando,.apagado-total{color:var(--rojo)}.apagado,.vacio{color:var(--gris)}
                form{display:inline;margin:0}button{font:inherit;font-size:.85rem;border:1px solid var(--tinta);background:var(--caja);color:var(--tinta);border-radius:6px;padding:2px 9px;cursor:pointer;margin:1px}
                button.si{background:var(--tinta);color:var(--caja)}a{color:var(--tinta)}.puerto{color:var(--gris);font-size:.8rem}
                .bloques{margin-top:8px;display:flex;flex-wrap:wrap;gap:6px;align-items:center}.trabajo{margin:.4rem 0 0;color:var(--ambar)}
                @media (prefers-color-scheme:dark){:root{--tinta:#e5e7eb;--verde:#4ade80;--ambar:#fbbf24;--rojo:#f87171;--gris:#9ca3af;--linea:#374151;--fondo:#111827;--caja:#1f2937}}
                @media (max-width:640px){td:first-child{display:none}}
                </style></head><body><main>
                <h1>Panel de máquinas</h1><p class="nota">Solo se abre en esta máquina. Se actualiza cada 5 segundos.
                En la web, la esquina inferior derecha (invisible) trae hasta aquí.</p>
                """);
        if (aviso != null) {
            h.append("<div class=\"caja aviso\" role=\"status\">").append(esc(aviso)).append("</div>");
        }
        if (!v.eurekaResponde()) {
            h.append("<div class=\"caja mal\" role=\"alert\">Eureka no responde en ").append(esc(nucleo))
                    .append(": solo se ve esta máquina. Si el núcleo es esta máquina y está arrancando, espera; si no, enciende discovery-server abajo.</div>");
        }
        List<Maquina> conectadas = v.maquinas().stream().filter(Maquina::conAgente).toList();
        long encendidos = TODOS.stream().filter(s -> !v.donde(s).isEmpty()).count();
        String hostNucleo = host(nucleo);
        Maquina conWeb = v.maquinas().stream().filter(m -> v.estado(m, "web-app").equals("encendido")).findFirst().orElse(null);
        h.append("<section class=\"caja\"><p style=\"margin:0\"><strong>").append(conectadas.size())
                .append(conectadas.size() == 1 ? " máquina" : " máquinas").append("</strong> con lanzador: ")
                .append(esc(conectadas.stream().map(Maquina::nombre).collect(Collectors.joining(", "))))
                .append(" · núcleo en ").append(esc(nucleo)).append(" · ").append(encendidos).append(" de ").append(TODOS.size())
                .append(" servicios encendidos</p><p style=\"margin:.4rem 0 0\">");
        if (conWeb != null) {
            h.append("<a target=\"_blank\" rel=\"noopener\" href=\"http://").append(esc(host(conWeb.ip()))).append(":8090/\">Abrir la web</a> · ");
        }
        h.append("<a target=\"_blank\" rel=\"noopener\" href=\"http://").append(esc(hostNucleo)).append(":8761/\">Eureka</a> · ")
                .append("<a target=\"_blank\" rel=\"noopener\" href=\"http://").append(esc(hostNucleo)).append(":8080/api/estado\">Gateway /api/estado</a>");
        if (!codigo.isEmpty()) {
            h.append(" · Código del equipo: <code>").append(esc(codigo)).append("</code>");
        }
        h.append("</p>");
        // Servicios que no están en ninguna máquina: se ofrecen botones para encenderlos donde haya lanzador.
        for (String s : TODOS) {
            if (!v.donde(s).isEmpty() || (INFRA.contains(s) && conectadas.stream().noneMatch(m -> m.ip().equals(nucleo)))) {
                continue;
            }
            h.append("<p style=\"margin:.5rem 0 0\"><span class=\"apagado-total\">● ").append(s).append(" no está en ninguna máquina.</span> Encender en:");
            for (Maquina m : conectadas) {
                if (m.responde() && (!INFRA.contains(s) || m.ip().equals(nucleo))) {
                    h.append(boton(m.ip(), "encender", s, m.nombre(), true));
                }
            }
            h.append("</p>");
        }
        h.append("</section>");

        for (Maquina m : v.maquinas()) {
            boolean esNucleo = m.ip().equals(nucleo);
            h.append("<section class=\"caja\"><h2>").append(esc(m.nombre()));
            if (esNucleo) {
                h.append("<span class=\"chip n\">núcleo</span>");
            }
            if (m.ip().equals(miIpC)) {
                h.append("<span class=\"chip\">esta máquina</span>");
            }
            h.append(" <span class=\"puerto\">").append(esc(String.join(" · ", Stream.of(m.equipo(), m.ip(), m.so()).filter(t -> !t.isBlank()).toList())))
                    .append("</span></h2>");
            if (!m.conAgente()) {
                h.append("<p class=\"trabajo\">Sin lanzador abierto: solo se ve lo que está en Eureka y no se puede controlar desde aquí.</p>");
            } else if (!m.responde()) {
                h.append("<p class=\"trabajo\">Su lanzador no responde (¿firewall o se cerró?). Se ve solo lo que está en Eureka.</p>");
            }
            if (m.trabajo() != null && !m.trabajo().isBlank()) {
                h.append("<p class=\"trabajo\">").append(esc(m.trabajo())).append("</p>");
            }
            boolean control = m.conAgente() && m.responde();
            h.append("<table><thead><tr><th>Bloque</th><th>Servicio</th><th>Estado</th><th></th></tr></thead><tbody>");
            for (Map.Entry<String, List<String>> b : BLOQUES.entrySet()) {
                boolean primero = true;
                for (String s : b.getValue()) {
                    String e = v.estado(m, s);
                    h.append("<tr><td>").append(primero ? NOMBRE_BLOQUE.get(b.getKey()) : "").append("</td><td>").append(enlace(m, s, e))
                            .append("</td><td class=\"").append(e.isEmpty() ? "vacio" : e).append("\">").append(textoEstado(e)).append("</td><td>");
                    primero = false;
                    boolean permitido = control && (!INFRA.contains(s) || esNucleo);
                    if (permitido && v.activo(m, s)) {
                        h.append(boton(m.ip(), "apagar", s, "Apagar", false));
                    } else if (permitido) {
                        boolean enOtra = !v.donde(s).isEmpty();
                        String texto = enOtra && UNA_SOLA.contains(s) ? "Mover aquí" : enOtra ? "Réplica aquí" : "Encender aquí";
                        h.append(boton(m.ip(), "encender", s, texto, !enOtra));
                        if (enOtra && !UNA_SOLA.contains(s)) {
                            h.append(boton(m.ip(), "mover", s, "Mover aquí", false));
                        }
                    }
                    h.append("</td></tr>");
                }
            }
            h.append("</tbody></table>");
            if (control) {
                h.append("<div class=\"bloques\"><span class=\"puerto\">Bloques:</span>");
                for (String b : BLOQUES.keySet()) {
                    if (b.equals("nucleo") && !esNucleo) {
                        continue;
                    }
                    boolean alguno = BLOQUES.get(b).stream().anyMatch(s -> v.activo(m, s));
                    boolean todos = BLOQUES.get(b).stream().allMatch(s -> v.activo(m, s));
                    if (!todos && !b.equals("nucleo")) {
                        h.append(boton(m.ip(), "mover", "bloque:" + b, "Mover " + NOMBRE_BLOQUE.get(b) + " aquí", false));
                    } else if (!todos) {
                        h.append(boton(m.ip(), "encender", "bloque:" + b, "Encender " + NOMBRE_BLOQUE.get(b), false));
                    }
                    if (alguno) {
                        h.append(boton(m.ip(), "apagar", "bloque:" + b, "Apagar " + NOMBRE_BLOQUE.get(b), false));
                    }
                }
                h.append("</div>");
            }
            h.append("</section>");
        }
        h.append("<section class=\"caja\"><h2>Unir otra máquina</h2><p style=\"margin:.4rem 0 0\">En la otra laptop (Windows, Mac o Linux), en la misma red ")
                .append("(si la red de la universidad no deja verse entre equipos, usen el hotspot de un celular): abre Docker, baja el repo y ejecuta ")
                .append("<code>iniciar.cmd</code> (Windows) o <code>./iniciar.sh</code> (Mac y Linux). Encuentra este núcleo (")
                .append(esc(nucleo)).append(") sola, pide una vez el código del equipo y pregunta qué levantar. Aparece aquí en unos segundos.</p></section>");
        return h.append("</main></body></html>").toString();
    }

    static String enlace(Maquina m, String servicio, String estado) {
        String puerto = ":" + PUERTOS.get(servicio);
        if (!estado.equals("encendido") || servicio.equals("postgres")) {
            return servicio + " <span class=\"puerto\">" + puerto + "</span>";
        }
        String ruta = servicio.equals("web-app") || servicio.equals("discovery-server") ? "/" : servicio.equals("api-gateway") ? "/api/estado" : "/swagger-ui/index.html";
        return "<a target=\"_blank\" rel=\"noopener\" href=\"http://" + esc(host(m.ip())) + puerto + ruta + "\">" + servicio + "</a> <span class=\"puerto\">"
                + puerto + "</span>";
    }

    static String textoEstado(String e) {
        return switch (e) {
            case "encendido" -> "● encendido";
            case "arrancando" -> "◐ arrancando";
            case "fallando" -> "● se reinicia (mira su log)";
            case "apagado" -> "○ apagado";
            default -> "–";
        };
    }

    static String boton(String ip, String accion, String que, String texto, boolean principal) {
        return "<form method=\"post\" action=\"/accion\"><input type=\"hidden\" name=\"t\" value=\"" + TOKEN_PANEL + "\">"
                + "<input type=\"hidden\" name=\"ip\" value=\"" + esc(ip) + "\"><input type=\"hidden\" name=\"accion\" value=\"" + accion + "\">"
                + "<input type=\"hidden\" name=\"que\" value=\"" + esc(que) + "\"><button" + (principal ? " class=\"si\"" : "") + ">" + esc(texto)
                + "</button></form>";
    }

    static String host(String ip) {
        return ip.equals("host.docker.internal") ? "localhost" : ip;
    }

    // ---------------------------------------------------------------- Utilidades

    static void responder(HttpExchange x, int codigoHttp, String tipo, String cuerpo) throws IOException {
        byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", tipo);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.getResponseHeaders().set("X-Frame-Options", "DENY");
        x.getResponseHeaders().set("Content-Security-Policy", "frame-ancestors 'none'");
        x.sendResponseHeaders(codigoHttp, b.length);
        try (OutputStream o = x.getResponseBody()) {
            o.write(b);
        }
    }

    static void redirigir(HttpExchange x, String aviso) throws IOException {
        x.getResponseHeaders().set("Location", "/?m=" + URLEncoder.encode(aviso, StandardCharsets.UTF_8));
        x.sendResponseHeaders(303, -1);
    }

    static Map<String, String> formulario(HttpExchange x) throws IOException {
        Map<String, String> f = new HashMap<>();
        String cuerpo = new String(x.getRequestBody().readNBytes(8192), StandardCharsets.UTF_8);
        for (String par : cuerpo.split("&")) {
            int igual = par.indexOf('=');
            if (igual > 0) {
                f.put(URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8), URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8));
            }
        }
        return f;
    }

    static String esc(String t) {
        return t == null ? "" : t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String jsonTexto(String t) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : (t == null ? "" : t).toCharArray()) {
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c < 0x20) {
                b.append(String.format("\\u%04x", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }

    static List<Object> comoLista(Object o) {
        return o == null ? List.of() : o instanceof List<?> l ? new ArrayList<>(l) : List.of(o);
    }

    static List<String> lista(Object... partes) {
        List<String> r = new ArrayList<>();
        for (Object p : partes) {
            if (p instanceof Collection<?> c) {
                c.forEach(e -> r.add(String.valueOf(e)));
            } else {
                r.add(String.valueOf(p));
            }
        }
        return r;
    }

    static String opcion(List<String> args, String nombre) {
        int i = args.indexOf(nombre);
        if (i < 0 || i + 1 >= args.size()) {
            return null;
        }
        String valor = args.get(i + 1);
        args.remove(i + 1);
        args.remove(i);
        return valor;
    }

    static java.io.BufferedReader entrada;

    static String leerLinea() throws IOException {
        if (entrada == null) {
            entrada = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        }
        return entrada.readLine();
    }

    static boolean siNo(String pregunta) throws IOException {
        System.out.print(pregunta);
        String r = leerLinea();
        return r == null || r.isBlank() || r.trim().toLowerCase(Locale.ROOT).startsWith("s");
    }

    static String nombreEquipo() {
        String e = System.getenv("COMPUTERNAME");
        if (e == null) {
            e = System.getenv("HOSTNAME");
        }
        if (e == null) {
            try {
                e = InetAddress.getLocalHost().getHostName();
            } catch (IOException ex) {
                e = "";
            }
        }
        return e;
    }

    static String hora() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    static void salir(String mensaje) {
        System.out.println(mensaje);
        System.exit(1);
    }

    // JSON mínimo (objetos, listas, textos; números y booleanos quedan como texto). Basta para Eureka, Compose y los agentes.
    static Object json(String s) {
        return new Json(s).valor();
    }

    static final class Json {
        final String s;
        int i;

        Json(String s) {
            this.s = s;
        }

        Object valor() {
            espacios();
            char c = s.charAt(i);
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                espacios();
                if (s.charAt(i) == '}') {
                    i++;
                    return m;
                }
                while (true) {
                    espacios();
                    String k = texto();
                    espacios();
                    i++; // ':'
                    m.put(k, valor());
                    espacios();
                    if (s.charAt(i++) == '}') {
                        return m;
                    }
                }
            }
            if (c == '[') {
                i++;
                List<Object> l = new ArrayList<>();
                espacios();
                if (s.charAt(i) == ']') {
                    i++;
                    return l;
                }
                while (true) {
                    l.add(valor());
                    espacios();
                    if (s.charAt(i++) == ']') {
                        return l;
                    }
                }
            }
            if (c == '"') {
                return texto();
            }
            int ini = i;
            while (i < s.length() && ",}] \n\r\t".indexOf(s.charAt(i)) < 0) {
                i++;
            }
            String t = s.substring(ini, i);
            return t.equals("null") ? null : t;
        }

        String texto() {
            i++;
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case 'n' -> b.append('\n');
                    case 't' -> b.append('\t');
                    case 'r' -> b.append('\r');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'u' -> {
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> b.append(e);
                }
            }
        }

        void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }

    // ---------------------------------------------------------------- Autoprueba: java infra/Lanzador.java --prueba

    static void pruebas() throws Exception {
        // JSON de Eureka con una instancia como objeto (no lista), escapes, números y dos registros falsos que se descartan.
        String eurekaJson = """
                {"applications":{"versions__delta":"1","application":[
                 {"name":"MS-CUOTAS","instance":{"app":"MS-CUOTAS","ipAddr":"192.168.1.20","status":"UP","port":{"$":8085,"@enabled":"true"},"metadata":{"management.port":"8085"}}},
                 {"name":"AGENTE","instance":[{"app":"AGENTE","ipAddr":"192.168.1.20","status":"UP","port":{"$":8099,"@enabled":"true"},"metadata":{"nombre":"Guille \\"G\\" \\u00d1"}},
                  {"app":"AGENTE","ipAddr":"127.0.0.1:9999/secreto?x=","status":"UP","port":{"$":8099,"@enabled":"true"}},
                  {"app":"AGENTE","ipAddr":"a b","status":"UP","port":{"$":8099,"@enabled":"true"}}]}]}}""";
        List<Instancia> ins = leerInstancias(eurekaJson);
        comprobar(ins.size() == 2, "dos instancias válidas; las IP falsas se descartan");
        comprobar(ins.get(0).app().equals("ms-cuotas") && ins.get(0).puerto() == 8085, "app en minúsculas y puerto");
        comprobar(ins.get(1).meta().get("nombre").equals("Guille \"G\" Ñ"), "escapes de JSON");
        comprobar(json(jsonTexto("a\"b\\c\n")).equals("a\"b\\c\n"), "ida y vuelta de un texto");

        // docker compose ps --format json: una lista (Compose antiguo) o un objeto por línea (Compose nuevo).
        Map<String, String> lista = leerPs("[{\"Service\":\"ms-cuotas\",\"State\":\"running\",\"Publishers\":[{\"URL\":\"0.0.0.0\"}]},{\"Service\":\"otro\",\"State\":\"running\"}]");
        Map<String, String> lineas = leerPs("{\"Service\":\"ms-cuotas\",\"State\":\"exited\"}\n{\"Service\":\"web-app\",\"State\":\"running\"}\n");
        comprobar(lista.equals(Map.of("ms-cuotas", "running")), "ps en lista, ignora servicios ajenos");
        comprobar(lineas.equals(Map.of("ms-cuotas", "exited", "web-app", "running")), "ps por líneas");

        // Bloques y argumentos.
        comprobar(serviciosDe(bloquesDe(List.of("todo"))).equals(TODOS), "todo = los 11 contenedores");
        comprobar(serviciosDe(bloquesDe(List.of("datos,nucleo"))).get(0).equals("postgres"), "orden del informe");
        comprobar(TODOS.size() == 11 && Set.copyOf(TODOS).size() == 11, "11 contenedores sin repetir");

        // .env: cambia solo esas claves y deja el resto.
        List<String> env = conValores(List.of("\uFEFFIP_EUREKA=1.1.1.1", "JWT_SECRET=x", "# IP_WEB=comentario"), Map.of("IP_EUREKA", "2.2.2.2"));
        comprobar(env.equals(List.of("JWT_SECRET=x", "# IP_WEB=comentario", "IP_EUREKA=2.2.2.2")), "valores del .env reemplazados");

        // Código del equipo y firma: el mismo código da la misma firma; otro código u otra IP de destino, otra.
        String nuevo = nuevoCodigo();
        comprobar(nuevo.matches("[A-Z2-9]{4}-[A-Z2-9]{4}") && !nuevo.equals(nuevoCodigo()), "código nuevo con formato y al azar");
        comprobar(normalizarCodigo(" k7qf m2xd ").equals("K7QF-M2XD") && normalizarCodigo("K7QF-M2XD").equals("K7QF-M2XD"), "código normalizado");
        codigo = "K7QF-M2XD";
        String f1 = firmar(mensajeFirmado("encender", List.of("ms-cuotas"), 100, "192.168.1.20"));
        comprobar(f1.equals(firmar(mensajeFirmado("encender", List.of("ms-cuotas"), 100, "192.168.1.20"))) && f1.length() == 64, "firma estable");
        comprobar(!f1.equals(firmar(mensajeFirmado("encender", List.of("ms-cuotas"), 100, "192.168.1.21"))), "otra IP de destino, otra firma");
        codigo = "ABCD-EFGH";
        comprobar(!f1.equals(firmar(mensajeFirmado("encender", List.of("ms-cuotas"), 100, "192.168.1.20"))), "otro código, otra firma");
        codigo = "";
        boolean sinCodigo = false;
        try {
            firmar("x");
        } catch (IllegalStateException e) {
            sinCodigo = true;
        }
        comprobar(sinCodigo, "sin código no se firma nada");

        // Estados: si se conoce el contenedor, manda él; si no, Eureka.
        Maquina m = new Maquina("192.168.1.20", "G", "", "", true, true, "", 8099,
                Map.of("ms-cuotas", "running", "ms-apuestas", "running", "web-app", "running", "ms-ingesta", "restarting", "ms-catalogo", "exited"));
        Maquina sinAgente = new Maquina("192.168.1.20", "G", "", "", false, false, "", 0, Map.of());
        Vista v = new Vista(true, List.of(m), ins);
        comprobar(v.estado(m, "ms-cuotas").equals("encendido"), "en Eureka = encendido");
        comprobar(v.estado(m, "ms-apuestas").equals("arrancando"), "contenedor sin Eureka = arrancando");
        comprobar(v.estado(m, "web-app").equals("encendido"), "web-app no usa Eureka");
        comprobar(v.estado(m, "ms-ingesta").equals("fallando"), "restarting = fallando");
        comprobar(v.estado(m, "ms-catalogo").equals("apagado"), "exited = apagado");
        comprobar(v.estado(m, "ms-usuarios").isEmpty(), "sin contenedor = vacío");
        Maquina apagadaPeroEnEureka = new Maquina("192.168.1.20", "G", "", "", true, true, "", 8099, Map.of("ms-cuotas", "exited"));
        comprobar(v.estado(apagadaPeroEnEureka, "ms-cuotas").equals("apagado"), "el contenedor apagado gana a Eureka atrasado");
        comprobar(v.estado(sinAgente, "ms-cuotas").equals("encendido"), "sin agente, solo Eureka");
        comprobar(v.donde("ms-ingesta").size() == 1 && v.donde("ms-catalogo").isEmpty(), "dónde corre");
        System.out.println("Autoprueba del lanzador: todo bien.");
    }

    static void comprobar(boolean condicion, String que) {
        if (!condicion) {
            throw new AssertionError("Falla: " + que);
        }
    }
}
