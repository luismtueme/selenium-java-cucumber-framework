package io.github.luismtueme.framework.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * The single source of settings for the framework, the demo app and the Cucumber suite.
 *
 * <p>Each setting is read from, first match wins: JVM system properties ({@code -DTEST_BROWSER=firefox}),
 * environment variables, a {@code .env} file in the working directory, then {@code config/defaults.properties}
 * on the classpath (non-secret defaults only). Every variable is listed in {@code .env.example}.
 *
 * <p>Invalid values fail at startup with the variable name, so a typo never silently falls back to a default.
 */
public record Config(
        String baseUrl,
        String apiBaseUrl,
        String environment,
        Browser browser,
        boolean headless,
        int windowWidth,
        int windowHeight,
        Duration waitTimeout,
        Duration pageLoadTimeout,
        Optional<URI> seleniumRemoteUrl,
        Optional<Credentials> credentials,
        Optional<DbConfig> db,
        String demoAppHost,
        int demoAppPort) {

    private static final String DEFAULTS = "/config/defaults.properties";

    /** True when the tests run against the bundled demo app (BASE_URL is empty). */
    public boolean usesDemoApp() {
        return baseUrl.isEmpty();
    }

    /** The login for UI and API tests, or a message saying which variables to set. */
    public Credentials requireCredentials() {
        return credentials.orElseThrow(
                () -> new ConfigException("APP_USERNAME and APP_PASSWORD must be set to test " + baseUrl));
    }

    /** The settings for this run, loaded once. */
    public static Config get() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Config INSTANCE = load();
    }

    /** Loads from system properties, environment variables, {@code .env} and the classpath defaults. */
    public static Config load() {
        Map<String, String> dotenv = new HashMap<>();
        for (DotenvEntry entry :
                Dotenv.configure().ignoreIfMissing().load().entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
            dotenv.put(entry.getKey(), entry.getValue());
        }
        Properties defaults = new Properties();
        try (InputStream in = Config.class.getResourceAsStream(DEFAULTS)) {
            if (in != null) defaults.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + DEFAULTS, e);
        }
        return from(name -> {
            String value = System.getProperty(name);
            if (value == null) value = System.getenv(name);
            if (value == null) value = dotenv.get(name);
            if (value == null) value = defaults.getProperty(name);
            return value;
        });
    }

    /**
     * Builds a config from a lookup function (variable name to value, or null when unset). Unit tests pass a map.
     *
     * @throws ConfigException naming the first invalid variable
     */
    public static Config from(UnaryOperator<String> lookup) {
        Settings settings = new Settings(lookup);

        String baseUrl = settings.url("BASE_URL").orElse("");
        String apiBaseUrl = settings.url("API_BASE_URL").orElse(baseUrl);

        // The demo credentials are only ever used against the demo app
        Optional<Credentials> credentials = settings.text("APP_USERNAME")
                .flatMap(user -> settings.text("APP_PASSWORD").map(password -> new Credentials(user, password)));
        if (credentials.isEmpty() && baseUrl.isEmpty()) credentials = Optional.of(Credentials.DEMO);

        boolean ci = settings.text("CI")
                .map(value -> !value.equalsIgnoreCase("false"))
                .orElse(false);
        boolean headless = settings.bool("HEADLESS")
                .orElse(ci || settings.bool("HEADLESS_DEFAULT").orElse(false));

        String windowSize = settings.text("WINDOW_SIZE").orElse("1280x720");
        String[] size = windowSize.split("x");
        if (size.length != 2 || !size[0].matches("\\d+") || !size[1].matches("\\d+")) {
            throw new ConfigException("WINDOW_SIZE must look like 1280x720, got \"%s\"".formatted(windowSize));
        }

        Optional<DbConfig> db = settings.text("DB_HOST")
                .map(host -> new DbConfig(
                        host,
                        settings.positiveInt("DB_PORT").orElse(3306),
                        settings.require("DB_USER", "DB_HOST is set"),
                        settings.text("DB_PASSWORD").orElse(""),
                        settings.require("DB_NAME", "DB_HOST is set")));

        return new Config(
                baseUrl,
                apiBaseUrl,
                settings.text("TEST_ENV").orElse("local"),
                settings.text("TEST_BROWSER")
                        .map(value -> Browser.fromId("TEST_BROWSER", value))
                        .orElse(Browser.CHROME),
                headless,
                Integer.parseInt(size[0]),
                Integer.parseInt(size[1]),
                Duration.ofMillis(settings.positiveInt("WAIT_TIMEOUT").orElse(10_000)),
                Duration.ofMillis(settings.positiveInt("PAGE_LOAD_TIMEOUT").orElse(30_000)),
                settings.url("SELENIUM_REMOTE_URL").map(URI::create),
                credentials,
                db,
                settings.text("DEMO_APP_HOST").orElse("127.0.0.1"),
                settings.port("DEMO_APP_PORT").orElse(4173));
    }

    /** Typed, validated reads. Blank values count as unset. */
    private record Settings(UnaryOperator<String> lookup) {

        Optional<String> text(String name) {
            return Optional.ofNullable(lookup.apply(name)).map(String::trim).filter(value -> !value.isEmpty());
        }

        String require(String name, String because) {
            return text(name)
                    .orElseThrow(() -> new ConfigException("%s must be set because %s".formatted(name, because)));
        }

        Optional<Boolean> bool(String name) {
            return text(name).map(value -> switch (value.toLowerCase(java.util.Locale.ROOT)) {
                case "true", "1", "yes" -> true;
                case "false", "0", "no" -> false;
                default -> throw new ConfigException("%s must be true or false, got \"%s\"".formatted(name, value));
            });
        }

        Optional<Integer> positiveInt(String name) {
            return text(name).map(value -> {
                try {
                    int number = Integer.parseInt(value);
                    if (number > 0) return number;
                } catch (NumberFormatException ignored) {
                    // reported below
                }
                throw new ConfigException("%s must be a positive whole number, got \"%s\"".formatted(name, value));
            });
        }

        Optional<Integer> port(String name) {
            return text(name).map(value -> {
                if (value.matches("\\d{1,5}") && Integer.parseInt(value) <= 65_535) return Integer.parseInt(value);
                throw new ConfigException("%s must be a port number (0-65535), got \"%s\"".formatted(name, value));
            });
        }

        /** An http(s) URL without a trailing slash, so paths can be appended as {@code baseUrl + "/login"}. */
        Optional<String> url(String name) {
            return text(name).map(value -> {
                if (!value.matches("https?://[^\\s/]+(/\\S*)?")) {
                    throw new ConfigException("%s must be an http(s) URL like https://app.example.com, got \"%s\""
                            .formatted(name, value));
                }
                return value.replaceAll("/+$", "");
            });
        }
    }
}
