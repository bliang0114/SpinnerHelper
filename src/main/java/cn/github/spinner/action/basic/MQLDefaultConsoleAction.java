package cn.github.spinner.action.basic;

import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.util.UIUtil;
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

        LightVirtualFile consoleFile = SpinnerToken.getMQLConsoleFile(project, SpinnerToken.DEFAULT_MQL_CONSOLE);
        if (consoleFile == null) {
            consoleFile = new LightVirtualFile(SpinnerToken.DEFAULT_MQL_CONSOLE);
            consoleFile.setLanguage(MQLLanguage.INSTANCE);
            consoleFile.setWritable(true);
            consoleFile.setCharset(null);
            SpinnerToken.putMQLConsoleFile(project, consoleFile);
        }
        MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(project);
        if (toolWindow != null) {
            toolWindow.addNodeToTree(SpinnerToken.DEFAULT_MQL_CONSOLE);
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(consoleFile, true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
