package cn.github.spinner.context;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
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

    public Map<Project, EnvironmentConfig> connectEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> connectingEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> clickEnvironment = new ConcurrentHashMap<>();
    public Map<Project, MatrixConnection> connection = new ConcurrentHashMap<>();

    public boolean isConnected(@NotNull Project project) {
        EnvironmentConfig environmentConfig = connectEnvironment.get(project);
        return environmentConfig != null;
    }
}
