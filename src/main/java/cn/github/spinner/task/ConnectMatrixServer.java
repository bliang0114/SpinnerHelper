package cn.github.spinner.task;

import cn.github.driver.MatrixDriverManager;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.MatrixDriversConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.util.MatrixJarLoadManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ConnectMatrixServer extends Task.Backgroundable {
    private final EnvironmentConfig environment;
    @Setter
    private Runnable successHandler;

    public ConnectMatrixServer(@Nullable Project project, EnvironmentConfig environment) {
        super(project, "Connect to 3DExperience", true);
        this.environment = environment;
        setCancelText("Cancel Connect");
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        MatrixConnection connection = UserInput.getInstance().connection.get(myProject);
        if (connection != null) {
            UIUtil.showWarningNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Server is connected.");
            return;
        }
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(environment.getDriver());
        if (driverInfo == null || driverInfo.getDriverClass() == null || driverInfo.getDriverClass().isEmpty()) {
            UIUtil.showWarningNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Load Driver Error<br/>" + environment.getDriver() + " Driver is empty.");
            return;
        }
        List<File> driverFiles = MatrixDriversConfig.getInstance().getDriverFiles(environment.getDriver());
        ClassLoader classLoader = MatrixJarLoadManager.loadMatrixJars(environment.getName(), driverFiles, getClass().getClassLoader());
        try {
            Class.forName(driverInfo.getDriverClass(), true, classLoader);
        } catch (ClassNotFoundException e) {
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Load Driver Error<br/>" + environment.getDriver() + " Driver class not found.");
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<MatrixConnection> future = executor.submit(() ->
                MatrixDriverManager.getConnection(
                        environment.getHostUrl(),
                        environment.getUser(),
                        environment.getPassword(),
                        environment.getVault(),
                        environment.getRole(),
                        classLoader
                )
        );
        try {
            // 用 IDEA 提供的工具方法等待，不阻塞进度条
            MatrixConnection newConnection = future.get(20, TimeUnit.SECONDS);
            UserInput.getInstance().connection.put(myProject, newConnection);
            UserInput.getInstance().connectEnvironment.put(myProject, environment);
            UIUtil.showNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Server connect successfully.");
        } catch (TimeoutException e) {
            future.cancel(true);
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Connect timeout (20s exceeded).");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Connect failed: " + e.getCause().getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (successHandler != null) {
            successHandler.run();
        }
    }

    @Override
    public void onFinished() {
        super.onFinished();
        UserInput.getInstance().connectingEnvironment.remove(myProject);
    }
}
