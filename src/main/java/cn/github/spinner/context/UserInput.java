package cn.github.spinner.context;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserInput {
    private static volatile UserInput instance;

    private UserInput() {
    }

    public static UserInput getInstance() {
        if (instance == null) {
            synchronized (UserInput.class) {
                if (instance == null) {
                    instance = new UserInput();
                }
            }
        }
        return instance;
    }

    public static final String NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER = "Connect Matrix Server";
    public static final String NOTIFICATION_TITLE_LOAD_DATA = "Load Data";
    public static final String NOTIFICATION_TITLE_MQL_EXECUTE = "MQL Execute";
    public static final String NOTIFICATION_TITLE_DEPLOY = "Deploy";
    public static final String DEFAULT_MQL_CONSOLE = "Default MQL Console";

    public Map<Project, EnvironmentConfig> connectEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> connectingEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> clickEnvironment = new ConcurrentHashMap<>();
    public Map<Project, MatrixConnection> connection = new ConcurrentHashMap<>();
    public Map<Project, Map<String, ConsoleManager>> mqlConsole = new ConcurrentHashMap<>();

    public ConsoleManager getConsole(Project project, String consoleName) {
        Map<String, ConsoleManager> consoleMap = mqlConsole.get(project);
        if (consoleMap == null) {
            return null;
        }
        return consoleMap.get(consoleName);
    }

    public void putConsole(Project project, String consoleName, ConsoleManager consoleManager) {
        Map<String, ConsoleManager> consoleMap = mqlConsole.computeIfAbsent(project, k -> new ConcurrentHashMap<>());
        consoleMap.put(consoleName, consoleManager);
    }
}
