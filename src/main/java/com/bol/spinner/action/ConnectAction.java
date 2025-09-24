package com.bol.spinner.action;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.task.Connect3DETask;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Slf4j
public class ConnectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getToolWindow(project);
        if (toolWindow == null) return;

        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) return;

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        // 关闭所有的连接
        if (SpinnerToken.connection != null) {
            SpinnerToken.closeConnection();
        }
        spinnerSettings.getEnvironments().stream().filter(EnvironmentConfig::isConnected).forEach(env -> env.setConnected(false));
        // 连接
        Optional<EnvironmentConfig> optional = spinnerSettings.getEnvironment(environment.getName());
        if (optional.isPresent()) {
            environment = optional.get();
            Connect3DETask task =  new Connect3DETask(project, environment);
            task.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getToolWindow(project);
        if (toolWindow == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(SpinnerToken.connection == null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
