package io.github.luismtueme.demoapp;

import io.github.luismtueme.framework.db.DbClient;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Items in the MySQL table {@code items}, created on startup if missing. */
public final class MySqlItemStore implements ItemStore {

    private final DbClient db;

    public MySqlItemStore(DbClient db) {
        this.db = db;
        db.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                )""");
    }

    @Override
    public List<Item> list() {
        return db.query("SELECT * FROM items ORDER BY id").stream()
                .map(MySqlItemStore::toItem)
                .toList();
    }

    @Override
    public Item create(String name) {
        long id = db.insert("INSERT INTO items (name) VALUES (?)", name);
        return get(id).orElseThrow(() -> new IllegalStateException("Item " + id + " was not saved"));
    }

    @Override
    public Optional<Item> get(long id) {
        return db.one("SELECT * FROM items WHERE id = ?", id).map(MySqlItemStore::toItem);
    }

    @Override
    public boolean remove(long id) {
        return db.execute("DELETE FROM items WHERE id = ?", id) > 0;
    }

    private static Item toItem(Map<String, Object> row) {
        Object createdAt = row.get("created_at");
        String instant =
                createdAt instanceof Timestamp timestamp ? timestamp.toInstant().toString() : String.valueOf(createdAt);
        return new Item(((Number) row.get("id")).longValue(), (String) row.get("name"), instant);
    }
}
