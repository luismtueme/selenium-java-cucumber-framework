package io.github.luismtueme.framework.api;

import static io.restassured.RestAssured.given;

import io.github.luismtueme.framework.config.Credentials;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/**
 * HTTP client for the application's JSON API, built on REST Assured.
 *
 * <p>Clients are immutable: {@link #withToken} returns a new client that sends {@code Authorization: Bearer}, and
 * {@link #recordingTo} one that hands every exchange, with secrets masked, to a sink (the Cucumber layer attaches them
 * to the report). A client never shares cookies with the browser, so API calls can't hide UI session bugs.
 */
public final class ApiClient {

    private static final ExchangeRecorder.Sink DISCARD = (name, json) -> {};

    private final String baseUrl;
    private final String token;
    private final ExchangeRecorder.Sink sink;

    public ApiClient(String baseUrl) {
        this(baseUrl, null, DISCARD);
    }

    private ApiClient(String baseUrl, String token, ExchangeRecorder.Sink sink) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.sink = sink;
    }

    public ApiClient withToken(String token) {
        return new ApiClient(baseUrl, token, sink);
    }

    public ApiClient recordingTo(ExchangeRecorder.Sink sink) {
        return new ApiClient(baseUrl, token, sink);
    }

    /** Logs in through {@code POST /api/login} and returns the session token. */
    public String login(Credentials credentials) {
        Response response =
                post("/api/login", Map.of("username", credentials.username(), "password", credentials.password()));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Login as %s failed with HTTP %d: %s"
                    .formatted(credentials.username(), response.statusCode(), response.asString()));
        }
        return response.jsonPath().getString("token");
    }

    public Response get(String path) {
        return request().get(path);
    }

    /** Sends {@code body} as JSON (a Map, a record or any Jackson-serializable object). */
    public Response post(String path, Object body) {
        return request().body(body).post(path);
    }

    public Response delete(String path) {
        return request().delete(path);
    }

    private RequestSpecification request() {
        RequestSpecification request = given().baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new ExchangeRecorder(sink));
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }
}
