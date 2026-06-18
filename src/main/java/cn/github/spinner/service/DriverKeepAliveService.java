package cn.github.spinner.service;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.MatrixDriversConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.ConnectMatrixServer;
import cn.github.spinner.util.MatrixConnectionUtil;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.PROJECT)
public final class DriverKeepAliveService implements Disposable {
    private static final int BUSY_RETRY_MINUTES = 1;

    private final Project project;
    private final Object lock = new Object();
    private ScheduledFuture<?> keepAliveFuture;
    private EnvironmentConfig environment;
    private long generation;

    public DriverKeepAliveService(@NotNull Project project) {
        this.project = project;
    }

    public static DriverKeepAliveService getInstance(@NotNull Project project) {
        return project.getService(DriverKeepAliveService.class);
    }

    public void schedule(@NotNull EnvironmentConfig environment) {
        int keepAliveMinutes = MatrixDriversConfig.getInstance().getKeepAliveMinutes(environment.getDriver());
        synchronized (lock) {
            generation++;
            this.environment = environment;
            scheduleLocked(generation, keepAliveMinutes);
        }
    }

    public void rescheduleCurrentConnectionIfDriver(@NotNull String driverName) {
        EnvironmentConfig connectEnvironment = UserInput.getInstance().connectEnvironment.get(project);
        if (connectEnvironment != null && driverName.equals(connectEnvironment.getDriver())) {
            schedule(connectEnvironment);
        }
    }

    public void cancel() {
        synchronized (lock) {
            generation++;
            environment = null;
            cancelLocked();
        }
    }

    @Override
    public void dispose() {
        cancel();
    }

    private void scheduleLocked(long targetGeneration, int delayMinutes) {
        cancelLocked();
        keepAliveFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> reconnectWhenIdle(targetGeneration),
                Math.max(delayMinutes, 1),
                TimeUnit.MINUTES
        );
    }

    private void cancelLocked() {
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(false);
            keepAliveFuture = null;
        }
    }

    private void reconnectWhenIdle(long targetGeneration) {
        if (project.isDisposed()) {
            return;
        }

        EnvironmentConfig targetEnvironment;
        synchronized (lock) {
            if (targetGeneration != generation) {
                return;
            }
            targetEnvironment = environment;
        }
        if (targetEnvironment == null) {
            return;
        }

        UserInput userInput = UserInput.getInstance();
        EnvironmentConfig connectedEnvironment = userInput.connectEnvironment.get(project);
        MatrixConnection connection = userInput.connection.get(project);
        if (connection == null || connectedEnvironment == null || !isSameConnection(targetEnvironment, connectedEnvironment)) {
            cancel();
            return;
        }

        if (userInput.isBackgroundTaskRunning(project) || userInput.connectingEnvironment.containsKey(project)) {
            synchronized (lock) {
                if (targetGeneration == generation) {
                    scheduleLocked(targetGeneration, BUSY_RETRY_MINUTES);
                }
            }
            return;
        }

        userInput.connection.remove(project);
        userInput.connectEnvironment.remove(project);
        userInput.connectingEnvironment.put(project, targetEnvironment);
        UIUtil.refreshEnvironmentToolWindow(project);
        MatrixConnectionUtil.closeAsync(project, connection, SpinnerBundle.message("action.Spinner Config.ReConnect.text"), () -> {
            if (project.isDisposed() || !isCurrentGeneration(targetGeneration)) {
                return;
            }
            ConnectMatrixServer task = new ConnectMatrixServer(project, targetEnvironment);
            task.queue();
        });
    }

    private boolean isCurrentGeneration(long targetGeneration) {
        synchronized (lock) {
            return targetGeneration == generation && environment != null;
        }
    }

    private boolean isSameConnection(@NotNull EnvironmentConfig scheduledEnvironment,
                                     @NotNull EnvironmentConfig connectedEnvironment) {
        return Objects.equals(scheduledEnvironment.getName(), connectedEnvironment.getName())
                && Objects.equals(scheduledEnvironment.getDriver(), connectedEnvironment.getDriver());
    }
}
