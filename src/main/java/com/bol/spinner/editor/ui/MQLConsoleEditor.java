package com.bol.spinner.editor.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class MQLConsoleEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile virtualFile;
    @Getter
    private Document document;
    private SimpleToolWindowPanel mainPanel;
    private Editor editor;

    public MQLConsoleEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        // 创建编辑器
        EditorFactory editorFactory = EditorFactory.getInstance();
        document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            document = editorFactory.createDocument("");
        }
        // 使用MQL文件类型创建编辑器
        this.editor = editorFactory.createEditor(document, project);
        EditorEx editorEx = (EditorEx) this.editor;
        // 配置编辑器设置
        configureEditorSettings(editorEx);
        // 创建带工具栏的面板
        createEditorWithToolbar();
    }

    private void createEditorWithToolbar() {
        this.mainPanel = new SimpleToolWindowPanel(true, true);
        // 创建工具栏
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("MQL Editor.Toolbar");
        ActionToolbar toolbar = actionManager.createActionToolbar("MQLEditorToolbar", actionGroup, true);
        toolbar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        toolbar.setTargetComponent(this.mainPanel);
        this.mainPanel.setToolbar(toolbar.getComponent());
        // 添加编辑器
        this.mainPanel.setContent(this.editor.getComponent());
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.editor.getComponent();
    }

    @NotNull
    @Override
    public String getName() {
        return "MQL Console";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        // 状态设置逻辑
    }

    @NotNull
    @Override
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // 属性变化监听
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        // 移除监听
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
        return virtualFile;
    }

    @Override
    public void dispose() {
        if (this.editor != null) {
            EditorFactory.getInstance().releaseEditor(this.editor);
            this.editor = null;
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private void configureEditorSettings(EditorEx editorEx) {
        EditorSettings settings = editorEx.getSettings();
        settings.setFoldingOutlineShown(true);
        settings.setLineNumbersShown(true);
        settings.setAutoCodeFoldingEnabled(true);
        settings.setRightMarginShown(true);
        settings.setRightMargin(100);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(true);
        settings.setAdditionalLinesCount(2);
        settings.setAdditionalColumnsCount(2);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setAnimatedScrolling(true);
        // 设置MQL语法高亮
        editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, virtualFile));
    }

    /**
     * 获取当前选中的文本内容
     *
     * @return {@link String}
     * @author zaydenwang
     */
    public String getSelectedText() {
        return editor.getSelectionModel().getSelectedText();
    }

    /**
     * 获取选中行的内容（支持多行选择）

     * @return {@link List}
     * @author xlwang
     */
    public List<String> getSelectedLines() {
        List<String> selectedLines = new ArrayList<>();
        SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.hasSelection()) {
            Document document = editor.getDocument();
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            int startLine = document.getLineNumber(startOffset);
            int endLine = document.getLineNumber(endOffset);
            for (int i = startLine; i <= endLine; i++) {
                String lineText = getLineContent(document, i);
                selectedLines.add(lineText);
            }
        }
        return selectedLines;
    }

    public List<String> getAllLines() {
        List<String> lines = new ArrayList<>();
        Document document = editor.getDocument();
        int lineCount = document.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            String line = getLineContent(document, i);
            lines.add(line);
        }
        return lines;
    }

    /**
     * 获取当前光标所在行的内容
     */
    public String getCurrentLine() {
        Document document = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        int currentLine = caretModel.getLogicalPosition().line;
        return getLineContent(document, currentLine);
    }

    private String getLineContent(Document document, int lineNumber) {
        try {
            int lineStart = document.getLineStartOffset(lineNumber);
            int lineEnd = document.getLineEndOffset(lineNumber);
            return document.getText().substring(lineStart, lineEnd).trim();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
