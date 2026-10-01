package io.github.luismtueme.framework.a11y;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import java.util.List;
import java.util.stream.Collectors;

/** Turns axe-core violations into a message a person can act on: what's wrong, where, and how to fix it. */
public final class AxeViolations {

    private AxeViolations() {}

    /**
     * @return for example:
     *     <pre>
     * 1 accessibility violation on http://127.0.0.1:4173/login
     *
     * [serious] label: Form elements must have labels (2 elements)
     *   #username
     *   #password
     *   Fix: https://dequeuniversity.com/rules/axe/4.11/label
     * </pre>
     */
    public static String describe(String url, List<Rule> violations) {
        String header = "%d accessibility violation%s on %s"
                .formatted(violations.size(), violations.size() == 1 ? "" : "s", url);
        return violations.stream()
                .map(AxeViolations::describe)
                .collect(Collectors.joining("\n\n", header + "\n\n", ""));
    }

    private static String describe(Rule rule) {
        List<CheckedNode> nodes = rule.getNodes() == null ? List.of() : rule.getNodes();
        String targets = nodes.stream().map(node -> "  " + node.getTarget()).collect(Collectors.joining("\n"));
        return "[%s] %s: %s (%d element%s)%n%s%n  Fix: %s"
                .formatted(
                        rule.getImpact(),
                        rule.getId(),
                        rule.getHelp(),
                        nodes.size(),
                        nodes.size() == 1 ? "" : "s",
                        targets,
                        rule.getHelpUrl());
    }
}
