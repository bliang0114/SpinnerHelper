package com.bol.spinner.provider;

import com.bol.spinner.ui.ProgramView;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ProgramViewProvider implements FileEditorProvider, DumbAware { // 实现 DumbAware 接口

    private static final String EDITOR_TYPE_ID = "spinner.program.editor";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        // 通过文件名判断是否由这个Provider处理
        return "ProgramConfiguration".equals(file.getName());
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new ProgramView(project, file);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @NotNull
    @Override
    public com.intellij.openapi.fileEditor.FileEditorPolicy getPolicy() {
        // 现在可以安全使用 HIDE_DEFAULT_EDITOR
        return com.intellij.openapi.fileEditor.FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

}