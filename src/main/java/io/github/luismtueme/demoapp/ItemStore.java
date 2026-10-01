package io.github.luismtueme.demoapp;

import java.util.List;
import java.util.Optional;

/** Where the demo app keeps items: in memory, or in MySQL when DB_HOST is set. Implementations are thread-safe. */
public interface ItemStore {

    List<Item> list();

    Item create(String name);

    Optional<Item> get(long id);

    /** @return false when there was no such item */
    boolean remove(long id);
}
