package io.github.luismtueme.demoapp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** Items kept in memory, lost when the app stops. */
public final class MemoryItemStore implements ItemStore {

    private final Map<Long, Item> items = new ConcurrentSkipListMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public List<Item> list() {
        return List.copyOf(items.values());
    }

    @Override
    public Item create(String name) {
        Item item = new Item(nextId.getAndIncrement(), name, Instant.now().toString());
        items.put(item.id(), item);
        return item;
    }

    @Override
    public Optional<Item> get(long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public boolean remove(long id) {
        return items.remove(id) != null;
    }
}
