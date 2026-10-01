package io.github.luismtueme.framework.browser;

import io.github.luismtueme.framework.config.Config;
import java.time.Duration;
import java.util.Map;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Creates one browser per scenario.
 *
 * <p>Local runs use Selenium Manager, which finds or downloads the right driver and browser, so there is nothing to
 * install. With SELENIUM_REMOTE_URL set, the browser runs on a Selenium Grid instead.
 *
 * <p>WebDriver BiDi is enabled on every session: it gives the framework browser console logs for failure evidence and
 * lets {@link BrowserClock} run scripts before the page's own.
 */
public final class DriverFactory {

    private DriverFactory() {}

    public static WebDriver create(Config config) {
        AbstractDriverOptions<?> options = options(config);
        WebDriver driver = config.seleniumRemoteUrl()
                .map(url -> new Augmenter()
                        .augment(RemoteWebDriver.builder()
                                .oneOf(options)
                                .address(url)
                                .build()))
                .orElseGet(() -> switch (config.browser()) {
                    case CHROME -> new ChromeDriver((ChromeOptions) options);
                    case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
                    case EDGE -> new EdgeDriver((EdgeOptions) options);
                });

        // Explicit waits only (in the page objects); an implicit wait would make every lookup slow to fail
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(config.pageLoadTimeout());
        driver.manage().window().setSize(new Dimension(config.windowWidth(), config.windowHeight()));
        return driver;
    }

    static AbstractDriverOptions<?> options(Config config) {
        return switch (config.browser()) {
            case CHROME -> chromium(new ChromeOptions(), config);
            case EDGE -> chromium(new EdgeOptions(), config);
            case FIREFOX -> {
                FirefoxOptions firefox = new FirefoxOptions().enableBiDi();
                if (config.headless()) firefox.addArguments("-headless");
                yield firefox;
            }
        };
    }

    private static <T extends ChromiumOptions<?>> T chromium(T options, Config config) {
        options.enableBiDi();
        options.addArguments("--window-size=%d,%d".formatted(config.windowWidth(), config.windowHeight()));
        if (config.headless()) options.addArguments("--headless");
        // No "save password" or "password found in a data breach" dialogs over the page under test
        options.setExperimentalOption(
                "prefs",
                Map.of(
                        "credentials_enable_service", false,
                        "profile.password_manager_enabled", false,
                        "profile.password_manager_leak_detection", false));
        return options;
    }
}
