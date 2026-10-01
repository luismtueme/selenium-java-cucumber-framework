package io.github.luismtueme.acceptance.support;

import io.github.luismtueme.demoapp.DemoApp;
import io.github.luismtueme.framework.api.ApiClient;
import io.github.luismtueme.framework.config.Config;

/**
 * The application the scenarios run against: BASE_URL, or the demo app started once for the whole run.
 *
 * <p>One demo app serves every parallel scenario; its store and sessions are thread-safe.
 */
public final class AppUnderTest {

    private static DemoApp demoApp;
    private static String baseUrl;
    private static String token;

    private AppUnderTest() {}

    public static synchronized void start(Config config) {
        if (baseUrl != null) return;
        if (config.usesDemoApp()) {
            demoApp = DemoApp.start("127.0.0.1", 0, DemoApp.storeFor(config));
            baseUrl = demoApp.url();
        } else {
            baseUrl = config.baseUrl();
        }
    }

    public static synchronized String baseUrl() {
        if (baseUrl == null)
            throw new IllegalStateException("AppUnderTest.start() hasn't run (it's a @BeforeAll hook)");
        return baseUrl;
    }

    /** The API base URL: API_BASE_URL, or the base URL. */
    public static String apiBaseUrl(Config config) {
        return config.usesDemoApp() || config.apiBaseUrl().isEmpty() ? baseUrl() : config.apiBaseUrl();
    }

    /**
     * A token for the configured user, logged in once per run and shared by every scenario. {@code @authenticated}
     * scenarios put it in the browser as the session cookie, so they skip the login page.
     */
    public static synchronized String sessionToken(Config config) {
        if (token == null) token = new ApiClient(apiBaseUrl(config)).login(config.requireCredentials());
        return token;
    }

    public static synchronized boolean ownsDemoApp() {
        return demoApp != null;
    }

    public static synchronized void stop() {
        if (demoApp != null) demoApp.close();
        demoApp = null;
        baseUrl = null;
        token = null;
    }
}
