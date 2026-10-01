package io.github.luismtueme.framework.db;

import io.github.luismtueme.framework.config.DbConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small JDBC client for MySQL. Parameterized queries only: values are always bound with {@code ?}, never concatenated
 * into SQL. Each call uses its own connection, so one client is safe to share between parallel scenarios.
 */
public final class DbClient {

    private final DbConfig config;

    public DbClient(DbConfig config) {
        this.config = config;
    }

    /** Rows as column label to value maps, in result order. */
    public List<Map<String, Object>> query(String sql, Object... params) {
        try (Connection connection = connect();
                PreparedStatement statement = prepare(connection, sql, params, Statement.NO_GENERATED_KEYS);
                ResultSet rows = statement.executeQuery()) {
            ResultSetMetaData meta = rows.getMetaData();
            List<Map<String, Object>> result = new ArrayList<>();
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int column = 1; column <= meta.getColumnCount(); column++) {
                    row.put(meta.getColumnLabel(column), rows.getObject(column));
                }
                result.add(row);
            }
            return result;
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /** The first row, if any. */
    public Optional<Map<String, Object>> one(String sql, Object... params) {
        return query(sql, params).stream().findFirst();
    }

    /** Runs an UPDATE, DELETE or DDL statement and returns the number of affected rows. */
    public int execute(String sql, Object... params) {
        try (Connection connection = connect();
                PreparedStatement statement = prepare(connection, sql, params, Statement.NO_GENERATED_KEYS)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /** Runs an INSERT and returns the generated key. */
    public long insert(String sql, Object... params) {
        try (Connection connection = connect();
                PreparedStatement statement = prepare(connection, sql, params, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("No generated key for: " + sql);
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.user(), config.password());
    }

    private static PreparedStatement prepare(Connection connection, String sql, Object[] params, int keys)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql, keys);
        for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
        return statement;
    }

    private IllegalStateException failure(String sql, SQLException e) {
        return new IllegalStateException("Query failed on %s: %s%n  %s".formatted(config, e.getMessage(), sql), e);
    }
}
