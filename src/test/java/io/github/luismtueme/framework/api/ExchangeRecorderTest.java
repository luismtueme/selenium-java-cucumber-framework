package io.github.luismtueme.framework.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luismtueme.demoapp.DemoApp;
import io.github.luismtueme.demoapp.MemoryItemStore;
import io.github.luismtueme.framework.config.Credentials;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Requests reach the report with secrets masked. */
class ExchangeRecorderTest {

    private DemoApp app;

    @BeforeEach
    void start() {
        app = DemoApp.start("127.0.0.1", 0, new MemoryItemStore());
    }

    @AfterEach
    void stop() {
        app.close();
    }

    @Test
    void recordsEachExchangeWithSecretsMasked() {
        List<String> names = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        ApiClient api = new ApiClient(app.url()).recordingTo((name, json) -> {
            names.add(name);
            documents.add(json);
        });

        String token = api.login(Credentials.DEMO);
        api.withToken(token).post("/api/items", Map.of("name", "Outfall 3"));

        assertThat(names).containsExactly("POST /api/login -> 200", "POST /api/items -> 201");
        assertThat(String.join("\n", documents))
                .contains("\"method\" : \"POST\"", "\"status\" : 201", "\"name\" : \"Outfall 3\"")
                .contains("\"Authorization\" : \"***\"", "\"password\" : \"***\"")
                .doesNotContain("demo-password", token);
    }

    @Test
    void recordsBodiesThatArentJsonAsText() {
        List<String> documents = new ArrayList<>();
        new ApiClient(app.url())
                .recordingTo((name, json) -> documents.add(json))
                .get("/login");
        assertThat(documents).singleElement().asString().contains("<h1>Log in</h1>");
    }

    @Test
    void recordsNothingByDefault() {
        assertThat(new ApiClient(app.url()).get("/api/items").statusCode()).isEqualTo(401);
    }
}
