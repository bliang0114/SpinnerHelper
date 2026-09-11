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
    private final VirtualFile virtualFile;
    private final Project project;
    private JComponent editorComponent;
    private final JComponent rootComponent;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private com.intellij.ui.tabs.JBTabs guardedTabs;
    private final java.beans.PropertyChangeSupport changes = new java.beans.PropertyChangeSupport(this);
    boolean retainOnClose;

    public SpinnerDataViewEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.virtualFile = virtualFile;
        this.project = project;
        var retained = project.getService(SpinnerDraftCloseRecovery.class).take(virtualFile);
        this.editorComponent = retained != null ? retained : new AbstractSpinnerViewComponent(project, virtualFile) {};
        ((AbstractSpinnerViewComponent) editorComponent).onDraftStateChanged(
                () -> changes.firePropertyChange(FileEditor.getPropModified(), null, isModified()));
        JPanel panel = new JPanel(new java.awt.BorderLayout());
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
        return !isDisposed.get() && ((AbstractSpinnerViewComponent) editorComponent).hasDraft();
    }

    @Override
    public boolean isValid() {
        return !isDisposed.get() && virtualFile.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
        changes.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener) {
        changes.removePropertyChangeListener(propertyChangeListener);
    }

    @Override
    public VirtualFile getFile() {
        return virtualFile;
    }

    @Override
    public void selectNotify() {
        installSelectionGuard();
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (!isDisposed.get()) installSelectionGuard();
        });
        if (editorComponent instanceof AbstractSpinnerViewComponent spinnerViewComponent && spinnerViewComponent.hasPendingRefresh()) {
            spinnerViewComponent.refreshFromDocument();
        }
    }

    boolean prepareToLeave() {
        return ((AbstractSpinnerViewComponent) editorComponent).prepareToLeave();
    }

    void restoreView(AbstractSpinnerViewComponent retained) {
        rootComponent.remove(editorComponent);
        Disposer.dispose((Disposable) editorComponent);
        editorComponent = retained;
        retained.onDraftStateChanged(() -> changes.firePropertyChange(FileEditor.getPropModified(), null, isModified()));
        rootComponent.add(retained, java.awt.BorderLayout.CENTER);
        rootComponent.revalidate();
        rootComponent.repaint();
    }

    private void installSelectionGuard() {
        var manager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project);
        if (!(manager instanceof com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl impl)) return;
        var composite = impl.getComposite(this);
        if (composite == null || composite.getTabs() == null || guardedTabs == composite.getTabs()) return;
        guardSelection(composite.getTabs(), () -> composite.getSelectedEditor() == this);
    }

    void guardSelection(com.intellij.ui.tabs.JBTabs tabs, java.util.function.BooleanSupplier selected) {
        guardedTabs = tabs;
        guardedTabs.setSelectionChangeHandler((target, focus, select) -> {
            if (selected.getAsBoolean() && tabs.getSelectedInfo() != target && !prepareToLeave()) {
                return com.intellij.openapi.util.ActionCallback.REJECTED;
            }
            return select.run();
        });
    }

    @Override
    public void dispose() {
        if (guardedTabs != null) guardedTabs.setSelectionChangeHandler((target, focus, select) -> select.run());
        if (editorComponent instanceof Disposable disposable && isDisposed.compareAndSet(false, true)) {
            if (retainOnClose) {
                ((AbstractSpinnerViewComponent) editorComponent).onDraftStateChanged(() -> {});
                project.getService(SpinnerDraftCloseRecovery.class).retain(virtualFile, (AbstractSpinnerViewComponent) editorComponent);
            } else Disposer.dispose(disposable);
        }
    }
}
