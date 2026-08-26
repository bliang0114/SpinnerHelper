package cn.github.spinner.action.editor;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MatrixAdminDefinitionCache;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class LoadDefinitionForMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, SpinnerBundle.message("message.connect.required"));
            return;
        }

        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.load.matrix.admin.definitions"), true) {
            private MatrixAdminDefinitionCache.MatrixAdminDefinitions definitions;
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(SpinnerBundle.message("progress.load.matrix.admin.definitions"));
                try {
                    definitions = MatrixAdminDefinitionCache.reload(project, connection);
                } catch (MQLException ex) {
                    error = ex;
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (error != null) {
                    UIUtil.showErrorNotification(project, UserInput.NOTIFICATION_TITLE_LOAD_DATA, error.getLocalizedMessage());
                    return;
                }
                UIUtil.refreshEnvironmentToolWindow(project);
                UIUtil.showNotification(project,
                        UserInput.NOTIFICATION_TITLE_LOAD_DATA,
                        SpinnerBundle.message("message.load.admin.definitions.success",
                                definitions.count(MatrixAdminDefinitionCache.AdminType.TYPE),
                                definitions.count(MatrixAdminDefinitionCache.AdminType.POLICY),
                                definitions.count(MatrixAdminDefinitionCache.AdminType.RELATIONSHIP),
                                definitions.count(MatrixAdminDefinitionCache.AdminType.ATTRIBUTE),
                                definitions.count(MatrixAdminDefinitionCache.AdminType.INTERFACE)));
            }
        }.queue();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && UserInput.getInstance().connection.get(project) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
