package com.bol.spinner.editor.ui;

import com.bol.spinner.editor.MQLLanguage;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class MQLConsoleManager {
    private final Project project;
    private final Map<String, MQLConsoleEditor> openEditors = new HashMap<>();

    public static MQLConsoleManager getInstance(@NotNull Project project) {
        return project.getService(MQLConsoleManager.class);
    }

    public MQLConsoleManager(@NotNull Project project) {
        this.project = project;
    }

    public void openOrFocusMQLConsole(@NotNull VirtualFile file) {
        // 检查编辑器是否已经打开
        if (openEditors.containsKey(file.getPath())) {
            MQLConsoleEditor existingEditor = openEditors.get(file.getPath());
            focusEditor(existingEditor);
        } else {
            openNewMQLConsole(file);
            // 初始化MQL执行器窗口
            UIUtil.getMQLExecutorToolWindow(project);
        }
    }

    private void openNewMQLConsole(@NotNull VirtualFile file) {
        try {
            // 创建新的MQL控制台编辑器
            MQLConsoleEditor consoleEditor = new MQLConsoleEditor(project, file);
            openEditors.put(file.getPath(), consoleEditor);

            focusEditor(consoleEditor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open MQL Console", e);
        }
    }

    private void focusEditor(@NotNull MQLConsoleEditor editor) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile file = editor.getFile();
        if (file != null) {
            fileEditorManager.openFile(file, true);
        }
    }
}
