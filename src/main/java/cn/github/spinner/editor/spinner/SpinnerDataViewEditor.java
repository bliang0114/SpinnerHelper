package cn.github.spinner.editor.spinner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpinnerDataViewEditor extends UserDataHolderBase implements FileEditor {
    private final VirtualFile virtualFile;
    private final JComponent editorComponent;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private final MessageBusConnection connection;

    public SpinnerDataViewEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
        this.editorComponent = new AbstractSpinnerViewComponent(project, virtualFile) {};
        this.connection = project.getMessageBus().connect();
        this.connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                if (event.getNewEditor() == SpinnerDataViewEditor.this &&
                        editorComponent instanceof AbstractSpinnerViewComponent spinnerViewComponent &&
                        spinnerViewComponent.hasPendingRefresh()) {
                    SwingUtilities.invokeLater(spinnerViewComponent::refreshFromDocument);
                }
            }
        });
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
    public void selectNotify() {
        if (editorComponent instanceof AbstractSpinnerViewComponent spinnerViewComponent && spinnerViewComponent.hasPendingRefresh()) {
            SwingUtilities.invokeLater(spinnerViewComponent::refreshFromDocument);
        }
    }

    @Override
    public void dispose() {
        connection.disconnect();
        if (editorComponent instanceof Disposable disposable && isDisposed.compareAndSet(false, true)) {
            Disposer.dispose(disposable);
        }
    }
}
