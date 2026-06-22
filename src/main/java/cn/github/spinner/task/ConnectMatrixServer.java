package cn.github.spinner.task;

import cn.github.driver.MatrixDriverManager;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.MatrixDriversConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.service.DriverKeepAliveService;
import cn.github.spinner.util.MatrixConnectionUtil;
import cn.github.spinner.util.MatrixJarLoadManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ConnectMatrixServer extends TrackedBackgroundTask {
    private final EnvironmentConfig environment;
    @Setter
    private Runnable successHandler;

    public ConnectMatrixServer(@Nullable Project project, EnvironmentConfig environment) {
        super(project, SpinnerBundle.message("progress.connect.3dexperience"), true);
        this.environment = environment;
        setCancelText(SpinnerBundle.message("progress.cancel.connect"));
    }

    @Override
    protected void runTracked(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        if (environment == null) {
            return;
        }
        MatrixConnection connection = UserInput.getInstance().connection.get(myProject);
        if (connection != null) {
            UIUtil.showWarningNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.server.connected"));
            return;
        }
        try {
            MatrixConnectionUtil.assertServerReachable(environment);
        } catch (Exception e) {
            UIUtil.showErrorNotification(myProject,
                    UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER,
                    SpinnerBundle.message("message.connect.failed", e.getMessage()));
            return;
        }
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(environment.getDriver());
        if (driverInfo == null || driverInfo.getDriverClass() == null || driverInfo.getDriverClass().isEmpty()) {
            UIUtil.showWarningNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.load.driver.empty", environment.getDriver()));
            return;
        }
        List<File> driverFiles = MatrixDriversConfig.getInstance().getDriverFiles(environment.getDriver());
        ClassLoader classLoader = MatrixJarLoadManager.loadMatrixJars(environment.getName(), driverFiles, getClass().getClassLoader());
        try {
            Class.forName(driverInfo.getDriverClass(), true, classLoader);
        } catch (ClassNotFoundException e) {
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.load.driver.class.not.found", environment.getDriver()));
            return;
        }
        ExecutorService executor = MatrixConnectionUtil.newDaemonSingleThreadExecutor("spinner-matrix-connect");
        Future<MatrixConnection> future = executor.submit(() ->
                MatrixDriverManager.getConnection(
                        environment.getHostUrl(),
                        environment.getUser(),
                        environment.getPassword(),
                        environment.getVault(),
                        environment.getRole(),
                        environment.isCas(),
                        classLoader
                )
        );
        try {
            // 用 IDEA 提供的工具方法等待，不阻塞进度条
            MatrixConnection newConnection = future.get(20, TimeUnit.SECONDS);
            UserInput.getInstance().connection.put(myProject, newConnection);
            UserInput.getInstance().connectEnvironment.put(myProject, environment);
            DriverKeepAliveService.getInstance(myProject).schedule(environment);
            UIUtil.refreshEnvironmentToolWindow(myProject);
            UIUtil.showNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.server.connect.success"));
            if (successHandler != null) {
                successHandler.run();
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.connect.timeout", 20));
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.connect.failed", e.getCause().getMessage()));
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public void onFinished() {
        super.onFinished();
        UserInput.getInstance().connectingEnvironment.remove(myProject);
    }
}
