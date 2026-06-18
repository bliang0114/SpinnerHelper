package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.service.DriverKeepAliveService;
import cn.github.spinner.util.MatrixConnectionUtil;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DisconnectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection != null) {
            DriverKeepAliveService.getInstance(project).cancel();
            UserInput.getInstance().connection.remove(project);
            UserInput.getInstance().connectEnvironment.remove(project);
            UIUtil.refreshEnvironmentToolWindow(project);
            MatrixConnectionUtil.closeAsync(project, connection, SpinnerBundle.message("action.Spinner Config.Disconnect.text"), null);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        e.getPresentation().setEnabled(connection != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
