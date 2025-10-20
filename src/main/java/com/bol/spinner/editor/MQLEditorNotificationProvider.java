package com.bol.spinner.editor;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class MQLEditorNotificationProvider implements EditorNotificationProvider {

    @Override
    public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(
            @NotNull Project project,
            @NotNull VirtualFile file) {
        return editor -> {
            // 只在特定文件类型显示工具栏
            if (!file.getFileType().equals(MQLFileType.INSTANCE)) {
                return null;
            }
            return createToolbar(editor);
        };
    }

    private JComponent createToolbar(FileEditor editor) {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        // 创建工具栏
        ActionManager actionManager = ActionManager.getInstance();
        AnAction runAction = actionManager.getAction("MQL Editor.Run");
        AnAction settingsAction = actionManager.getAction("MQL Editor.Settings");
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(runAction);
        ActionToolbar toolbar = actionManager.createActionToolbar("MQLEditorToolbar", actionGroup, true);
        toolbar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        toolbar.setTargetComponent(editor.getPreferredFocusedComponent());

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(settingsAction);
        ActionToolbar rightToolbar = actionManager.createActionToolbar("MQLEditorRightToolbar", rightActionGroup, true);
        rightToolbar.setLayoutStrategy(ToolbarLayoutStrategy.NOWRAP_STRATEGY);
        rightToolbar.setTargetComponent(editor.getPreferredFocusedComponent());

        toolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }
}