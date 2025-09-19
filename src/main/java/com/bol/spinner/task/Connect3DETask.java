package com.bol.spinner.task;

import com.bol.spinner.MatrixContext;
import com.bol.spinner.auth.SpinnerToken;
import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.util.SpinnerNotifier;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Connect3DETask extends Task.Backgroundable {
    private final EnvironmentConfig environment;

    public Connect3DETask(@Nullable Project project, EnvironmentConfig environment) {
        super(project, "Login", true);
        this.environment = environment;
        setCancelText("Stop Login");
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
            MatrixContext context = SpinnerToken.connect(environment.getHostUrl(), environment.getUser(), environment.getPassword(), environment.getVault(), environment.getRole());
            if (context != null) {
                this.environment.setConnected(true);
                SpinnerNotifier.showNotification(myProject, "Login successful", "");
            }
        } catch (Exception ex) {
            SpinnerNotifier.showErrorNotification(myProject, "Login failed", ex.getLocalizedMessage());
        }
    }
}
