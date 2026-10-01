package io.github.luismtueme.framework.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.module.Script;

/**
 * Puts the page's clock under test control, so time-dependent behavior (idle timeouts, countdowns) is tested without
 * waiting in real time and boundaries are exact.
 *
 * <p>{@link #install} registers a WebDriver BiDi preload script that replaces {@code Date}, {@code setTimeout} and
 * {@code setInterval} before any page script runs. Time then stands still until {@link #fastForward} moves it, firing
 * every timer that falls due on the way. Works in Chrome, Edge and Firefox, locally and on a Grid.
 */
public final class BrowserClock {

    private static final String SCRIPT = readScript();

    private BrowserClock() {}

    /** Call before opening the page: the clock applies to every page loaded afterwards. */
    public static void install(WebDriver driver) {
        // Closing the module only drops its event listeners; the preload script stays registered
        try (Script script = new Script(driver)) {
            script.addPreloadScript(SCRIPT);
        }
    }

    /** Moves the page's clock forward, running the timers that fall due in order. */
    public static void fastForward(WebDriver driver, Duration duration) {
        Object installed = ((JavascriptExecutor) driver)
                .executeScript(
                        "if (!window.__testClock) return false; window.__testClock.tick(arguments[0]); return true;",
                        duration.toMillis());
        if (!Boolean.TRUE.equals(installed)) {
            throw new IllegalStateException(
                    "The browser clock isn't installed on this page. Call BrowserClock.install() before opening it.");
        }
    }

    private static String readScript() {
        try (InputStream in = BrowserClock.class.getResourceAsStream("/framework/fake-clock.js")) {
            if (in == null) throw new IllegalStateException("framework/fake-clock.js is missing from the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
