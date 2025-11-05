package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.ui.EnvironmentSettingsDialog;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CopyEnvAction extends EnvironmentTbActionAdapter {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) return;

        EnvironmentSettingsDialog dialog = new EnvironmentSettingsDialog(project, null);
        dialog.setValue(environment);
        if (dialog.showAndGet()) {
            environment = dialog.getEnvironment();
            SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
            spinnerSettings.getEnvironments().add(environment);
            toolWindow.refreshTree();
        }
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
