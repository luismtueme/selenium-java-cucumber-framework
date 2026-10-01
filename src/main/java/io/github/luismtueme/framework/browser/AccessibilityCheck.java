package io.github.luismtueme.framework.browser;

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.selenium.AxeBuilder;
import java.util.List;
import org.openqa.selenium.WebDriver;

/** Runs axe-core on the current page against WCAG 2.1 A and AA. See {@code AxeViolations} for the report. */
public final class AccessibilityCheck {

    public static final List<String> WCAG_21_AA = List.of("wcag2a", "wcag2aa", "wcag21a", "wcag21aa");

    private AccessibilityCheck() {}

    /**
     * @param exclude CSS selectors to skip, for content you don't control (a third-party widget)
     * @param disableRules axe rule ids to skip; say why in a comment where you call this
     */
    public static Results analyze(WebDriver driver, List<String> exclude, List<String> disableRules) {
        AxeBuilder axe = new AxeBuilder().withTags(WCAG_21_AA);
        if (!exclude.isEmpty()) axe.exclude(exclude);
        if (!disableRules.isEmpty()) axe.disableRules(disableRules);
        return axe.analyze(driver);
    }
}
