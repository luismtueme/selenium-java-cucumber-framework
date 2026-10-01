package io.github.luismtueme.framework.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonMatchTest {

    @Test
    void extraFieldsInTheActualDocumentAreAllowed() {
        assertThat(JsonMatch.differences("{\"name\":\"a\"}", "{\"id\":1,\"name\":\"a\",\"createdAt\":\"x\"}"))
                .isEmpty();
    }

    @Test
    void reportsEachDifferenceWithItsPath() {
        assertThat(JsonMatch.differences(
                        "{\"error\":{\"code\":\"NOT_FOUND\",\"field\":\"name\"}}",
                        "{\"error\":{\"code\":\"UNAUTHORIZED\"}}"))
                .containsExactly(
                        "$.error.code: expected \"NOT_FOUND\", got \"UNAUTHORIZED\"",
                        "$.error.field: expected \"name\", but it is missing");
    }

    @Test
    void comparesArraysElementByElement() {
        assertThat(JsonMatch.differences("[{\"a\":1},{\"a\":2}]", "[{\"a\":1,\"b\":0},{\"a\":2}]"))
                .isEmpty();
        assertThat(JsonMatch.differences("[1,2]", "[1,3]")).containsExactly("$[1]: expected 2, got 3");
        assertThat(JsonMatch.differences("[1]", "[1,2]")).containsExactly("$: expected 1 elements, got 2");
    }

    @Test
    void typesMustMatch() {
        assertThat(JsonMatch.differences("{\"id\":1}", "{\"id\":\"1\"}"))
                .containsExactly("$.id: expected 1, got \"1\"");
        assertThat(JsonMatch.differences("{\"a\":{}}", "{\"a\":[]}")).hasSize(1);
    }

    @Test
    void invalidJsonSaysWhichSide() {
        assertThatThrownBy(() -> JsonMatch.differences("{", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The expected value isn't valid JSON");
        assertThatThrownBy(() -> JsonMatch.differences("{}", "<html>"))
                .hasMessageStartingWith("The actual value isn't valid JSON");
    }
}
