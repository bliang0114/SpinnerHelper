package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.ui.EnvironmentSettingsDialog;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EditEnvAction extends EnvironmentTbActionAdapter {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) return;

        Optional<EnvironmentConfig> optional = getSettingsEnvironment(project);
        if (optional.isEmpty()) return;

        EnvironmentConfig environment = optional.get();
        EnvironmentSettingsDialog dialog = new EnvironmentSettingsDialog(project, environment);
        if (dialog.showAndGet()) {
            environment = dialog.getEnvironment();
            optional.get().update(environment);
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
