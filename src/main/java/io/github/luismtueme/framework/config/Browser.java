package io.github.luismtueme.framework.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Browsers the framework can drive. Selenium Manager downloads the matching driver (and browser, if missing). */
public enum Browser {
    CHROME,
    FIREFOX,
    EDGE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    static Browser fromId(String variable, String value) {
        return Arrays.stream(values())
                .filter(browser -> browser.id().equals(value.trim().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new ConfigException("%s must be one of %s, got \"%s\""
                        .formatted(
                                variable,
                                Arrays.stream(values()).map(Browser::id).collect(Collectors.joining(", ")),
                                value)));
    }
}
