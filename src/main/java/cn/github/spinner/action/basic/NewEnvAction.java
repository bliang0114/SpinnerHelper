package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.ui.EnvironmentSettingsDialog;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class NewEnvAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) {
            return;
        }
        EnvironmentSettingsDialog dialog = new EnvironmentSettingsDialog(project, null);
        if (dialog.showAndGet()) {
            EnvironmentConfig environment = dialog.getEnvironment();
            SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
            spinnerSettings.getEnvironments().add(environment);
            toolWindow.refreshTree();
        }
    }
}
