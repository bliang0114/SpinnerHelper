package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.ui.EnvironmentSettingsDialog;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EditEnvAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        EnvironmentConfig clickEnvironment = UserInput.getInstance().clickEnvironment.get(project);
        EnvironmentSettingsDialog dialog = new EnvironmentSettingsDialog(project, clickEnvironment);
        if (dialog.showAndGet()) {
            EnvironmentConfig environment = dialog.getEnvironment();
            SpinnerSettings.getInstance(project).replaceEnvironment(environment);
            toolWindow.refreshTree();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        EnvironmentConfig clickEnvironment = UserInput.getInstance().clickEnvironment.get(project);
        EnvironmentConfig connectEnvironment = UserInput.getInstance().connectEnvironment.get(project);
        e.getPresentation().setEnabled(clickEnvironment != null && clickEnvironment != connectEnvironment);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
