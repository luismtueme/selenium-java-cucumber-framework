package io.github.luismtueme.framework.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hands every request and its response to a {@link Sink} as one JSON document, with passwords, tokens and other
 * secrets masked (see {@link Masking}). The Cucumber layer's sink attaches it to the report; the framework itself
 * doesn't know which runner is in use.
 */
public final class ExchangeRecorder implements Filter {

    /** Receives each exchange. {@code name} reads like {@code POST /api/items -> 201}. */
    @FunctionalInterface
    public interface Sink {
        void record(String name, String json);
    }

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Sink sink;

    public ExchangeRecorder(Sink sink) {
        this.sink = sink;
    }

    @Override
    public Response filter(
            FilterableRequestSpecification request, FilterableResponseSpecification spec, FilterContext context) {
        Response response = context.next(request, spec);
        String name = "%s %s -> %d".formatted(request.getMethod(), request.getDerivedPath(), response.statusCode());
        sink.record(name, describe(request, response));
        return response;
    }

    static String describe(FilterableRequestSpecification request, Response response) {
        Object requestBody = request.getBody();
        Map<String, Object> exchange = new LinkedHashMap<>();
        exchange.put(
                "request",
                Map.of(
                        "method", request.getMethod(),
                        "url", request.getURI(),
                        "headers", Masking.headers(asMap(request.getHeaders().asList())),
                        "body", bodyOf(requestBody == null ? null : String.valueOf(requestBody))));
        exchange.put(
                "response",
                Map.of(
                        "status", response.statusCode(),
                        "headers", Masking.headers(asMap(response.getHeaders().asList())),
                        "body", bodyOf(response.asString())));
        try {
            return JSON.writeValueAsString(exchange);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The body as a JSON tree when it parses (so the document nests it), otherwise as text. */
    private static Object bodyOf(String body) {
        if (body == null || body.isEmpty()) return "";
        String masked = Masking.json(body);
        try {
            return JSON.readTree(masked);
        } catch (JsonProcessingException notJson) {
            return masked;
        }
    }

    private static Map<String, String> asMap(Iterable<Header> headers) {
        Map<String, String> map = new LinkedHashMap<>();
        headers.forEach(header -> map.merge(header.getName(), header.getValue(), (a, b) -> a + ", " + b));
        return map;
    }
}
