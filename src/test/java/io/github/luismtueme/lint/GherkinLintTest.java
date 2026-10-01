package io.github.luismtueme.lint;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luismtueme.lint.GherkinLint.Problem;
import java.util.List;
import org.junit.jupiter.api.Test;

class GherkinLintTest {

    private static List<String> rules(String source) {
        return GherkinLint.lint("test.feature", source).stream()
                .map(Problem::rule)
                .toList();
    }

    @Test
    void acceptsAFeatureThatFollowsTheConventions() {
        assertThat(GherkinLint.lint("ok.feature", """
                @ui @Regression
                Feature: Fine
                  Scenario: Checks something
                    Given a step
                    Then a check
                """)).isEmpty();
    }

    @Test
    void reportsInvalidGherkinWithItsLine() {
        List<Problem> problems = GherkinLint.lint("bad.feature", """
                Feature: Broken
                  Scenario: One
                    Given a step
                  This line is not Gherkin
                """);
        assertThat(problems).singleElement().satisfies(problem -> {
            assertThat(problem.rule()).isEqualTo("parse-error");
            assertThat(problem.line()).isEqualTo(4);
        });
    }

    @Test
    void rejectsUnknownTagsOnFeaturesScenariosAndExamples() {
        assertThat(rules("""
                @smoke
                Feature: Tags
                  @wip
                  Scenario Outline: <x>
                    Then <x>

                    @nightly
                    Examples:
                      | x |
                      | 1 |
                """)).containsExactly("unknown-tag", "unknown-tag", "unknown-tag");
    }

    @Test
    void acceptsIssueTags() {
        assertThat(rules("""
                Feature: Issues
                  @jira:QA-123
                  Scenario: Linked
                    Then a check
                """)).isEmpty();
    }

    @Test
    void requiresATicketToQuarantine() {
        assertThat(rules("""
                Feature: Flaky
                  @quarantine
                  Scenario: Unexplained
                    Then a check

                  @quarantine @jira:QA-7
                  Scenario: Explained
                    Then a check
                """)).containsExactly("quarantine-ticket");
    }

    @Test
    void requiresScenarios() {
        assertThat(rules("Feature: Empty\n")).containsExactly("no-scenarios");
    }

    @Test
    void scenarioNamesAreUniqueWithinAFeature() {
        List<Problem> problems = GherkinLint.lint("dup.feature", """
                Feature: Duplicates
                  Scenario: Same
                    Then a check
                  Scenario: Same
                    Then a check
                """);
        assertThat(problems).singleElement().satisfies(problem -> {
            assertThat(problem.rule()).isEqualTo("duplicate-name");
            assertThat(problem.message()).contains("line 2");
        });
    }

    @Test
    void everyScenarioChecksSomething() {
        assertThat(rules("""
                Feature: No checks
                  Scenario: Only actions
                    Given a step
                    When something happens
                    And something else
                """)).containsExactly("missing-then");
    }

    @Test
    void outlinesNeedRowsAndAColumnForEveryPlaceholder() {
        assertThat(rules("""
                Feature: Outlines
                  Scenario Outline: No rows
                    Then <value>

                    Examples:
                      | value |

                  Scenario Outline: Missing column for <case>
                    Then <value> shows
                      \"\"\"
                      <doc>
                      \"\"\"

                    Examples:
                      | case | value |
                      | a    | b     |
                """)).containsExactly("outline-examples", "outline-examples");
    }

    @Test
    void scenariosInsideRulesAreChecked() {
        assertThat(rules("""
                Feature: Rules
                  Rule: A business rule
                    Scenario: Without a check
                      Given a step
                """)).containsExactly("missing-then");
    }

    @Test
    void problemsPrintAsFileLineMessageAndRule() {
        assertThat(new Problem("a.feature", 3, "missing-then", "No Then").toString())
                .isEqualTo("a.feature:3  No Then  [missing-then]");
    }
}
