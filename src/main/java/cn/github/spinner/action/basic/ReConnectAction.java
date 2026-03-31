package cn.github.spinner.action.basic;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MQLKeywords;
import cn.github.spinner.task.ConnectMatrixServer;
import cn.github.spinner.util.DeployUtil;
import cn.github.spinner.util.MatrixConnectionUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReConnectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection != null) {
            EnvironmentConfig connectEnvironment = UserInput.getInstance().connectEnvironment.get(project);
            if (connectEnvironment == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_CONNECT_MATRIX_SERVER, "Connected environment is missing.");
                return;
            }
            UserInput.getInstance().connection.remove(project);
            UserInput.getInstance().connectEnvironment.remove(project);

            UserInput.getInstance().connectingEnvironment.put(project, connectEnvironment);
            MatrixConnectionUtil.closeAsync(project, connection, "Reconnect Matrix Server", () -> {
                ConnectMatrixServer task = new ConnectMatrixServer(project, connectEnvironment);
                task.setSuccessHandler(() -> {
                    DeployUtil.installDeployJpo(project);
                    try {
                        MatrixConnection newConnection = UserInput.getInstance().connection.get(project);
                        StringBuilder builder = new StringBuilder();
                        MatrixStatement statement = newConnection.executeStatement("list type");
                        MatrixResultSet resultSet = statement.executeQuery();
                        if (resultSet.isSuccess()) {
                            MQLKeywords.TYPE_INSTANCES.clear();
                            List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                            MQLKeywords.TYPE_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
                            builder.append("Load Type Definition Successfully<br/>");
                        }
                        statement = newConnection.executeStatement("list relationship");
                        resultSet = statement.executeQuery();
                        if (resultSet.isSuccess()) {
                            MQLKeywords.RELATIONSHIP_INSTANCES.clear();
                            List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                            MQLKeywords.RELATIONSHIP_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
                            builder.append("Load Relationship Definition Successfully<br/>");
                        }
                        if (!builder.isEmpty()) {
                            UIUtil.showNotification(project, UserInput.NOTIFICATION_TITLE_LOAD_DATA, builder.toString());
                        }
                    } catch (MQLException ex) {
                        UIUtil.showErrorNotification(project, UserInput.NOTIFICATION_TITLE_LOAD_DATA, ex.getLocalizedMessage());
                    }
                });
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
