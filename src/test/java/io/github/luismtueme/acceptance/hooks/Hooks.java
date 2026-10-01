package io.github.luismtueme.acceptance.hooks;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import io.github.luismtueme.acceptance.support.AppUnderTest;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.config.Config;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.opentest4j.TestAbortedException;

/**
 * Cucumber lifecycle.
 *
 * <ul>
 *   <li>Once per run: start the app under test (the demo app, unless BASE_URL is set), and at
 *       the end check that no test data was left behind.
 *   <li>{@code @ui}: a fresh browser per scenario; on failure, a screenshot, the URL and the page source.
 *   <li>{@code @authenticated}: the scenario starts logged in, with the run's saved session.
 *   <li>{@code @db}: skipped when no database is configured.
 *   <li>Every scenario: cleanups run afterwards, pass or fail.
 * </ul>
 *
 * <p>Before hooks run in ascending {@code order}, after hooks in descending order.
 */
public class Hooks {

    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void startApplication() {
        AppUnderTest.start(Config.get());
    }

    @AfterAll
    public static void checkLeftoverDataAndStop() {
        try {
            // Only for the bundled demo app: its data belongs to this run alone
            if (AppUnderTest.ownsDemoApp()) {
                Response items = new TestContext().authenticatedApi().get("/api/items");
                List<String> names = items.jsonPath().getList("items.name", String.class);
                if (!names.isEmpty()) {
                    throw new IllegalStateException(
                            ("Scenarios left %d item(s) behind: %s. Every scenario must delete what it"
                                            + " creates (TestContext.cleanUpItem or addCleanup).")
                                    .formatted(names.size(), names));
                }
            }
        } finally {
            AppUnderTest.stop();
        }
    }

    @Before(order = 0)
    public void rememberScenario(Scenario scenario) {
        context.setScenario(scenario);
    }

    @Before(value = "@db", order = 1)
    public void skipWithoutDatabase() {
        if (context.config().db().isEmpty()) {
            throw new TestAbortedException("Skipped: @db scenarios need a database (set DB_HOST, see .env.example)");
        }
    }

    @Before(value = "@ui", order = 10)
    public void startBrowser() {
        context.startBrowser();
    }

    /** Puts the run's session cookie in the browser. The cookie domain must be open first, so it loads a light page. */
    @Before(value = "@authenticated", order = 20)
    public void restoreSession() {
        WebDriver driver = context.driver();
        driver.get(context.baseUrl() + "/login");
        driver.manage()
                .addCookie(new Cookie.Builder("session", AppUnderTest.sessionToken(context.config()))
                        .path("/")
                        .isHttpOnly(true)
                        .sameSite("Lax")
                        .build());
    }

    @After(order = 100)
    public void attachFailureEvidence(Scenario scenario) {
        if (!scenario.isFailed() || !context.hasBrowser()) return;
        WebDriver driver = context.driver();
        try {
            scenario.attach(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES), "image/png", "Screenshot");
            scenario.attach(driver.getCurrentUrl(), "text/uri-list", "URL");
            scenario.attach(driver.getPageSource().getBytes(StandardCharsets.UTF_8), "text/html", "Page source");
        } catch (RuntimeException e) {
            // The browser may be the thing that failed; the scenario's own error matters more
            scenario.log("Could not capture failure evidence: " + e.getMessage());
        }
    }

    @After(order = 50)
    public void runCleanups() {
        context.runCleanups();
    }

    @After(order = 10)
    public void quitBrowser() {
        context.quitBrowser();
    }
}
