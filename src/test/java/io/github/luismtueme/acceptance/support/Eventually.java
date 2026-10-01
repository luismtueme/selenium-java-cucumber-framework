package io.github.luismtueme.acceptance.support;

import io.github.luismtueme.framework.config.Config;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;

/**
 * Retries an assertion until it passes or the wait timeout (WAIT_TIMEOUT) runs out, then fails with the last
 * assertion error. Use it for anything that changes after an action: text the page updates, rows the app saves
 * asynchronously. Never sleep instead.
 *
 * <pre>
 * Eventually.assertThat(() -> assertThat(loginPage.errorText()).isEqualTo("Invalid username or password"));
 * </pre>
 */
public final class Eventually {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private Eventually() {}

    public static void assertThat(ThrowingRunnable assertion) {
        Awaitility.await()
                .atMost(Config.get().waitTimeout())
                .pollInterval(POLL_INTERVAL)
                .pollInSameThread()
                .untilAsserted(assertion);
    }
}
