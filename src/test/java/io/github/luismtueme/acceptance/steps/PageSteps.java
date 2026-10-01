package io.github.luismtueme.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.results.Results;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.luismtueme.acceptance.support.TestContext;
import io.github.luismtueme.framework.a11y.AxeViolations;
import io.github.luismtueme.framework.browser.AccessibilityCheck;
import io.github.luismtueme.framework.pages.BasePage;
import io.github.luismtueme.framework.pages.PageStructure;
import io.github.luismtueme.framework.pages.PageStructure.AccessibleElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Steps that work on any page: opening a page by name, its accessible structure, accessibility checks. */
public class PageSteps {

    private final TestContext context;

    public PageSteps(TestContext context) {
        this.context = context;
    }

    /** Rows of a {@code | role | name |} table. Cucumber passes an empty cell as null; here it means "no name". */
    @DataTableType
    public AccessibleElement accessibleElement(Map<String, String> row) {
        String name = row.get("name");
        return new AccessibleElement(row.get("role"), name == null ? "" : name);
    }

    @Given("I open the {word} page")
    public void iOpenThePage(String name) {
        Map<String, BasePage> pages =
                Map.of("form", context.formPage(), "login", context.loginPage(), "items", context.itemsPage());
        assertThat(pages).as("known pages").containsKey(name);
        pages.get(name).open();
    }

    /**
     * The listed elements must appear inside {@code <main>} in this order, each with this ARIA role and accessible
     * name. Other elements in between are allowed, so the table lists only what the scenario cares about.
     */
    @Then("the page shows, in order:")
    public void thePageShowsInOrder(List<AccessibleElement> expected) {
        List<AccessibleElement> actual = new PageStructure(context.driver()).outline();
        int found = 0;
        for (AccessibleElement element : actual) {
            if (found < expected.size() && element.equals(expected.get(found))) found++;
        }
        assertThat(found)
                .as(
                        "%s is missing or out of order. The page shows:%n  %s",
                        found < expected.size() ? expected.get(found) : "",
                        actual.stream().map(Object::toString).collect(Collectors.joining("\n  ")))
                .isEqualTo(expected.size());
    }

    @Then("the page has no accessibility violations")
    public void thePageHasNoAccessibilityViolations() {
        Results results = AccessibilityCheck.analyze(context.driver(), List.of(), List.of());
        if (!results.violationFree()) {
            String report = AxeViolations.describe(results.getUrl(), results.getViolations());
            context.attach(report, "text/plain", "Accessibility violations");
            assertThat(results.getViolations()).as(report).isEmpty();
        }
    }
}
