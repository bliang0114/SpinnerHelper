package com.bol.spinner.config;

import cn.github.driver.connection.MatrixConnection;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SpinnerToken {
    public static final Map<String, MatrixConnection> CONNECTION_MAP = new ConcurrentHashMap<>();
    public static final Map<String, String> ENVIRONMENT_NAME_MAP = new ConcurrentHashMap<>();
    public static final Map<String, ObjectWhereExpression> OBJECT_WHERE_EXPRESSION_MAP = new ConcurrentHashMap<>();

    public static MatrixConnection getCurrentConnection(@NotNull Project project) {
        return CONNECTION_MAP.get(project.getLocationHash());
    }

    public static void putConnection(@NotNull Project project, @NotNull MatrixConnection connection) {
        CONNECTION_MAP.put(project.getLocationHash(), connection);
    }

    public static String getEnvironmentName(@NotNull Project project) {
        return ENVIRONMENT_NAME_MAP.get(project.getLocationHash());
    }

    public static void putEnvironmentName(@NotNull Project project, String environmentName) {
        ENVIRONMENT_NAME_MAP.put(project.getLocationHash(), environmentName);
    }

    public static ObjectWhereExpression getObjectWhereExpression(@NotNull Project project) {
        return OBJECT_WHERE_EXPRESSION_MAP.get(project.getLocationHash());
    }

    public static void putObjectWhereExpression(@NotNull Project project, @NotNull ObjectWhereExpression objectWhereExpression) {
        OBJECT_WHERE_EXPRESSION_MAP.put(project.getLocationHash(), objectWhereExpression);
    }

    public static void closeConnection(MatrixConnection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            log.error("Error: close matrix connection, {}", e.getLocalizedMessage(), e);
        }
    }
}
