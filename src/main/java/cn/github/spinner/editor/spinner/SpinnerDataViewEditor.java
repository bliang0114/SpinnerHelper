package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.EnvironmentIndicator;
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
    private final VirtualFile virtualFile;
    private final JComponent editorComponent;
    private final JComponent rootComponent;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);

    public SpinnerDataViewEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
        this.editorComponent = new AbstractSpinnerViewComponent(project, virtualFile) {};
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        panel.add(new EnvironmentIndicator(project), java.awt.BorderLayout.NORTH);
        panel.add(editorComponent, java.awt.BorderLayout.CENTER);
        this.rootComponent = panel;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return rootComponent;
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
        return !isDisposed.get() && virtualFile.isValid();
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
    public void selectNotify() {
        if (editorComponent instanceof AbstractSpinnerViewComponent spinnerViewComponent && spinnerViewComponent.hasPendingRefresh()) {
            spinnerViewComponent.refreshFromDocument();
        }
    }

    @Override
    public void dispose() {
        if (editorComponent instanceof Disposable disposable && isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable);
        }
    }
}
