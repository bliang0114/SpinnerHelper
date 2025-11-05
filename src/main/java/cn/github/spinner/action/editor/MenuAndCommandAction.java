package cn.github.spinner.action.editor;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.editor.MatrixDataViewFileType;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class MenuAndCommandAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        // 创建一个虚拟文件用于标识编辑器
        LightVirtualFile file = new LightVirtualFile("MenuAndCommand View");
        file.setFileType(MatrixDataViewFileType.MENU_COMMAND);
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
}