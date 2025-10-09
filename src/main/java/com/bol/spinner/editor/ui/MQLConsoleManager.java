package com.bol.spinner.editor.ui;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class MQLConsoleManager {
    private final Project project;
    private final Map<String, MQLConsoleEditor> openEditors = new HashMap<>();
    private static final String CONSOLE_EDITOR_ID = "MQL_CONSOLE_EDITOR";

    public static MQLConsoleManager getInstance(@NotNull Project project) {
        return project.getService(MQLConsoleManager.class);
    }

    public MQLConsoleManager(@NotNull Project project) {
        this.project = project;
    }

    public void openOrFocusMQLConsole() {
        // 检查编辑器是否已经打开
        if (openEditors.containsKey(CONSOLE_EDITOR_ID)) {
            MQLConsoleEditor existingEditor = openEditors.get(CONSOLE_EDITOR_ID);
            focusEditor(existingEditor);
        } else {
            openNewMQLConsole();
        }
    }

    private void openNewMQLConsole() {
        try {
            // 创建新的MQL控制台编辑器
            MQLConsoleEditor consoleEditor = new MQLConsoleEditor(project);
            openEditors.put(CONSOLE_EDITOR_ID, consoleEditor);

            focusEditor(consoleEditor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open MQL Console", e);
        }
    }

    private void focusEditor(@NotNull MQLConsoleEditor editor) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile virtualFile = editor.getFile();
        if (virtualFile != null) {
            fileEditorManager.openFile(virtualFile, true);
        }
//        activateEditorToolWindow();
    }

    private void activateEditorToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow editorToolWindow = toolWindowManager.getToolWindow("Editor");
        if (editorToolWindow != null) {
            editorToolWindow.activate(null);
        }
    }

    public void closeMQLConsole() {
        MQLConsoleEditor editor = openEditors.remove(CONSOLE_EDITOR_ID);
        if (editor != null) {
            if (editor.getFile() != null) {
                FileEditorManager.getInstance(project).closeFile(editor.getFile());
            }
            editor.dispose();
        }
    }

    public void projectClosed() {
        closeMQLConsole();
    }

    public boolean isConsoleOpen() {
        return openEditors.containsKey(CONSOLE_EDITOR_ID);
    }
}
