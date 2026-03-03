package cn.github.spinner.action.basic;

import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MQLDefaultConsoleAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, UserInput.DEFAULT_MQL_CONSOLE);
        if (consoleManager == null) {
            LightVirtualFile consoleFile = new LightVirtualFile(UserInput.DEFAULT_MQL_CONSOLE);
            consoleFile.setLanguage(MQLLanguage.INSTANCE);
            consoleFile.setWritable(true);
            consoleFile.setCharset(null);
            consoleManager = new ConsoleManager(project, UserInput.DEFAULT_MQL_CONSOLE, consoleFile);
            UserInput.getInstance().putConsole(project, consoleManager.getConsoleName(), consoleManager);
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(consoleManager.getConsoleFile(), true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
