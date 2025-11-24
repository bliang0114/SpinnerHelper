package cn.github.spinner.config;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SpinnerToken {
    public static final String DEFAULT_MQL_CONSOLE = "Default MQL Console";
    public static final Map<String, MatrixConnection> CONNECTION_MAP = new ConcurrentHashMap<>();
    public static final Map<String, String> ENVIRONMENT_NAME_MAP = new ConcurrentHashMap<>();
    public static final Map<String, ObjectWhereExpression> OBJECT_WHERE_EXPRESSION_MAP = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, LightVirtualFile>> MQL_CONSOLE_MAP = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, ConsoleManager>> MQL_CONSOLE_EXECUTOR_MAP = new ConcurrentHashMap<>();

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

    public static List<LightVirtualFile> getMQLConsoleFiles(@NotNull Project project) {
        Map<String, LightVirtualFile> virtualFileMap = MQL_CONSOLE_MAP.get(project.getLocationHash());
        if (virtualFileMap == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(virtualFileMap.values());
    }

    public static LightVirtualFile getMQLConsoleFile(@NotNull Project project, String consoleName) {
        Map<String, LightVirtualFile> virtualFileMap = MQL_CONSOLE_MAP.get(project.getLocationHash());
        if (virtualFileMap == null) {
            return null;
        }

        return virtualFileMap.get(consoleName);
    }

    public static void putMQLConsoleFile(@NotNull Project project, @NotNull LightVirtualFile virtualFile) {
        Map<String, LightVirtualFile> virtualFileMap = MQL_CONSOLE_MAP.computeIfAbsent(project.getLocationHash(), k -> new ConcurrentHashMap<>());
        virtualFileMap.put(virtualFile.getName(), virtualFile);
    }

    public static ConsoleManager getConsoleManager(@NotNull Project project, String consoleName) {
        Map<String, ConsoleManager> consoleManagerMap = MQL_CONSOLE_EXECUTOR_MAP.get(project.getLocationHash());
        if (consoleManagerMap == null) {
            return null;
        }
        return consoleManagerMap.get(consoleName);
    }

    public static void putConsoleManager(@NotNull Project project, String consoleName, @NotNull ConsoleManager consoleManager) {
        Map<String, ConsoleManager> consoleManagerMap = MQL_CONSOLE_EXECUTOR_MAP.computeIfAbsent(project.getLocationHash(), k -> new ConcurrentHashMap<>());
        consoleManagerMap.put(consoleName, consoleManager);
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

    public static void closeConnection(@NotNull Project project) {
        MatrixConnection connection = getCurrentConnection(project);
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            log.error("Error: close matrix connection, {}", e.getLocalizedMessage(), e);
        }
        CONNECTION_MAP.remove(project.getLocationHash());
    }
}
