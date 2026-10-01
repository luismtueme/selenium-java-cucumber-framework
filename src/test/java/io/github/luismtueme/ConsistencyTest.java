package io.github.luismtueme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Versions that are written in more than one place stay in step. */
class ConsistencyTest {

    private static String read(String file) throws IOException {
        return Files.readString(Path.of(file));
    }

    private static String javaVersion() throws IOException {
        Matcher matcher = Pattern.compile("<java.version>(\\d+)</java.version>").matcher(read("pom.xml"));
        assertThat(matcher.find()).as("<java.version> in pom.xml").isTrue();
        return matcher.group(1);
    }

    @Test
    void theDockerfileUsesTheProjectsJavaVersion() throws IOException {
        assertThat(read("Dockerfile"))
                .as("Dockerfile FROM line. Use: FROM maven:<maven version>-eclipse-temurin-%s", javaVersion())
                .containsPattern("(?m)^FROM maven:[\\d.]+-eclipse-temurin-" + javaVersion() + "\\b");
    }

    @Test
    void everyWorkflowUsesTheProjectsJavaVersion() throws IOException {
        List<Path> workflows;
        try (Stream<Path> files = Files.list(Path.of(".github/workflows"))) {
            workflows = files.filter(file -> file.toString().endsWith(".yml")).toList();
        }
        assertThat(workflows).isNotEmpty();
        for (Path workflow : workflows) {
            String text = Files.readString(workflow);
            Matcher versions = Pattern.compile("JAVA_VERSION: '?(\\d+)'?").matcher(text);
            assertThat(versions.find()).as("JAVA_VERSION in %s", workflow).isTrue();
            assertThat(versions.group(1)).as("JAVA_VERSION in %s", workflow).isEqualTo(javaVersion());
        }
    }

    @Test
    void theWrapperUsesASupportedMaven() throws IOException {
        assertThat(read(".mvn/wrapper/maven-wrapper.properties"))
                .containsPattern("apache-maven-3\\.9\\.\\d+-bin\\.zip");
    }
}
