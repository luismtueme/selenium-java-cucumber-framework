package io.github.luismtueme.framework.config;

/** An invalid or missing setting. The message names the variable to fix. */
public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }
}
