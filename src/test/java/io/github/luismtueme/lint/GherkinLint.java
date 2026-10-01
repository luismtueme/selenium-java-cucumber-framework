package io.github.luismtueme.lint;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.ParseError;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepKeywordType;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.Tag;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks feature files against this framework's conventions, using Cucumber's own Gherkin parser. Runs as a unit test
 * ({@code FeatureFilesTest}), so a broken convention fails the build like any other test.
 *
 * <ul>
 *   <li>{@code parse-error}: the file must be valid Gherkin
 *   <li>{@code unknown-tag}: tags must be in {@link #ALLOWED_TAGS} or a ticket ({@code @jira:QA-123})
 *   <li>{@code quarantine-ticket}: {@code @quarantine} needs an {@code @jira:QA-123} tag saying why
 *   <li>{@code no-scenarios}: a feature must contain at least one scenario
 *   <li>{@code duplicate-name}: scenario names must be unique within a feature
 *   <li>{@code missing-then}: every scenario needs a Then step (it must check something)
 *   <li>{@code outline-examples}: outlines need Examples rows, and every {@code <placeholder>} needs a column
 * </ul>
 */
public final class GherkinLint {

    /** Add new tags here, so they're documented in one place. */
    public static final Set<String> ALLOWED_TAGS =
            Set.of("@Smoke", "@Regression", "@ui", "@api", "@db", "@authenticated", "@a11y", "@quarantine");

    /** A ticket reference, such as the one explaining a quarantine. */
    public static final Pattern JIRA_TAG = Pattern.compile("^@jira:[A-Z][A-Z0-9]*-\\d+$");

    private static final Pattern PLACEHOLDER = Pattern.compile("<[^<>\\s]+>");

    public record Problem(String file, int line, String rule, String message) {
        @Override
        public String toString() {
            return "%s:%d  %s  [%s]".formatted(file, line, message, rule);
        }
    }

    private GherkinLint() {}

    public static List<Problem> lint(String file, String source) {
        List<Problem> problems = new ArrayList<>();
        List<Envelope> envelopes = GherkinParser.builder()
                .includeSource(false)
                .includePickles(false)
                .build()
                .parse(file, source.getBytes(StandardCharsets.UTF_8))
                .toList();

        for (Envelope envelope : envelopes) {
            Optional<ParseError> error = envelope.getParseError();
            if (error.isPresent()) {
                int line = error.get()
                        .getSource()
                        .getLocation()
                        .map(Location::getLine)
                        .orElse(1);
                problems.add(new Problem(
                        file,
                        line,
                        "parse-error",
                        error.get().getMessage().lines().findFirst().orElse("")));
            }
        }
        if (!problems.isEmpty()) return problems;

        envelopes.stream()
                .flatMap(envelope -> envelope.getGherkinDocument().flatMap(doc -> doc.getFeature()).stream())
                .forEach(feature -> lintFeature(file, feature, problems));
        return problems;
    }

    private static void lintFeature(String file, Feature feature, List<Problem> problems) {
        checkTags(file, feature.getTags(), problems);

        List<Scenario> scenarios =
                feature.getChildren().stream().flatMap(GherkinLint::scenarios).toList();
        if (scenarios.isEmpty()) {
            problems.add(new Problem(
                    file,
                    line(feature.getLocation()),
                    "no-scenarios",
                    "Feature \"%s\" has no scenarios".formatted(feature.getName())));
        }

        Map<String, Integer> seen = new HashMap<>();
        for (Scenario scenario : scenarios) {
            int line = line(scenario.getLocation());
            checkTags(file, scenario.getTags(), problems);
            scenario.getExamples().forEach(examples -> checkTags(file, examples.getTags(), problems));

            Integer firstUse = seen.putIfAbsent(scenario.getName(), line);
            if (firstUse != null) {
                problems.add(new Problem(
                        file,
                        line,
                        "duplicate-name",
                        "Scenario name \"%s\" is also used on line %d".formatted(scenario.getName(), firstUse)));
            }

            boolean checksSomething = scenario.getSteps().stream()
                    .anyMatch(step -> step.getKeywordType().orElse(null) == StepKeywordType.OUTCOME);
            if (!checksSomething) {
                problems.add(new Problem(
                        file,
                        line,
                        "missing-then",
                        "Scenario \"%s\" has no Then step, so it doesn't check anything"
                                .formatted(scenario.getName())));
            }

            List<String> tags = Stream.of(
                            feature.getTags().stream(),
                            scenario.getTags().stream(),
                            scenario.getExamples().stream().flatMap(examples -> examples.getTags().stream()))
                    .flatMap(stream -> stream)
                    .map(Tag::getName)
                    .toList();
            if (tags.contains("@quarantine")
                    && tags.stream().noneMatch(tag -> JIRA_TAG.matcher(tag).matches())) {
                problems.add(new Problem(
                        file,
                        line,
                        "quarantine-ticket",
                        "Quarantined scenario \"%s\" needs an @jira:ABC-123 tag".formatted(scenario.getName())));
            }

            checkOutline(file, scenario, problems);
        }
    }

    private static void checkOutline(String file, Scenario scenario, List<Problem> problems) {
        Set<String> placeholders = new LinkedHashSet<>();
        Stream.concat(
                        Stream.of(scenario.getName()),
                        scenario.getSteps().stream().map(GherkinLint::textWithArguments))
                .forEach(text -> {
                    Matcher matcher = PLACEHOLDER.matcher(text);
                    while (matcher.find()) placeholders.add(matcher.group());
                });
        if (scenario.getExamples().isEmpty() && placeholders.isEmpty()) return;

        int rows = scenario.getExamples().stream()
                .mapToInt(examples -> examples.getTableBody().size())
                .sum();
        if (rows == 0) {
            problems.add(new Problem(
                    file,
                    line(scenario.getLocation()),
                    "outline-examples",
                    "Scenario Outline \"%s\" has no Examples rows".formatted(scenario.getName())));
        }
        for (Examples examples : scenario.getExamples()) {
            Set<String> columns = new LinkedHashSet<>();
            examples.getTableHeader()
                    .ifPresent(header ->
                            header.getCells().stream().map(TableCell::getValue).forEach(columns::add));
            for (String placeholder : placeholders) {
                String column = placeholder.substring(1, placeholder.length() - 1);
                if (!columns.contains(column)) {
                    problems.add(new Problem(
                            file,
                            line(examples.getLocation()),
                            "outline-examples",
                            "Examples table has no \"%s\" column for %s".formatted(column, placeholder)));
                }
            }
        }
    }

    private static String textWithArguments(Step step) {
        StringBuilder text = new StringBuilder(step.getText());
        step.getDocString().ifPresent(doc -> text.append('\n').append(doc.getContent()));
        step.getDataTable()
                .ifPresent(table -> table.getRows()
                        .forEach(row ->
                                row.getCells().forEach(cell -> text.append(' ').append(cell.getValue()))));
        return text.toString();
    }

    private static void checkTags(String file, List<Tag> tags, List<Problem> problems) {
        for (Tag tag : tags) {
            if (!ALLOWED_TAGS.contains(tag.getName())
                    && !JIRA_TAG.matcher(tag.getName()).matches()) {
                problems.add(new Problem(
                        file,
                        line(tag.getLocation()),
                        "unknown-tag",
                        "Unknown tag %s. Allowed: %s, @jira:ABC-123 (see GherkinLint.ALLOWED_TAGS)"
                                .formatted(
                                        tag.getName(),
                                        String.join(
                                                ", ",
                                                ALLOWED_TAGS.stream().sorted().toList()))));
            }
        }
    }

    private static Stream<Scenario> scenarios(FeatureChild child) {
        if (child.getScenario().isPresent()) return child.getScenario().stream();
        return child.getRule().stream()
                .flatMap(rule -> rule.getChildren().stream())
                .map(RuleChild::getScenario)
                .flatMap(Optional::stream);
    }

    private static int line(Location location) {
        return location.getLine();
    }
}
