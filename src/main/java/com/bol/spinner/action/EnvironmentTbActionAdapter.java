package com.bol.spinner.action;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.task.Connect3DETask;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public abstract class EnvironmentTbActionAdapter extends AnAction {

    protected EnvironmentConfig getEnvironment(Project project) {
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return null;

        return toolWindow.getEnvironment();
    }

    protected Optional<EnvironmentConfig> getSettingsEnvironment(Project project) {
        EnvironmentConfig environment = getEnvironment(project);
        if (environment == null) return Optional.empty();

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        return spinnerSettings.getEnvironment(environment.getName());
    }

    protected boolean enableAction(AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) throw new IllegalArgumentException();

        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) throw new IllegalArgumentException();
        return environment.isConnected();
    }

    protected void connect(AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        Optional<EnvironmentConfig> optional = getSettingsEnvironment(project);
        if (optional.isPresent()) {
            Connect3DETask task =  new Connect3DETask(project, optional.get());
            task.queue();
        }
    }
}
