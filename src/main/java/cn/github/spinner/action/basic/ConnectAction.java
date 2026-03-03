package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.task.ConnectMatrixServer;
import cn.github.spinner.util.DeployUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ConnectAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        EnvironmentConfig clickEnvironment = UserInput.getInstance().clickEnvironment.get(project);

        UserInput.getInstance().connectingEnvironment.put(project, clickEnvironment);
        ConnectMatrixServer task = new ConnectMatrixServer(project, clickEnvironment);
        task.setSuccessHandler(() -> DeployUtil.installDeployJpo(project));
        ProgressManager.getInstance().run(task);
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
        EnvironmentConfig connectingEnvironment = UserInput.getInstance().connectingEnvironment.get(project);
        e.getPresentation().setEnabled(clickEnvironment != null
                && clickEnvironment != connectEnvironment
                && clickEnvironment != connectingEnvironment);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
