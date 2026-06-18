package cn.github.spinner.action.editor;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MQLKeywords;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.load.mql.definitions"), true) {
            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    loadDefinitions(project, connection);
                } catch (MQLException ex) {
                    UIUtil.showErrorNotification(project, UserInput.NOTIFICATION_TITLE_LOAD_DATA, ex.getLocalizedMessage());
                }
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

    private void loadDefinitions(@NotNull Project project, @NotNull MatrixConnection connection) throws MQLException {
        StringBuilder builder = new StringBuilder();
        MatrixStatement statement = connection.executeStatement("list type");
        MatrixResultSet resultSet = statement.executeQuery();
        if (resultSet.isSuccess()) {
            MQLKeywords.TYPE_INSTANCES.clear();
            List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
            MQLKeywords.TYPE_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            builder.append(SpinnerBundle.message("message.load.type.definition.success"));
        }
        statement = connection.executeStatement("list relationship");
        resultSet = statement.executeQuery();
        if (resultSet.isSuccess()) {
            MQLKeywords.RELATIONSHIP_INSTANCES.clear();
            List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
            MQLKeywords.RELATIONSHIP_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            builder.append(SpinnerBundle.message("message.load.relationship.definition.success"));
        }
        if (!builder.isEmpty()) {
            UIUtil.showNotification(project, UserInput.NOTIFICATION_TITLE_LOAD_DATA, builder.toString());
        }
    }
}
