package cn.github.spinner.editor.ui;

import cn.github.spinner.editor.MatrixDataViewFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class MatrixCommonViewProvider implements FileEditorProvider, DumbAware {

    private static final String EDITOR_TYPE_ID = "matrix.common.editor";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return file.getFileType() instanceof MatrixDataViewFileType;
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new MatrixCommonViewEditor(project, file);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @NotNull
    @Override
    public com.intellij.openapi.fileEditor.FileEditorPolicy getPolicy() {
        return com.intellij.openapi.fileEditor.FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

}