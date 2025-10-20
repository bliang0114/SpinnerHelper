package com.bol.spinner.action.basic;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.task.Disconnect3DETask;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DisconnectAction extends EnvironmentTbActionAdapter {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) return;

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        Optional<EnvironmentConfig> optional = spinnerSettings.getEnvironment(environment.getName());
        if (optional.isPresent()) {
            environment = optional.get();
            Disconnect3DETask task = new Disconnect3DETask(project, toolWindow, environment);
            task.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            e.getPresentation().setEnabled(enableAction(e));
        } catch (Exception ex) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
