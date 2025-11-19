package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.editor.MatrixDataViewFileType;
import cn.github.spinner.ui.EnvironmentToolWindow;
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
public class ObjectBrowserAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        LightVirtualFile file = new LightVirtualFile("Matrix Object Browser");
        file.setFileType(MatrixDataViewFileType.OBJECT_BROWSER);
        FileEditorManager.getInstance(project).openFile(file, true);
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        EnvironmentToolWindow toolWindow = UIUtil.getEnvironmentToolWindow(project);
        if (toolWindow == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        EnvironmentConfig environment = toolWindow.getEnvironment();
        if (environment == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(environment.isConnected());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
