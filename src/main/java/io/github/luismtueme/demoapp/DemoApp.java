package io.github.luismtueme.demoapp;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.luismtueme.framework.config.Config;
import io.github.luismtueme.framework.config.Credentials;
import io.github.luismtueme.framework.db.DbClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo application under test: a small web app and JSON API, so the example scenarios have a deterministic target.
 * Point BASE_URL at your real application and delete this package when you adopt the framework.
 *
 * <pre>
 *   Pages:  /            form page
 *           /login       login page (?next=/items returns there after login)
 *           /items       items page, needs a session (otherwise redirects to /login)
 *   API:    POST   /api/login        { username, password } -> { token, user }, also sets a session cookie
 *           GET    /api/items        (auth) list items
 *           POST   /api/items        (auth) { name } -> item
 *           GET    /api/items/{id}   (auth)
 *           DELETE /api/items/{id}   (auth)
 * </pre>
 *
 * API routes accept either {@code Authorization: Bearer <token>} or the session cookie. Items are kept in memory, or
 * in MySQL when DB_HOST is set.
 *
 * <p>Run standalone: {@code ./mvnw exec:java} (port DEMO_APP_PORT, default 4173).
 */
public final class DemoApp implements AutoCloseable {

    private static final Map<String, String> PAGES =
            Map.of("/", "index.html", "/login", "login.html", "/items", "items.html");
    private static final Set<String> PROTECTED_PAGES = Set.of("/items");
    private static final String SESSION_COOKIE = "session";
    private static final Pattern ITEM_PATH = Pattern.compile("^/api/items/(\\d+)$");
    private static final Pattern BEARER = Pattern.compile("^Bearer (.+)$");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final ExecutorService executor;
    private final ItemStore store;
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final String url;

    private DemoApp(String host, int port, ItemStore store) throws IOException {
        this.store = store;
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
        String publicHost = host.equals("0.0.0.0") ? "localhost" : host;
        this.url = "http://%s:%d".formatted(publicHost, server.getAddress().getPort());
    }

    /**
     * Starts the app. Port 0 picks a free port.
     *
     * @param host the interface to listen on: "127.0.0.1" for this machine only, "0.0.0.0" to accept other containers
     */
    public static DemoApp start(String host, int port, ItemStore store) {
        try {
            return new DemoApp(host, port, store);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start the demo app on %s:%d".formatted(host, port), e);
        }
    }

    /** The store the settings ask for: MySQL when DB_HOST is set, otherwise memory. */
    public static ItemStore storeFor(Config config) {
        return config.db()
                .<ItemStore>map(db -> new MySqlItemStore(new DbClient(db)))
                .orElseGet(MemoryItemStore::new);
    }

    public String url() {
        return url;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.close();
    }

    private void handle(HttpExchange exchange) throws IOException {
        // Not try-with-resources: that would close the exchange before the error response is sent
        try {
            route(exchange);
        } catch (HttpError error) {
            send(exchange, error.status, Map.of("error", Map.of("code", error.code, "message", error.getMessage())));
        } catch (RuntimeException error) {
            send(
                    exchange,
                    500,
                    Map.of("error", Map.of("code", "SERVER_ERROR", "message", String.valueOf(error.getMessage()))));
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String page = PAGES.get(path);
        if (method.equals("GET") && page != null) {
            if (PROTECTED_PAGES.contains(path) && !sessions.contains(sessionToken(exchange))) {
                exchange.getResponseHeaders()
                        .add("Location", "/login?next=" + URLEncoder.encode(path, StandardCharsets.UTF_8));
                send(exchange, 302, "");
                return;
            }
            send(exchange, 200, readPage(page));
            return;
        }

        if (method.equals("POST") && path.equals("/api/login")) {
            Map<?, ?> body = readJson(exchange);
            Credentials demo = Credentials.DEMO;
            if (!demo.username().equals(body.get("username"))
                    || !demo.password().equals(body.get("password"))) {
                throw new HttpError(401, "INVALID_CREDENTIALS", "Invalid username or password");
            }
            String token = UUID.randomUUID().toString();
            sessions.add(token);
            exchange.getResponseHeaders()
                    .add("Set-Cookie", "%s=%s; HttpOnly; SameSite=Lax; Path=/".formatted(SESSION_COOKIE, token));
            send(exchange, 200, Map.of("token", token, "user", Map.of("username", demo.username())));
            return;
        }

        if (path.startsWith("/api/items")) {
            if (!sessions.contains(sessionToken(exchange))) {
                throw new HttpError(401, "UNAUTHORIZED", "Missing or invalid token");
            }
            if (path.equals("/api/items") && method.equals("GET")) {
                send(exchange, 200, Map.of("items", store.list()));
                return;
            }
            if (path.equals("/api/items") && method.equals("POST")) {
                Object name = readJson(exchange).get("name");
                if (!(name instanceof String text) || text.isBlank()) {
                    send(
                            exchange,
                            400,
                            Map.of(
                                    "error",
                                    Map.of(
                                            "code",
                                            "VALIDATION_ERROR",
                                            "field",
                                            "name",
                                            "message",
                                            "Name is required")));
                    return;
                }
                send(exchange, 201, store.create(text.trim()));
                return;
            }
            Matcher item = ITEM_PATH.matcher(path);
            if (item.matches()) {
                long id = Long.parseLong(item.group(1));
                if (method.equals("GET")) {
                    Optional<Item> found = store.get(id);
                    if (found.isPresent()) {
                        send(exchange, 200, found.get());
                        return;
                    }
                }
                if (method.equals("DELETE") && store.remove(id)) {
                    send(exchange, 204, "");
                    return;
                }
            }
        }

        throw new HttpError(404, "NOT_FOUND", "No such resource: %s %s".formatted(method, path));
    }

    private static String sessionToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null) {
            Matcher bearer = BEARER.matcher(authorization);
            if (bearer.matches()) return bearer.group(1);
        }
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) return "";
        return Arrays.stream(cookies.split(";\\s*"))
                .filter(cookie -> cookie.startsWith(SESSION_COOKIE + "="))
                .map(cookie -> cookie.substring(SESSION_COOKIE.length() + 1))
                .findFirst()
                .orElse("");
    }

    private static Map<?, ?> readJson(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length == 0) return Map.of();
        try {
            return JSON.readValue(raw, Map.class);
        } catch (JacksonException e) {
            throw new HttpError(400, "INVALID_JSON", "Invalid JSON body");
        }
    }

    /** Strings are sent as HTML, everything else as JSON. */
    private static void send(HttpExchange exchange, int status, Object body) throws IOException {
        boolean html = body instanceof String;
        byte[] bytes = html ? ((String) body).getBytes(StandardCharsets.UTF_8) : JSON.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", html ? "text/html; charset=utf-8" : "application/json");
        if (status == 204 || bytes.length == 0) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String readPage(String file) throws IOException {
        try (InputStream in = DemoApp.class.getResourceAsStream("/demo-app/public/" + file)) {
            if (in == null) throw new IllegalStateException("Missing page: demo-app/public/" + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class HttpError extends RuntimeException {
        private final int status;
        private final String code;

        HttpError(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Config config = Config.get();
        DemoApp app = start(config.demoAppHost(), config.demoAppPort(), storeFor(config));
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        System.out.println("Demo app running at " + app.url());
        Thread.currentThread().join();
    }
}
