package io.github.luismtueme.framework.pages;

import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * The current page's accessible structure, as assistive technology sees it: each visible element inside
 * {@code <main>} with its computed ARIA role and accessible name, in document order. It changes when a label or role
 * changes and ignores styling, so it's a steadier check of what a page contains than a screenshot.
 */
public final class PageStructure {

    /** Roles that only group or wrap content; they're left out of the outline. */
    private static final Set<String> STRUCTURAL_ROLES =
            Set.of("", "generic", "none", "presentation", "paragraph", "LabelText", "StaticText", "group");

    private final WebDriver driver;

    public PageStructure(WebDriver driver) {
        this.driver = driver;
    }

    public List<AccessibleElement> outline() {
        return driver.findElements(By.cssSelector("main *")).stream()
                .filter(WebElement::isDisplayed)
                .map(element -> new AccessibleElement(element.getAriaRole(), element.getAccessibleName()))
                .filter(element -> !STRUCTURAL_ROLES.contains(element.role()))
                .toList();
    }

    /** One entry of the outline, such as {@code button "Log in"}. */
    public record AccessibleElement(String role, String name) {
        @Override
        public String toString() {
            return name.isEmpty() ? role : "%s \"%s\"".formatted(role, name);
        }
    }
}
