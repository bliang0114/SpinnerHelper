package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

abstract class AbstractReloadCacheAction extends AnAction {
    private final String titleKey;
    private final String progressKey;
    private final String successKey;

    protected AbstractReloadCacheAction(@NotNull String titleKey,
                                        @NotNull String progressKey,
                                        @NotNull String successKey) {
        this.titleKey = titleKey;
        this.progressKey = progressKey;
        this.successKey = successKey;
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, SpinnerBundle.message(titleKey),
                    SpinnerBundle.message("message.connect.required"));
            return;
        }

        new TrackedBackgroundTask(project, SpinnerBundle.message(progressKey)) {
            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    reload(connection);
                    UIUtil.showNotification(project, SpinnerBundle.message(titleKey), SpinnerBundle.message(successKey));
                } catch (Exception exception) {
                    UIUtil.showErrorNotification(project, SpinnerBundle.message(titleKey),
                            SpinnerBundle.message("message.reload.cache.failed", exception.getLocalizedMessage()));
                }
            }
        }.queue();
    }

    protected abstract void reload(@NotNull MatrixConnection connection) throws Exception;

    @Override
    public final void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        EnvironmentConfig environment = project == null ? null : UserInput.getInstance().connectEnvironment.get(project);
        event.getPresentation().setEnabled(environment != null);
    }

    @Override
    public final @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
