package io.github.luismtueme.framework.browser;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luismtueme.framework.config.Config;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;

class DriverFactoryTest {

    private static Config config(Map<String, String> values) {
        return Config.from(values::get);
    }

    @Test
    void headlessChromiumUsesNewHeadlessAndCiFlags() {
        for (String browser : List.of("chrome", "edge")) {
            List<String> args = chromiumArgs(config(Map.of("TEST_BROWSER", browser, "HEADLESS", "true")));
            assertThat(args)
                    .as(browser)
                    .contains("--headless=new", "--no-sandbox", "--disable-dev-shm-usage")
                    .doesNotContain("--headless");
        }
    }

    @Test
    void headedChromiumDoesNotAddHeadlessFlags() {
        List<String> args = chromiumArgs(config(Map.of("TEST_BROWSER", "edge")));
        assertThat(args)
                .contains("--window-size=1280,720")
                .doesNotContain("--headless", "--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
    }

    @Test
    void headlessFirefoxUsesFirefoxHeadless() {
        FirefoxOptions options =
                (FirefoxOptions) DriverFactory.options(config(Map.of("TEST_BROWSER", "firefox", "HEADLESS", "true")));
        assertThat(options.asMap().toString()).contains("-headless");
    }

    @SuppressWarnings("unchecked")
    private static List<String> chromiumArgs(Config config) {
        AbstractDriverOptions<?> options = DriverFactory.options(config);
        String key = options instanceof EdgeOptions ? "ms:edgeOptions" : "goog:chromeOptions";
        Map<String, Object> nested = (Map<String, Object>) options.asMap().get(key);
        assertThat(options).isInstanceOfAny(ChromeOptions.class, EdgeOptions.class);
        return (List<String>) nested.get("args");
    }
}
