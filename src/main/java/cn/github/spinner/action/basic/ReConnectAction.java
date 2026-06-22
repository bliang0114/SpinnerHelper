package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.service.DriverKeepAliveService;
import cn.github.spinner.task.ConnectMatrixServer;
import cn.github.spinner.util.DeployUtil;
import cn.github.spinner.util.MatrixConnectionUtil;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ReConnectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

            MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection != null) {
            EnvironmentConfig connectEnvironment = UserInput.getInstance().connectEnvironment.get(project);
            if (connectEnvironment == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, SpinnerBundle.message("message.connected.environment.missing"));
                return;
            }
            DriverKeepAliveService.getInstance(project).cancel();
            UserInput.getInstance().connection.remove(project);
            UserInput.getInstance().connectEnvironment.remove(project);
            UIUtil.refreshEnvironmentToolWindow(project);

            UserInput.getInstance().connectingEnvironment.put(project, connectEnvironment);
            MatrixConnectionUtil.closeAsync(project, connection, SpinnerBundle.message("action.Spinner Config.ReConnect.text"), () -> {
                ConnectMatrixServer task = new ConnectMatrixServer(project, connectEnvironment);
                task.setSuccessHandler(() -> DeployUtil.installDeployJpo(project));
                ProgressManager.getInstance().run(task);
            });
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
