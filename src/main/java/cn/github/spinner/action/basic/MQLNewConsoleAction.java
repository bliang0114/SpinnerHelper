package cn.github.spinner.action.basic;

import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.ConsoleFileManager;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MQLNewConsoleAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        ConsoleManager consoleManager = ConsoleFileManager.createNewConsole(project);
        ConsoleFileManager.openConsole(project, consoleManager);
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow != null) {
            toolWindow.refreshTree();
            toolWindow.selectConsole(consoleManager.getConsoleName());
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
