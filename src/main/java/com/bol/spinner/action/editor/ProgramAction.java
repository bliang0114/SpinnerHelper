package com.bol.spinner.action.editor; // 请根据你的实际包名调整

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class ProgramAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 创建一个虚拟文件用于标识编辑器
        LightVirtualFile file = new LightVirtualFile("Program View");
        file.setFileType(MatrixDataViewFileType.PROGRAM);
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    // ... 你已有的 update 和 getActionUpdateThread 方法 ...
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