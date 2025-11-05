package cn.github.spinner.editor.spinner;

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

public class SpinnerDataViewEditor extends UserDataHolderBase implements FileEditor {
    private final Project project;
    private final VirtualFile virtualFile;
    private final JComponent editorComponent;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    public SpinnerDataViewEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        this.editorComponent = new AbstractSpinnerViewComponent(project, virtualFile) {};
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
        return "Spinner";
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
        return virtualFile;
    }

    @Override
    public void dispose() {
        if (editorComponent instanceof Disposable disposable && isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable);
        }
    }
}
