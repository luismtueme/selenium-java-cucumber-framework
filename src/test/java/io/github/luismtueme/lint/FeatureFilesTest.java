package io.github.luismtueme.lint;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luismtueme.lint.GherkinLint.Problem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every feature file in the project follows the Gherkin conventions (see {@link GherkinLint}). */
class FeatureFilesTest {

    private static final Path FEATURES = Path.of("src/test/resources/features");

    @Test
    void featureFilesFollowTheConventions() throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.walk(FEATURES)) {
            files = paths.filter(path -> path.toString().endsWith(".feature"))
                    .sorted()
                    .toList();
        }
        assertThat(files).as("feature files in " + FEATURES).isNotEmpty();

        List<Problem> problems = new ArrayList<>();
        for (Path file : files) {
            problems.addAll(GherkinLint.lint(file.toString().replace('\\', '/'), Files.readString(file)));
        }
        assertThat(problems)
                .as(
                        "Gherkin lint problems:%n%s",
                        String.join(
                                "\n", problems.stream().map(Problem::toString).toList()))
                .isEmpty();
    }
}
