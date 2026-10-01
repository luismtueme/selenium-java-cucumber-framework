package io.github.luismtueme.demoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luismtueme.framework.api.ApiClient;
import io.github.luismtueme.framework.config.Config;
import io.github.luismtueme.framework.config.Credentials;
import io.restassured.response.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The demo app and the API client, together: every route, through the client the scenarios use. */
class DemoAppTest {

    private static DemoApp app;
    private static ApiClient api;
    private static ApiClient authed;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void start() {
        app = DemoApp.start("127.0.0.1", 0, new MemoryItemStore());
        api = new ApiClient(app.url());
        authed = api.withToken(api.login(Credentials.DEMO));
    }

    @AfterAll
    static void stop() {
        app.close();
    }

    private static HttpResponse<String> browserGet(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(app.url() + path));
        if (cookie != null) request.header("Cookie", cookie);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void listensOnAFreePort() {
        assertThat(app.url()).matches("http://127\\.0\\.0\\.1:\\d+");
    }

    @Test
    void servesThePublicPages() throws Exception {
        HttpResponse<String> form = browserGet("/", null);
        assertThat(form.statusCode()).isEqualTo(200);
        assertThat(form.headers().firstValue("Content-Type")).contains("text/html; charset=utf-8");
        assertThat(form.body()).contains("<title>Example Application</title>");
        assertThat(browserGet("/login", null).body()).contains("<h1>Log in</h1>");
    }

    @Test
    void theItemsPageRedirectsToLoginWithoutASession() throws Exception {
        HttpResponse<String> response = browserGet("/items", null);
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).contains("/login?next=%2Fitems");
    }

    @Test
    void loginSetsASessionCookieThatOpensTheItemsPage() throws Exception {
        Response login = api.post("/api/login", Map.of("username", "demo-user", "password", "demo-password"));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.jsonPath().getString("user.username")).isEqualTo("demo-user");
        String cookie = login.getHeader("Set-Cookie");
        assertThat(cookie)
                .contains("HttpOnly")
                .startsWith("session=" + login.jsonPath().getString("token"));

        HttpResponse<String> items = browserGet("/items", cookie.split(";")[0]);
        assertThat(items.statusCode()).isEqualTo(200);
        assertThat(items.body()).contains("<h1>Items</h1>");
    }

    @Test
    void wrongCredentialsAreRejected() {
        Response response = api.post("/api/login", Map.of("username", "demo-user", "password", "nope"));
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error.message")).isEqualTo("Invalid username or password");
        assertThatThrownBy(() -> api.login(new Credentials("demo-user", "nope")))
                .hasMessageStartingWith("Login as demo-user failed with HTTP 401");
    }

    @Test
    void itemsNeedAToken() {
        assertThat(api.get("/api/items").statusCode()).isEqualTo(401);
        assertThat(api.withToken("made-up").get("/api/items").jsonPath().getString("error.code"))
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    void createsFetchesListsAndDeletesItems() {
        Response created = authed.post("/api/items", Map.of("name", "  Manhole 42  "));
        assertThat(created.statusCode()).isEqualTo(201);
        long id = created.jsonPath().getLong("id");
        assertThat(created.jsonPath().getString("name")).as("trimmed").isEqualTo("Manhole 42");
        assertThat(created.jsonPath().getString("createdAt")).isNotBlank();

        assertThat(authed.get("/api/items/" + id).jsonPath().getString("name")).isEqualTo("Manhole 42");
        assertThat(authed.get("/api/items").jsonPath().getList("items.id", Long.class))
                .contains(id);

        assertThat(authed.delete("/api/items/" + id).statusCode()).isEqualTo(204);
        assertThat(authed.get("/api/items/" + id).statusCode()).isEqualTo(404);
        assertThat(authed.delete("/api/items/" + id).statusCode()).isEqualTo(404);
    }

    @Test
    void anItemNeedsAName() {
        for (Object body : new Object[] {Map.of("name", " "), Map.of("name", 5), Map.of()}) {
            Response response = authed.post("/api/items", body);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.jsonPath().getString("error.field")).isEqualTo("name");
        }
    }

    @Test
    void invalidJsonIsABadRequest() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(app.url() + "/api/login"))
                        .POST(HttpRequest.BodyPublishers.ofString("{not json"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("INVALID_JSON");
    }

    @Test
    void unknownRoutesAreNotFound() {
        assertThat(api.get("/nope").statusCode()).isEqualTo(404);
        assertThat(authed.get("/api/items/abc").statusCode()).isEqualTo(404);
    }

    @Test
    void usesMemoryWithoutADatabase() {
        assertThat(DemoApp.storeFor(Config.from(name -> null))).isInstanceOf(MemoryItemStore.class);
    }

    @Test
    void canListenOnAllInterfaces() {
        try (DemoApp everywhere = DemoApp.start("0.0.0.0", 0, new MemoryItemStore())) {
            assertThat(everywhere.url()).startsWith("http://localhost:");
            assertThat(new ApiClient(everywhere.url()).get("/").statusCode()).isEqualTo(200);
        }
    }

    @Test
    void aPortInUseFailsWithTheAddress() {
        int port = URI.create(app.url()).getPort();
        assertThatThrownBy(() -> DemoApp.start("127.0.0.1", port, new MemoryItemStore()))
                .hasMessage("Could not start the demo app on 127.0.0.1:" + port);
    }
}
