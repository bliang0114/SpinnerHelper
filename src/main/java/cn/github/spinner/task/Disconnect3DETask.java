package cn.github.spinner.task;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.ui.EnvironmentToolWindow;
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
        assert myProject != null;
        SpinnerToken.closeConnection(myProject);
        environment.setConnected(false);
        SpinnerToken.putEnvironmentName(myProject, "");
        SwingUtilities.invokeLater(toolWindow::repaint);
    }
}
