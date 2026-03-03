package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.context.UserInput;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DisconnectAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection != null) {
            new Thread(() -> {
                try {
                    connection.close();
                } catch (IOException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            UserInput.getInstance().connection.remove(project);
            UserInput.getInstance().connectEnvironment.remove(project);
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
