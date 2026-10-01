package io.github.luismtueme.framework.config;

/** MySQL connection settings. Present only when DB_HOST is set. */
public record DbConfig(String host, int port, String user, String password, String name) {

    public String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s".formatted(host, port, name);
    }

    @Override
    public String toString() {
        return "DbConfig[%s, user=%s, password=***]".formatted(jdbcUrl(), user);
    }
}
