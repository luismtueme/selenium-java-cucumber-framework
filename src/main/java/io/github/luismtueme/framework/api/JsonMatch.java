package io.github.luismtueme.framework.api;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Partial JSON comparison: the actual document must contain everything in the expected one. Extra fields in actual
 * objects are allowed (ids, timestamps); arrays must have the same length and match element by element.
 */
public final class JsonMatch {

    private static final ObjectMapper JSON = new ObjectMapper();

    private JsonMatch() {}

    /**
     * @return one line per difference, like {@code $.error.code: expected "NOT_FOUND", got "UNAUTHORIZED"}; empty when
     *     actual contains expected
     * @throws IllegalArgumentException when either document isn't valid JSON
     */
    public static List<String> differences(String expectedJson, String actualJson) {
        List<String> differences = new ArrayList<>();
        compare("$", parse("expected", expectedJson), parse("actual", actualJson), differences);
        return differences;
    }

    private static JsonNode parse(String which, String json) {
        try {
            return JSON.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "The %s value isn't valid JSON: %s".formatted(which, e.getOriginalMessage()), e);
        }
    }

    private static void compare(String path, JsonNode expected, JsonNode actual, List<String> differences) {
        if (actual == null || actual.isMissingNode()) {
            differences.add("%s: expected %s, but it is missing".formatted(path, expected));
        } else if (expected.isObject() && actual.isObject()) {
            for (Map.Entry<String, JsonNode> field : expected.properties()) {
                compare(path + "." + field.getKey(), field.getValue(), actual.get(field.getKey()), differences);
            }
        } else if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) {
                differences.add("%s: expected %d elements, got %d".formatted(path, expected.size(), actual.size()));
                return;
            }
            for (int i = 0; i < expected.size(); i++) {
                compare("%s[%d]".formatted(path, i), expected.get(i), actual.get(i), differences);
            }
        } else if (!expected.equals(actual)) {
            differences.add("%s: expected %s, got %s".formatted(path, expected, actual));
        }
    }
}
