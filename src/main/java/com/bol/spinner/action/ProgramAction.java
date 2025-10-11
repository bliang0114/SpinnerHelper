package com.bol.spinner.action; // 请根据你的实际包名调整

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.bol.spinner.util.UIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProgramAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        LightVirtualFile virtualFile = new LightVirtualFile("ProgramConfiguration");
        Project project = anActionEvent.getProject();
        if (project == null) return;
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
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