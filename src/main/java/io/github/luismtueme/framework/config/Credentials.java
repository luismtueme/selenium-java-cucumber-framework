package io.github.luismtueme.framework.config;

/** A login for the application under test. {@link #toString()} never prints the password. */
public record Credentials(String username, String password) {

    /** Built into the demo app. Used only when BASE_URL is empty, never against a real application. */
    public static final Credentials DEMO = new Credentials("demo-user", "demo-password");

    @Override
    public String toString() {
        return "Credentials[username=%s, password=***]".formatted(username);
    }
}
