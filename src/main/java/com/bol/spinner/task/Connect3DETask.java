package com.bol.spinner.task;

import cn.github.driver.MatrixDriverManager;
import cn.github.driver.connection.MatrixConnection;
import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.MatrixDriversConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.util.MatrixJarLoadManager;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class Connect3DETask extends Task.Backgroundable {
    private final EnvironmentConfig environment;

    public Connect3DETask(@Nullable Project project, EnvironmentConfig environment) {
        super(project, "Connect to 3DExperience", true);
        this.environment = environment;
        setCancelText("Stop Connect");
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        // 关闭所有的连接
        assert myProject != null;
        MatrixConnection connection = SpinnerToken.getCurrentConnection(myProject);
        if (connection != null) {
            progressIndicator.setText("Disconnect to 3DExperience");
            SpinnerToken.closeConnection(connection);
        }
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(myProject);
        spinnerSettings.getEnvironments().stream().filter(EnvironmentConfig::isConnected).forEach(env -> env.setConnected(false));
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(environment.getDriver());
        if (driverInfo == null || driverInfo.getDriverClass() == null || driverInfo.getDriverClass().isEmpty()) {
            UIUtil.showWarningNotification(myProject, "Load Driver Error<br/>" + environment.getDriver() + " Driver is empty", "");
            return;
        }
        List<File> driverFiles = MatrixDriversConfig.getInstance().getDriverFiles(environment.getDriver());
        ClassLoader classLoader = MatrixJarLoadManager.loadMatrixJars(environment.getName(), driverFiles, getClass().getClassLoader());
        try {
            Class.forName(driverInfo.getDriverClass(), true, classLoader);
            connection = MatrixDriverManager.getConnection(environment.getHostUrl(), environment.getUser(), environment.getPassword(), environment.getVault(), environment.getRole(), classLoader);
            SpinnerToken.putConnection(myProject, connection);
            SpinnerToken.putEnvironmentName(myProject, environment.getName());
            environment.setConnected(true);
            UIUtil.showNotification(myProject, "Connect successful", "");
        } catch (ClassNotFoundException e) {
            UIUtil.showErrorNotification(myProject, "Load Driver Error<br/>" + environment.getDriver(), "");
        } catch (Exception ex) {
            UIUtil.showErrorNotification(myProject, "Connect failed", ex.getLocalizedMessage());
        }
    }
}
