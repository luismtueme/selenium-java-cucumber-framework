package io.github.luismtueme.framework.a11y;

import static org.assertj.core.api.Assertions.assertThat;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import java.util.List;
import org.junit.jupiter.api.Test;

class AxeViolationsTest {

    private static CheckedNode node(String selector) {
        CheckedNode node = new CheckedNode();
        node.setTarget(List.of(selector));
        return node;
    }

    private static Rule rule(String id, String impact, List<CheckedNode> nodes) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setImpact(impact);
        rule.setHelp("Help for " + id);
        rule.setHelpUrl("https://dequeuniversity.com/rules/axe/" + id);
        rule.setNodes(nodes);
        return rule;
    }

    @Test
    void listsEachRuleWithItsElementsAndTheFix() {
        String report = AxeViolations.describe(
                "http://app/login",
                List.of(
                        rule("label", "serious", List.of(node("#username"), node("#password"))),
                        rule("image-alt", "critical", List.of(node("img")))));

        assertThat(report)
                .startsWith("2 accessibility violations on http://app/login\n\n")
                .contains("[serious] label: Help for label (2 elements)")
                .contains("#username", "#password")
                .contains("[critical] image-alt: Help for image-alt (1 element)")
                .contains("Fix: https://dequeuniversity.com/rules/axe/label");
    }

    @Test
    void handlesASingleViolationWithoutNodes() {
        assertThat(AxeViolations.describe("http://app/", List.of(rule("region", "moderate", null))))
                .startsWith("1 accessibility violation on http://app/")
                .contains("(0 elements)");
    }
}
