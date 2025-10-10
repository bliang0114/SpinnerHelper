package com.bol.spinner.task;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class Disconnect3DETask extends Task.Backgroundable {
    private final EnvironmentConfig environment;
    private final EnvironmentToolWindow toolWindow;

    public Disconnect3DETask(@Nullable Project project, EnvironmentToolWindow toolWindow, EnvironmentConfig environment) {
        super(project, "Disconnect to 3DExperience", true);
        this.toolWindow = toolWindow;
        this.environment = environment;
        setCancelText("Stop Disconnect");
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        SpinnerToken.closeConnection();
        environment.setConnected(false);
        SpinnerToken.environmentName = null;
        SwingUtilities.invokeLater(toolWindow::repaint);
    }
}
