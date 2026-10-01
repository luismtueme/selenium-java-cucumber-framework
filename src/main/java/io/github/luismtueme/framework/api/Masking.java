package io.github.luismtueme.framework.api;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Hides secrets before requests and responses reach the report. */
public final class Masking {

    public static final String MASK = "***";

    /** Compared lowercase, with "-" and "_" removed, so "api_key", "apiKey" and "X-Api-Key" all match "apikey". */
    private static final Set<String> SECRET_NAMES = Set.of(
            "password",
            "token",
            "accesstoken",
            "refreshtoken",
            "authorization",
            "apikey",
            "xapikey",
            "secret",
            "cookie",
            "setcookie");

    private static final ObjectMapper JSON = new ObjectMapper();

    private Masking() {}

    public static boolean isSecret(String name) {
        return SECRET_NAMES.contains(name.toLowerCase(Locale.ROOT).replaceAll("[-_]", ""));
    }

    /** Header values with secrets replaced. Keeps the order. */
    public static Map<String, String> headers(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((name, value) -> masked.put(name, isSecret(name) ? MASK : value));
        return masked;
    }

    /** A JSON document with every secret field masked, at any depth. Bodies that aren't JSON are returned unchanged. */
    public static String json(String body) {
        if (body == null || body.isBlank()) return body;
        try {
            JsonNode tree = JSON.readTree(body);
            mask(tree);
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (JacksonException notJson) {
            return body;
        }
    }

    private static void mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (Map.Entry<String, JsonNode> field : object.properties()) {
                if (isSecret(field.getKey()) && field.getValue().isValueNode()) field.setValue(object.textNode(MASK));
                else mask(field.getValue());
            }
        } else if (node.isArray()) {
            node.forEach(Masking::mask);
        }
    }
}
