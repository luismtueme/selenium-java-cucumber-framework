package io.github.luismtueme.acceptance.support;

import io.cucumber.java.Scenario;
import io.github.luismtueme.framework.api.ApiClient;
import io.github.luismtueme.framework.browser.DriverFactory;
import io.github.luismtueme.framework.config.Config;
import io.github.luismtueme.framework.db.DbClient;
import io.github.luismtueme.framework.pages.FormPage;
import io.github.luismtueme.framework.pages.ItemsPage;
import io.github.luismtueme.framework.pages.LoginPage;
import io.restassured.response.Response;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.openqa.selenium.WebDriver;

/**
 * Everything one scenario shares between its steps and hooks: the browser, page objects, API clients, the last API
 * response and the cleanups to run afterwards.
 *
 * <p>PicoContainer creates a new instance per scenario and injects it into every step class and {@code Hooks} through
 * their constructors, so parallel scenarios never share state.
 */
public class TestContext {

    private final Config config = Config.get();
    private final Deque<Cleanup> cleanups = new ArrayDeque<>();
    private WebDriver driver;
    private FormPage formPage;
    private LoginPage loginPage;
    private ItemsPage itemsPage;
    private ApiClient api;
    private Response lastResponse;
    private Long createdItemId;
    private Scenario scenario;

    public Config config() {
        return config;
    }

    /** Set by a hook before every scenario, so steps and clients can attach evidence to the report. */
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /** Adds text to the scenario in the Cucumber report (no-op outside a scenario). */
    public void attach(String content, String mediaType, String name) {
        if (scenario != null) scenario.attach(content, mediaType, name);
    }

    public String baseUrl() {
        return AppUnderTest.baseUrl();
    }

    // --- Browser ---------------------------------------------------------------------------------------------------

    /** Started by the {@code @ui} hook. */
    public void startBrowser() {
        driver = DriverFactory.create(config);
    }

    public boolean hasBrowser() {
        return driver != null;
    }

    public WebDriver driver() {
        if (driver == null)
            throw new IllegalStateException("No browser in this scenario. Tag the feature or scenario @ui.");
        return driver;
    }

    public void quitBrowser() {
        if (driver != null) driver.quit();
        driver = null;
    }

    public FormPage formPage() {
        if (formPage == null) formPage = new FormPage(driver(), baseUrl(), config.waitTimeout());
        return formPage;
    }

    public LoginPage loginPage() {
        if (loginPage == null) loginPage = new LoginPage(driver(), baseUrl(), config.waitTimeout());
        return loginPage;
    }

    public ItemsPage itemsPage() {
        if (itemsPage == null) itemsPage = new ItemsPage(driver(), baseUrl(), config.waitTimeout());
        return itemsPage;
    }

    // --- API and database ------------------------------------------------------------------------------------------

    /** The client the API steps use: anonymous until a step logs in with {@link #useAuthenticatedApi()}. */
    public ApiClient api() {
        if (api == null) api = anonymousApi();
        return api;
    }

    public void useAuthenticatedApi() {
        api = authenticatedApi();
    }

    public ApiClient anonymousApi() {
        // Every request and response is attached to the report, with secrets masked
        return new ApiClient(AppUnderTest.apiBaseUrl(config))
                .recordingTo((name, json) -> attach(json, "application/json", name));
    }

    /** A client logged in as the configured user. Never carries the browser session. */
    public ApiClient authenticatedApi() {
        return anonymousApi().withToken(AppUnderTest.sessionToken(config));
    }

    public DbClient db() {
        return new DbClient(config.db().orElseThrow(() -> new IllegalStateException("DB_HOST is not set")));
    }

    public Response lastResponse() {
        if (lastResponse == null) throw new IllegalStateException("No API request has been sent in this scenario");
        return lastResponse;
    }

    public void setLastResponse(Response response) {
        lastResponse = response;
    }

    public long createdItemId() {
        if (createdItemId == null) throw new IllegalStateException("No item has been created in this scenario");
        return createdItemId;
    }

    public void setCreatedItemId(long id) {
        createdItemId = id;
    }

    // --- Cleanup ---------------------------------------------------------------------------------------------------

    /** Runs after the scenario, pass or fail, in reverse order of registration. */
    public void addCleanup(String description, Runnable action) {
        cleanups.push(new Cleanup(description, action));
    }

    /** Deletes the item through the API after the scenario. An item the scenario already deleted is fine. */
    public void cleanUpItem(long id) {
        addCleanup("delete item " + id, () -> {
            int status = authenticatedApi().delete("/api/items/" + id).statusCode();
            if (status != 204 && status != 404) throw new IllegalStateException("HTTP " + status);
        });
    }

    /** Runs every cleanup, even when one fails, then reports all failures together. */
    public void runCleanups() {
        List<String> failures = new ArrayList<>();
        while (!cleanups.isEmpty()) {
            Cleanup cleanup = cleanups.pop();
            try {
                cleanup.action().run();
            } catch (RuntimeException e) {
                failures.add(cleanup.description() + ": " + e.getMessage());
            }
        }
        if (!failures.isEmpty()) throw new IllegalStateException("Cleanup failed: " + String.join("; ", failures));
    }

    private record Cleanup(String description, Runnable action) {}
}
