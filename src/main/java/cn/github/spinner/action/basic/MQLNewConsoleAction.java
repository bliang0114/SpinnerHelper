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
import com.intellij.testFramework.LightVirtualFileBase;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.OptionalInt;

@Slf4j
public class MQLNewConsoleAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        List<LightVirtualFile> consoleFiles = SpinnerToken.getMQLConsoleFiles(project);
        OptionalInt maxSequence = consoleFiles.stream().map(LightVirtualFileBase::getName)
                .filter(str -> !SpinnerToken.DEFAULT_MQL_CONSOLE.equals(str))
                .map(str -> str.replace("MQL Console ", ""))
                .map(Integer::parseInt)
                .mapToInt(Integer::intValue).max();
        int max = maxSequence.isPresent() ? maxSequence.getAsInt() + 1 : 1;
        String consoleName = "MQL Console " + max;
        LightVirtualFile consoleFile = new LightVirtualFile(consoleName);
        consoleFile.setLanguage(MQLLanguage.INSTANCE);
        consoleFile.setWritable(true);
        consoleFile.setCharset(null);
        SpinnerToken.putMQLConsoleFile(project, consoleFile);

        MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(project);
        if (toolWindow != null) {
            toolWindow.addNodeToTree(consoleName);
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(consoleFile, true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
