package cn.github.spinner.util;

import cn.github.driver.MQLException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class TriggerQueryCache {
    private static final String QUERY_FORMAT_VERSION = "2";

    private TriggerQueryCache() {
    }

    public static @NotNull CachedTriggerQueryResults get(@NotNull Project project,
                                                         @NotNull TriggerQueryUtil.SchemaType schemaType,
                                                         @NotNull String name,
                                                         @Nullable String stateFilter) {
        return get(project, schemaType, name, stateFilter, true);
    }

    public static @NotNull CachedTriggerQueryResults get(@NotNull Project project,
                                                         @NotNull TriggerQueryUtil.SchemaType schemaType,
                                                         @NotNull String name,
                                                         @Nullable String stateFilter,
                                                         boolean includeRelatedPolicies) {
        String environmentKey = MatrixAdminDefinitionCache.currentEnvironmentKey(project);
        if (environmentKey.isBlank() || !MatrixAdminDefinitionCache.cacheDatabaseExists(project)) {
            return CachedTriggerQueryResults.missing();
        }

        QueryKey queryKey = QueryKey.of(schemaType, name, stateFilter, includeRelatedPolicies);
        try (Connection connection = MatrixAdminDefinitionCache.openDatabase(project, false)) {
            ensureSchema(connection);
            if (!hasMeta(connection, environmentKey, queryKey)) {
                return CachedTriggerQueryResults.missing();
            }
            return new CachedTriggerQueryResults(readRows(connection, environmentKey, queryKey), true);
        } catch (IOException | SQLException | IllegalStateException ignored) {
            return CachedTriggerQueryResults.missing();
        }
    }

    public static void put(@NotNull Project project,
                           @NotNull TriggerQueryUtil.SchemaType schemaType,
                           @NotNull String name,
                           @Nullable String stateFilter,
                           @NotNull List<TriggerQueryUtil.TriggerQueryResult> results) throws MQLException {
        put(project, schemaType, name, stateFilter, true, results);
    }

    public static void put(@NotNull Project project,
                           @NotNull TriggerQueryUtil.SchemaType schemaType,
                           @NotNull String name,
                           @Nullable String stateFilter,
                           boolean includeRelatedPolicies,
                           @NotNull List<TriggerQueryUtil.TriggerQueryResult> results) throws MQLException {
        String environmentKey = MatrixAdminDefinitionCache.currentEnvironmentKey(project);
        if (environmentKey.isBlank()) {
            return;
        }

        QueryKey queryKey = QueryKey.of(schemaType, name, stateFilter, includeRelatedPolicies);
        long loadedAtMillis = System.currentTimeMillis();
        Connection connection = null;
        try {
            connection = MatrixAdminDefinitionCache.openDatabase(project, true);
            ensureSchema(connection);
            connection.setAutoCommit(false);
            deleteQuery(connection, environmentKey, queryKey);
            insertRows(connection, environmentKey, queryKey, results, loadedAtMillis);
            insertMeta(connection, environmentKey, queryKey, results.size(), loadedAtMillis);
            connection.commit();
        } catch (IOException | SQLException | IllegalStateException e) {
            rollbackQuietly(connection);
            throw new MQLException("Persist trigger query cache failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(connection);
        }
    }

    static void clearEnvironment(@NotNull Project project, @NotNull String environmentKey) {
        if (environmentKey.isBlank() || !MatrixAdminDefinitionCache.cacheDatabaseExists(project)) {
            return;
        }

        try (Connection connection = MatrixAdminDefinitionCache.openDatabase(project, false)) {
            ensureSchema(connection);
            deleteEnvironment(connection, environmentKey);
        } catch (IOException | SQLException | IllegalStateException ignored) {
        }
    }

    private static void ensureSchema(@NotNull Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists trigger_query_cache_meta (
                        environment text not null,
                        query_schema_type text not null,
                        query_schema_name text not null,
                        state_filter text not null,
                        row_count integer not null,
                        loaded_at integer not null,
                        primary key (environment, query_schema_type, query_schema_name, state_filter)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists trigger_query_cache (
                        environment text not null,
                        query_schema_type text not null,
                        query_schema_name text not null,
                        state_filter text not null,
                        row_index integer not null,
                        result_schema_type text not null,
                        result_schema_name text not null,
                        state text not null,
                        event_type text not null,
                        sequence integer not null,
                        trigger_name text not null,
                        program text not null,
                        method text not null,
                        source_path text not null,
                        source_line integer not null,
                        loaded_at integer not null,
                        primary key (environment, query_schema_type, query_schema_name, state_filter, row_index)
                    )
                    """);
            statement.executeUpdate("""
                    create index if not exists idx_trigger_query_cache_lookup
                    on trigger_query_cache(environment, query_schema_type, query_schema_name, state_filter, row_index)
                    """);
        }
    }

    private static boolean hasMeta(@NotNull Connection connection,
                                   @NotNull String environmentKey,
                                   @NotNull QueryKey queryKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select row_count from trigger_query_cache_meta
                where environment = ? and query_schema_type = ? and query_schema_name = ? and state_filter = ?
                """)) {
            bindQueryKey(statement, environmentKey, queryKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static @NotNull List<TriggerQueryUtil.TriggerQueryResult> readRows(@NotNull Connection connection,
                                                                               @NotNull String environmentKey,
                                                                               @NotNull QueryKey queryKey)
            throws SQLException {
        List<TriggerQueryUtil.TriggerQueryResult> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select result_schema_type, result_schema_name, state, event_type, sequence,
                       trigger_name, program, method, source_path, source_line
                from trigger_query_cache
                where environment = ? and query_schema_type = ? and query_schema_name = ? and state_filter = ?
                order by row_index
                """)) {
            bindQueryKey(statement, environmentKey, queryKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new TriggerQueryUtil.TriggerQueryResult(
                            resultSet.getString("result_schema_type"),
                            resultSet.getString("result_schema_name"),
                            resultSet.getString("state"),
                            resultSet.getString("event_type"),
                            resultSet.getInt("sequence"),
                            resultSet.getString("trigger_name"),
                            resultSet.getString("program"),
                            resultSet.getString("method"),
                            resultSet.getString("source_path"),
                            resultSet.getInt("source_line")
                    ));
                }
            }
        }
        return List.copyOf(results);
    }

    private static void deleteQuery(@NotNull Connection connection,
                                    @NotNull String environmentKey,
                                    @NotNull QueryKey queryKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                delete from trigger_query_cache
                where environment = ? and query_schema_type = ? and query_schema_name = ? and state_filter = ?
                """)) {
            bindQueryKey(statement, environmentKey, queryKey);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                delete from trigger_query_cache_meta
                where environment = ? and query_schema_type = ? and query_schema_name = ? and state_filter = ?
                """)) {
            bindQueryKey(statement, environmentKey, queryKey);
            statement.executeUpdate();
        }
    }

    private static void deleteEnvironment(@NotNull Connection connection,
                                          @NotNull String environmentKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from trigger_query_cache where environment = ?")) {
            statement.setString(1, environmentKey);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from trigger_query_cache_meta where environment = ?")) {
            statement.setString(1, environmentKey);
            statement.executeUpdate();
        }
    }

    private static void insertRows(@NotNull Connection connection,
                                   @NotNull String environmentKey,
                                   @NotNull QueryKey queryKey,
                                   @NotNull List<TriggerQueryUtil.TriggerQueryResult> results,
                                   long loadedAtMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into trigger_query_cache(
                    environment, query_schema_type, query_schema_name, state_filter, row_index,
                    result_schema_type, result_schema_name, state, event_type, sequence,
                    trigger_name, program, method, source_path, source_line, loaded_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (int i = 0; i < results.size(); i++) {
                TriggerQueryUtil.TriggerQueryResult result = results.get(i);
                bindQueryKey(statement, environmentKey, queryKey);
                statement.setInt(5, i);
                statement.setString(6, result.schemaType());
                statement.setString(7, result.schemaName());
                statement.setString(8, result.state());
                statement.setString(9, result.eventType());
                statement.setInt(10, result.sequence());
                statement.setString(11, result.triggerName());
                statement.setString(12, result.program());
                statement.setString(13, result.method());
                statement.setString(14, result.sourcePath());
                statement.setInt(15, result.sourceLine());
                statement.setLong(16, loadedAtMillis);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void insertMeta(@NotNull Connection connection,
                                   @NotNull String environmentKey,
                                   @NotNull QueryKey queryKey,
                                   int rowCount,
                                   long loadedAtMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into trigger_query_cache_meta(
                    environment, query_schema_type, query_schema_name, state_filter, row_count, loaded_at
                ) values (?, ?, ?, ?, ?, ?)
                """)) {
            bindQueryKey(statement, environmentKey, queryKey);
            statement.setInt(5, rowCount);
            statement.setLong(6, loadedAtMillis);
            statement.executeUpdate();
        }
    }

    private static void bindQueryKey(@NotNull PreparedStatement statement,
                                     @NotNull String environmentKey,
                                     @NotNull QueryKey queryKey) throws SQLException {
        statement.setString(1, environmentKey);
        statement.setString(2, queryKey.schemaType());
        statement.setString(3, queryKey.schemaName());
        statement.setString(4, queryKey.stateFilter());
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private record QueryKey(@NotNull String schemaType,
                            @NotNull String schemaName,
                            @NotNull String stateFilter) {
        private static @NotNull QueryKey of(@NotNull TriggerQueryUtil.SchemaType schemaType,
                                            @NotNull String name,
                                            @Nullable String stateFilter,
                                            boolean includeRelatedPolicies) {
            String cacheSchemaType = QUERY_FORMAT_VERSION + "|" + schemaType.name();
            if (!includeRelatedPolicies && schemaType.includes(TriggerQueryUtil.SchemaType.TYPE)) {
                cacheSchemaType += "|WITHOUT_RELATED_POLICIES";
            }
            return new QueryKey(cacheSchemaType, name.trim(), stateFilter == null ? "" : stateFilter.trim());
        }
    }

    public record CachedTriggerQueryResults(@NotNull List<TriggerQueryUtil.TriggerQueryResult> results,
                                            boolean loaded) {
        public CachedTriggerQueryResults {
            results = List.copyOf(results);
        }

        private static @NotNull CachedTriggerQueryResults missing() {
            return new CachedTriggerQueryResults(List.of(), false);
        }
    }
}
