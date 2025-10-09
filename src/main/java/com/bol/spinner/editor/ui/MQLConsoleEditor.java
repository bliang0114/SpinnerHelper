package com.bol.spinner.editor.ui;

import com.bol.spinner.editor.MQLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class MQLConsoleEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile virtualFile;
    private Editor editor;

    public MQLConsoleEditor(@NotNull Project project) {
        this.project = project;
        this.virtualFile = createVirtualFile();
        // 创建编辑器
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        // 使用MQL文件类型创建编辑器
        this.editor = editorFactory.createEditor(document, project);
        EditorEx editorEx = (EditorEx) this.editor;
        // 配置编辑器设置
        configureEditorSettings(editorEx);
        // 设置MQL语法高亮
        setupSyntaxHighlighting(editorEx);
    }

    private VirtualFile createVirtualFile() {
        // 创建一个虚拟文件用于标识编辑器
        LightVirtualFile file = new LightVirtualFile("MQL Console");
        file.setLanguage(MQLLanguage.INSTANCE);
        file.setWritable(true);
        file.setCharset(null);
        return file;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return new JPanel();
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
    }

    private void setupSyntaxHighlighting(EditorEx editorEx) {
        editorEx.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, virtualFile));
    }
}
