package io.github.luismtueme.framework.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigTest {

    private static Config config(Map<String, String> values) {
        return Config.from(values::get);
    }

    @Test
    void defaultsRunAgainstTheDemoAppInChrome() {
        Config config = config(Map.of());
        assertThat(config.usesDemoApp()).isTrue();
        assertThat(config.browser()).isEqualTo(Browser.CHROME);
        assertThat(config.headless()).isFalse();
        assertThat(config.environment()).isEqualTo("local");
        assertThat(config.windowWidth()).isEqualTo(1280);
        assertThat(config.windowHeight()).isEqualTo(720);
        assertThat(config.waitTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.pageLoadTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.seleniumRemoteUrl()).isEmpty();
        assertThat(config.db()).isEmpty();
        assertThat(config.demoAppHost()).isEqualTo("127.0.0.1");
        assertThat(config.demoAppPort()).isEqualTo(4173);
    }

    @Test
    void theDemoAppUsesTheDemoCredentials() {
        assertThat(config(Map.of()).requireCredentials()).isEqualTo(Credentials.DEMO);
    }

    @Test
    void aRealApplicationNeverGetsTheDemoCredentials() {
        Config config = config(Map.of("BASE_URL", "https://app.example.com"));
        assertThat(config.credentials()).isEmpty();
        assertThatThrownBy(config::requireCredentials)
                .isInstanceOf(ConfigException.class)
                .hasMessage("APP_USERNAME and APP_PASSWORD must be set to test https://app.example.com");
    }

    @Test
    void readsEverySetting() {
        Config config = config(Map.ofEntries(
                Map.entry("BASE_URL", "https://app.example.com/"),
                Map.entry("API_BASE_URL", "https://api.example.com"),
                Map.entry("TEST_ENV", "qa"),
                Map.entry("TEST_BROWSER", " Firefox "),
                Map.entry("HEADLESS", "yes"),
                Map.entry("WINDOW_SIZE", "1920x1080"),
                Map.entry("WAIT_TIMEOUT", "2500"),
                Map.entry("PAGE_LOAD_TIMEOUT", "60000"),
                Map.entry("SELENIUM_REMOTE_URL", "http://grid:4444"),
                Map.entry("APP_USERNAME", "qa-user"),
                Map.entry("APP_PASSWORD", "secret"),
                Map.entry("DEMO_APP_HOST", "0.0.0.0"),
                Map.entry("DEMO_APP_PORT", "0")));

        assertThat(config.baseUrl()).as("trailing slash removed").isEqualTo("https://app.example.com");
        assertThat(config.apiBaseUrl()).isEqualTo("https://api.example.com");
        assertThat(config.environment()).isEqualTo("qa");
        assertThat(config.browser()).isEqualTo(Browser.FIREFOX);
        assertThat(config.headless()).isTrue();
        assertThat(config.windowWidth()).isEqualTo(1920);
        assertThat(config.windowHeight()).isEqualTo(1080);
        assertThat(config.waitTimeout()).isEqualTo(Duration.ofMillis(2500));
        assertThat(config.pageLoadTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.seleniumRemoteUrl()).contains(URI.create("http://grid:4444"));
        assertThat(config.requireCredentials()).isEqualTo(new Credentials("qa-user", "secret"));
        assertThat(config.demoAppHost()).isEqualTo("0.0.0.0");
        assertThat(config.demoAppPort()).isZero();
    }

    @Test
    void theApiUrlDefaultsToTheBaseUrl() {
        assertThat(config(Map.of("BASE_URL", "https://app.example.com")).apiBaseUrl())
                .isEqualTo("https://app.example.com");
    }

    @Test
    void ciRunsHeadlessUnlessToldOtherwise() {
        assertThat(config(Map.of("CI", "true")).headless()).isTrue();
        assertThat(config(Map.of("CI", "false")).headless()).isFalse();
        assertThat(config(Map.of("CI", "true", "HEADLESS", "false")).headless()).isFalse();
        assertThat(config(Map.of("HEADLESS_DEFAULT", "true")).headless()).isTrue();
    }

    @Test
    void blankValuesCountAsUnset() {
        assertThat(config(Map.of("BASE_URL", "  ", "TEST_BROWSER", "")).usesDemoApp())
                .isTrue();
    }

    @Test
    void databaseSettingsAreReadWhenDbHostIsSet() {
        Config config =
                config(Map.of("DB_HOST", "127.0.0.1", "DB_USER", "tester", "DB_PASSWORD", "pw", "DB_NAME", "testdb"));
        assertThat(config.db()).contains(new DbConfig("127.0.0.1", 3306, "tester", "pw", "testdb"));
        assertThat(config.db().orElseThrow().jdbcUrl()).isEqualTo("jdbc:mysql://127.0.0.1:3306/testdb");
    }

    @Test
    void databaseSettingsNeedAUserAndADatabaseName() {
        assertThatThrownBy(() -> config(Map.of("DB_HOST", "db", "DB_NAME", "x")))
                .hasMessage("DB_USER must be set because DB_HOST is set");
        assertThatThrownBy(() -> config(Map.of("DB_HOST", "db", "DB_USER", "x")))
                .hasMessage("DB_NAME must be set because DB_HOST is set");
    }

    @Test
    void invalidValuesNameTheVariable() {
        assertThatThrownBy(() -> config(Map.of("TEST_BROWSER", "ie11")))
                .isInstanceOf(ConfigException.class)
                .hasMessage("TEST_BROWSER must be one of chrome, firefox, edge, got \"ie11\"");
        assertThatThrownBy(() -> config(Map.of("HEADLESS", "maybe")))
                .hasMessage("HEADLESS must be true or false, got \"maybe\"");
        assertThatThrownBy(() -> config(Map.of("WAIT_TIMEOUT", "-5")))
                .hasMessage("WAIT_TIMEOUT must be a positive whole number, got \"-5\"");
        assertThatThrownBy(() -> config(Map.of("WAIT_TIMEOUT", "soon")))
                .hasMessage("WAIT_TIMEOUT must be a positive whole number, got \"soon\"");
        assertThatThrownBy(() -> config(Map.of("DEMO_APP_PORT", "70000")))
                .hasMessage("DEMO_APP_PORT must be a port number (0-65535), got \"70000\"");
        assertThatThrownBy(() -> config(Map.of("WINDOW_SIZE", "big")))
                .hasMessage("WINDOW_SIZE must look like 1280x720, got \"big\"");
        assertThatThrownBy(() -> config(Map.of("BASE_URL", "app.example.com")))
                .hasMessage("BASE_URL must be an http(s) URL like https://app.example.com, got \"app.example.com\"");
    }

    @Test
    void secretsNeverAppearInToString() {
        assertThat(new Credentials("user", "hunter2").toString())
                .doesNotContain("hunter2")
                .contains("user");
        assertThat(new DbConfig("db", 3306, "user", "hunter2", "testdb").toString())
                .doesNotContain("hunter2");
    }

    @Test
    void loadReadsSystemPropertiesFirst() {
        System.setProperty("TEST_ENV", "from-system-property");
        try {
            assertThat(Config.load().environment()).isEqualTo("from-system-property");
        } finally {
            System.clearProperty("TEST_ENV");
        }
    }

    @Test
    void loadFallsBackToTheClasspathDefaults() {
        assertThat(Config.load().waitTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(Config.get()).isSameAs(Config.get());
    }
}
