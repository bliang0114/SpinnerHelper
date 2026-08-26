package cn.github.spinner.context;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.MatrixJarLoadManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service(Service.Level.APP)
public final class UserInput implements Disposable {
    private static final Logger LOG = Logger.getInstance(UserInput.class);

    public UserInput() {
        ApplicationManager.getApplication().getMessageBus().connect(this)
                .subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
                    @Override
                    public void projectClosed(Project project) {
                        disposeProject(project);
                    }
                });
    }

    public static UserInput getInstance() {
        return ApplicationManager.getApplication().getService(UserInput.class);
    }

    public static final String NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER = SpinnerBundle.message("notification.title.connect.matrix.server");
    public static final String NOTIFICATION_TITLE_LOAD_DATA = SpinnerBundle.message("notification.title.load.data");
    public static final String NOTIFICATION_TITLE_MQL_EXECUTE = SpinnerBundle.message("notification.title.mql.execute");
    public static final String NOTIFICATION_TITLE_DEPLOY = SpinnerBundle.message("notification.title.deploy");
    public static final String DEFAULT_MQL_CONSOLE = "Default MQL Console";

    public Map<Project, EnvironmentConfig> connectEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> connectingEnvironment = new ConcurrentHashMap<>();
    public Map<Project, EnvironmentConfig> clickEnvironment = new ConcurrentHashMap<>();
    public Map<Project, MatrixConnection> connection = new ConcurrentHashMap<>();
    public Map<Project, Map<String, ConsoleManager>> mqlConsole = new ConcurrentHashMap<>();
    private final Map<Project, AtomicInteger> backgroundTaskCount = new ConcurrentHashMap<>();

    public ConsoleManager getConsole(Project project, String consoleName) {
        Map<String, ConsoleManager> consoleMap = mqlConsole.get(project);
        if (consoleMap == null) {
            return null;
        }
        return consoleMap.get(consoleName);
    }

    public void putConsole(Project project, String consoleName, ConsoleManager consoleManager) {
        if (project.isDisposed()) {
            consoleManager.dispose();
            return;
        }
        Map<String, ConsoleManager> consoleMap = mqlConsole.computeIfAbsent(project, k -> new ConcurrentHashMap<>());
        consoleMap.put(consoleName, consoleManager);
    }

    public void backgroundTaskStarted(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }
        backgroundTaskCount.computeIfAbsent(project, key -> new AtomicInteger()).incrementAndGet();
    }

    public void backgroundTaskFinished(Project project) {
        if (project == null) {
            return;
        }
        AtomicInteger count = backgroundTaskCount.get(project);
        if (count == null || count.decrementAndGet() <= 0) {
            backgroundTaskCount.remove(project);
        }
    }

    public boolean isBackgroundTaskRunning(Project project) {
        AtomicInteger count = backgroundTaskCount.get(project);
        return count != null && count.get() > 0;
    }

    public void disposeProject(Project project) {
        MatrixConnection matrixConnection = connection.remove(project);
        connectEnvironment.remove(project);
        connectingEnvironment.remove(project);
        clickEnvironment.remove(project);
        backgroundTaskCount.remove(project);

        Map<String, ConsoleManager> consoleMap = mqlConsole.remove(project);
        if (consoleMap != null) {
            for (ConsoleManager consoleManager : consoleMap.values()) {
                consoleManager.dispose();
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (matrixConnection != null) {
                try {
                    matrixConnection.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close Matrix connection for disposed project.", e);
                }
            }
            MatrixJarLoadManager.closeProject(project);
        });
    }

    @Override
    public void dispose() {
        for (MatrixConnection matrixConnection : connection.values()) {
            try {
                matrixConnection.close();
            } catch (IOException e) {
                LOG.warn("Failed to close Matrix connection.", e);
            }
        }
        connection.clear();
        connectEnvironment.clear();
        connectingEnvironment.clear();
        clickEnvironment.clear();

        for (Map<String, ConsoleManager> consoleMap : mqlConsole.values()) {
            for (ConsoleManager consoleManager : consoleMap.values()) {
                consoleManager.dispose();
            }
        }
        mqlConsole.clear();
        backgroundTaskCount.clear();

        MatrixJarLoadManager.closeAll();
    }
}
