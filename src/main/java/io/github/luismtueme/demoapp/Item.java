package io.github.luismtueme.demoapp;

/** An item as the demo API returns it. {@code createdAt} is an ISO-8601 instant. */
public record Item(long id, String name, String createdAt) {}
