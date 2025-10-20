package com.bol.spinner.action.basic;

import com.bol.spinner.editor.MQLLanguage;
import com.bol.spinner.util.UIUtil;
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
public class MQLConsoleAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        // 初始化MQL执行器窗口
        UIUtil.getMQLExecutorToolWindow(project);
        // 创建一个虚拟文件用于标识编辑器
        LightVirtualFile file = new LightVirtualFile("MQL Console");
        file.setLanguage(MQLLanguage.INSTANCE);
        file.setWritable(true);
        file.setCharset(null);
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(file, true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
