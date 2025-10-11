package com.bol.spinner.action;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RemoveEnvAction extends EnvironmentTbActionAdapter {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        Optional<EnvironmentConfig> optional = getSettingsEnvironment(project);
        if (optional.isEmpty()) return;

        optional.ifPresent(environmentConfig -> spinnerSettings.getEnvironments().remove(environmentConfig));
        toolWindow.refreshTree();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            e.getPresentation().setEnabled(!enableAction(e));
        } catch (Exception ex) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
