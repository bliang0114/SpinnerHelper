package com.bol.spinner.editor.ui;

import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.RelationshipDataViewComponent;
import com.bol.spinner.editor.ui.dataview.TypeDataViewComponent;
import com.bol.spinner.ui.FormsAndTablesView;
import com.bol.spinner.editor.ui.dataview.ProgramTableComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class MatrixCommonViewEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile virtualFile;
    private final JComponent editorComponent;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    public MatrixCommonViewEditor(@NotNull Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        switch (fileType.getViewType()) {
            case RELATIONSHIP:
                this.editorComponent = new RelationshipDataViewComponent(virtualFile);
                break;
            case TYPE:
                this.editorComponent = new TypeDataViewComponent(project, virtualFile);
                break;
            case PROGRAM:
                this.editorComponent = new ProgramTableComponent(project, virtualFile);
                break;
            case FORM_TABLE:
                this.editorComponent = new FormsAndTablesView(project, virtualFile);
                break;
            default:
                this.editorComponent = new JPanel();
                break;
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return editorComponent;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return editorComponent;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return virtualFile.getName();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {

    }

    @Override
    public VirtualFile getFile() {
        return this.virtualFile;
    }

    @Override
    public void dispose() {
        if (editorComponent instanceof Disposable disposable && isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable);
        }
    }
}
