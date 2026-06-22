package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MatrixAdminDefinitionCache {
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String PLUGIN_DIRECTORY = "SpinnerHelper";
    private static final String DATABASE_FILE = "admin-definitions.sqlite";
    private static final MatrixAdminDefinitions EMPTY = new MatrixAdminDefinitions(new EnumMap<>(AdminType.class), 0);

    private MatrixAdminDefinitionCache() {
    }

    public static @NotNull MatrixAdminDefinitions reload(@NotNull Project project,
                                                         @NotNull MatrixConnection connection) throws MQLException {
        String environmentKey = currentEnvironmentKey(project);
        if (environmentKey.isBlank()) {
            throw new MQLException("Connected environment is missing.");
        }

        EnumMap<AdminType, List<String>> definitions = loadAllDefinitions(project, connection);
        long loadedAtMillis = System.currentTimeMillis();
        writeSnapshot(project, environmentKey, definitions, loadedAtMillis);
        TriggerQueryCache.clearEnvironment(project, environmentKey);
        return new MatrixAdminDefinitions(definitions, loadedAtMillis);
    }

    public static @NotNull MatrixAdminDefinitions get(@NotNull Project project) {
        String environmentKey = currentEnvironmentKey(project);
        if (environmentKey.isBlank()) {
            return EMPTY;
        }

        CachedState cachedState = readCachedState(project, environmentKey);
        if (cachedState.loaded()) {
            return cachedState.definitions();
        }
        return loadAllDefinitionsFromCurrentConnection(project);
    }

    public static @NotNull MatrixAdminDefinitions getCached(@NotNull Project project) {
        String environmentKey = currentEnvironmentKey(project);
        if (environmentKey.isBlank()) {
            return EMPTY;
        }

        CachedState cachedState = readCachedState(project, environmentKey);
        return cachedState.loaded() ? cachedState.definitions() : EMPTY;
    }

    public static @NotNull List<String> get(@NotNull Project project, @NotNull AdminType type) {
        String environmentKey = currentEnvironmentKey(project);
        if (environmentKey.isBlank()) {
            return List.of();
        }

        CachedState cachedState = readCachedState(project, environmentKey, type);
        if (cachedState.loaded()) {
            return cachedState.definitions().get(type);
        }
        return loadDefinitionsFromCurrentConnection(project, type);
    }

    public static boolean contains(@NotNull Project project, @NotNull AdminType type, @NotNull String name) {
        String normalizedName = normalizeName(name);
        for (String definition : get(project, type)) {
            if (normalizeName(definition).equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLoaded(@NotNull Project project) {
        String environmentKey = currentEnvironmentKey(project);
        return !environmentKey.isBlank() && readCachedLoadedAt(project, environmentKey) > 0;
    }

    private static @NotNull CachedState readCachedState(@NotNull Project project,
                                                        @NotNull String environmentKey) {
        if (!cacheDatabaseExists(project)) {
            return CachedState.missing();
        }

        try (java.sql.Connection connection = openDatabase(project, false)) {
            ensureSchema(connection);
            long loadedAtMillis = readLoadedAt(connection, environmentKey);
            if (loadedAtMillis <= 0) {
                return CachedState.missing();
            }
            return new CachedState(readDefinitions(connection, environmentKey, loadedAtMillis), true);
        } catch (IOException | SQLException | IllegalStateException ignored) {
            return CachedState.missing();
        }
    }

    private static @NotNull CachedState readCachedState(@NotNull Project project,
                                                        @NotNull String environmentKey,
                                                        @NotNull AdminType type) {
        if (!cacheDatabaseExists(project)) {
            return CachedState.missing();
        }

        try (java.sql.Connection connection = openDatabase(project, false)) {
            ensureSchema(connection);
            long loadedAtMillis = readLoadedAt(connection, environmentKey);
            if (loadedAtMillis <= 0) {
                return CachedState.missing();
            }

            EnumMap<AdminType, List<String>> definitions = new EnumMap<>(AdminType.class);
            definitions.put(type, readDefinitions(connection, environmentKey, type));
            return new CachedState(new MatrixAdminDefinitions(definitions, loadedAtMillis), true);
        } catch (IOException | SQLException | IllegalStateException ignored) {
            return CachedState.missing();
        }
    }

    private static long readCachedLoadedAt(@NotNull Project project,
                                           @NotNull String environmentKey) {
        if (!cacheDatabaseExists(project)) {
            return 0;
        }

        try (java.sql.Connection connection = openDatabase(project, false)) {
            ensureSchema(connection);
            return readLoadedAt(connection, environmentKey);
        } catch (IOException | SQLException | IllegalStateException ignored) {
            return 0;
        }
    }

    private static @NotNull MatrixAdminDefinitions loadAllDefinitionsFromCurrentConnection(@NotNull Project project) {
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            return EMPTY;
        }

        try {
            return new MatrixAdminDefinitions(loadAllDefinitions(project, connection), 0);
        } catch (MQLException ignored) {
            return EMPTY;
        }
    }

    private static @NotNull List<String> loadDefinitionsFromCurrentConnection(@NotNull Project project,
                                                                              @NotNull AdminType type) {
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            return List.of();
        }

        try {
            return loadDefinitions(project, connection, type);
        } catch (MQLException ignored) {
            return List.of();
        }
    }

    private static @NotNull EnumMap<AdminType, List<String>> loadAllDefinitions(@Nullable Project project,
                                                                                @NotNull MatrixConnection connection)
            throws MQLException {
        EnumMap<AdminType, List<String>> definitions = new EnumMap<>(AdminType.class);
        for (AdminType type : AdminType.values()) {
            definitions.put(type, loadDefinitions(project, connection, type));
        }
        return definitions;
    }

    private static void writeSnapshot(@NotNull Project project,
                                      @NotNull String environmentKey,
                                      @NotNull Map<AdminType, List<String>> definitions,
                                      long loadedAtMillis) throws MQLException {
        java.sql.Connection connection = null;
        try {
            connection = openDatabase(project, true);
            ensureSchema(connection);
            connection.setAutoCommit(false);
            deleteEnvironmentSnapshot(connection, environmentKey);
            insertDefinitionRows(connection, environmentKey, definitions, loadedAtMillis);
            insertMetaRow(connection, environmentKey, loadedAtMillis);
            connection.commit();
        } catch (IOException | SQLException | IllegalStateException e) {
            rollbackQuietly(connection);
            throw new MQLException("Persist Matrix definitions failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(connection);
        }
    }

    private static void deleteEnvironmentSnapshot(@NotNull java.sql.Connection connection,
                                                  @NotNull String environmentKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from admin_definitions where environment = ?")) {
            statement.setString(1, environmentKey);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "delete from admin_definition_meta where environment = ?")) {
            statement.setString(1, environmentKey);
            statement.executeUpdate();
        }
    }

    private static void insertDefinitionRows(@NotNull java.sql.Connection connection,
                                             @NotNull String environmentKey,
                                             @NotNull Map<AdminType, List<String>> definitions,
                                             long loadedAtMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into admin_definitions(environment, admin_type, name, loaded_at) values (?, ?, ?, ?)")) {
            for (AdminType type : AdminType.values()) {
                for (String name : definitions.getOrDefault(type, List.of())) {
                    statement.setString(1, environmentKey);
                    statement.setString(2, type.name());
                    statement.setString(3, name);
                    statement.setLong(4, loadedAtMillis);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static void insertMetaRow(@NotNull java.sql.Connection connection,
                                      @NotNull String environmentKey,
                                      long loadedAtMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into admin_definition_meta(environment, loaded_at) values (?, ?)")) {
            statement.setString(1, environmentKey);
            statement.setLong(2, loadedAtMillis);
            statement.executeUpdate();
        }
    }

    private static @NotNull MatrixAdminDefinitions readDefinitions(@NotNull java.sql.Connection connection,
                                                                   @NotNull String environmentKey,
                                                                   long loadedAtMillis) throws SQLException {
        EnumMap<AdminType, List<String>> definitions = new EnumMap<>(AdminType.class);
        for (AdminType type : AdminType.values()) {
            definitions.put(type, new ArrayList<>());
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "select admin_type, name from admin_definitions where environment = ? order by admin_type, lower(name), name")) {
            statement.setString(1, environmentKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        AdminType type = AdminType.valueOf(resultSet.getString("admin_type"));
                        definitions.get(type).add(resultSet.getString("name"));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return new MatrixAdminDefinitions(definitions, loadedAtMillis);
    }

    private static @NotNull List<String> readDefinitions(@NotNull java.sql.Connection connection,
                                                         @NotNull String environmentKey,
                                                         @NotNull AdminType type) throws SQLException {
        List<String> definitions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select name from admin_definitions where environment = ? and admin_type = ? order by lower(name), name")) {
            statement.setString(1, environmentKey);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    definitions.add(resultSet.getString("name"));
                }
            }
        }
        return List.copyOf(definitions);
    }

    private static long readLoadedAt(@NotNull java.sql.Connection connection,
                                     @NotNull String environmentKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select loaded_at from admin_definition_meta where environment = ?")) {
            statement.setString(1, environmentKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("loaded_at");
                }
            }
        }
        return 0;
    }

    private static void ensureSchema(@NotNull java.sql.Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    create table if not exists admin_definitions (
                        environment text not null,
                        admin_type text not null,
                        name text not null,
                        loaded_at integer not null,
                        primary key (environment, admin_type, name)
                    )
                    """);
            statement.executeUpdate("""
                    create table if not exists admin_definition_meta (
                        environment text primary key,
                        loaded_at integer not null
                    )
                    """);
            statement.executeUpdate("""
                    create index if not exists idx_admin_definitions_lookup
                    on admin_definitions(environment, admin_type, name)
                    """);
        }
    }

    static @NotNull java.sql.Connection openDatabase(@NotNull Project project,
                                                     boolean create) throws IOException, SQLException {
        Path databaseFile = databaseFile(project);
        if (!create && Files.notExists(databaseFile)) {
            throw new IOException("Matrix definitions cache does not exist.");
        }
        if (create) {
            Files.createDirectories(databaseFile.getParent());
        }
        loadSqliteDriver();
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
    }

    private static void loadSqliteDriver() throws SQLException {
        try {
            Class.forName(SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver is missing.", e);
        }
    }

    static boolean cacheDatabaseExists(@NotNull Project project) {
        try {
            return Files.exists(databaseFile(project));
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static @NotNull Path databaseFile(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Project base path is unavailable.");
        }
        return Path.of(basePath, ".idea", PLUGIN_DIRECTORY, DATABASE_FILE);
    }

    private static @NotNull List<String> loadDefinitions(@Nullable Project project,
                                                         @NotNull MatrixConnection connection,
                                                         @NotNull AdminType type) throws MQLException {
        MatrixResultSet resultSet = MQLUtil.executeQuery(project, connection, "list " + type.mqlName());
        if (!resultSet.isSuccess()) {
            throw new MQLException(resultSet.getMessage());
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        resultSet.getResult().lines()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .forEach(names::add);
        List<String> sortedNames = new ArrayList<>(names);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        return sortedNames;
    }

    private static void rollbackQuietly(java.sql.Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static void closeQuietly(java.sql.Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    static @NotNull String currentEnvironmentKey(@NotNull Project project) {
        EnvironmentConfig environment = UserInput.getInstance().connectEnvironment.get(project);
        if (environment == null) {
            return "";
        }
        String environmentName = environment.getName();
        return environmentName == null ? "" : environmentName.trim();
    }

    private static @NotNull String normalizeName(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public enum AdminType {
        TYPE("type", "Type", "Types"),
        POLICY("policy", "Policy", "Policies"),
        RELATIONSHIP("relationship", "Relationship", "Relationships"),
        ATTRIBUTE("attribute", "Attribute", "Attributes"),
        INTERFACE("interface", "Interface", "Interfaces");

        private final String mqlName;
        private final String displayName;
        private final String pluralDisplayName;

        AdminType(@NotNull String mqlName, @NotNull String displayName, @NotNull String pluralDisplayName) {
            this.mqlName = mqlName;
            this.displayName = displayName;
            this.pluralDisplayName = pluralDisplayName;
        }

        public @NotNull String mqlName() {
            return mqlName;
        }

        public @NotNull String displayName() {
            return displayName;
        }

        public @NotNull String pluralDisplayName() {
            return pluralDisplayName;
        }
    }

    public record MatrixAdminDefinitions(@NotNull Map<AdminType, List<String>> definitions,
                                         long loadedAtMillis) {
        public MatrixAdminDefinitions {
            EnumMap<AdminType, List<String>> copy = new EnumMap<>(AdminType.class);
            for (AdminType type : AdminType.values()) {
                copy.put(type, List.copyOf(definitions.getOrDefault(type, List.of())));
            }
            definitions = Collections.unmodifiableMap(copy);
        }

        public @NotNull List<String> get(@NotNull AdminType type) {
            return definitions.getOrDefault(type, List.of());
        }

        public int count(@NotNull AdminType type) {
            return get(type).size();
        }

        public int totalCount() {
            return definitions.values().stream().mapToInt(List::size).sum();
        }
    }

    private record CachedState(@NotNull MatrixAdminDefinitions definitions, boolean loaded) {
        private static @NotNull CachedState missing() {
            return new CachedState(EMPTY, false);
        }
    }
}
