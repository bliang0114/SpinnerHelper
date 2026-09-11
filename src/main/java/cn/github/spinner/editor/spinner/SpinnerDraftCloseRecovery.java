package cn.github.spinner.editor.spinner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/** 251 CloseTab bypasses VirtualFilePreCloseCheck; retain the exact view when that close is cancelled. */
@Service(Service.Level.PROJECT)
public final class SpinnerDraftCloseRecovery implements Disposable {
    private final Project project;
    private final Map<VirtualFile, AbstractSpinnerViewComponent> retained = new HashMap<>();

    public SpinnerDraftCloseRecovery(Project project) {
        this.project = project;
        var connection = project.getMessageBus().connect(this);
        connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (!file.isValid() || new SpinnerDraftCloseCheck().canCloseFile(file)) return;
                for (var editor : source.getAllEditors(file)) {
                    if (editor instanceof SpinnerDataViewEditor spinner) spinner.retainOnClose = true;
                }
            }
        });
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                for (var editor : source.getAllEditors(file)) {
                    if (editor instanceof SpinnerDataViewEditor spinner) spinner.retainOnClose = false;
                }
                if (!retained.containsKey(file)) return;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed() || !file.isValid() || !retained.containsKey(file)) return;
                    // A different split can still own this file, so opening it need not create a provider.
                    for (var editor : source.getAllEditors(file)) {
                        if (editor instanceof SpinnerDataViewEditor spinner && spinner.isValid()) {
                            spinner.restoreView(take(file));
                            break;
                        }
                    }
                    source.openFile(file, true);
                    source.setSelectedEditor(file, "spinner.editor");
                }, com.intellij.openapi.application.ModalityState.any(), project.getDisposed());
            }
        });
    }

    AbstractSpinnerViewComponent take(VirtualFile file) { return retained.remove(file); }

    boolean prepareToClose() {
        for (var view : java.util.List.copyOf(retained.values())) {
            if (!view.prepareToLeave()) return false;
        }
        return true;
    }

    void retain(VirtualFile file, AbstractSpinnerViewComponent view) {
        var previous = retained.put(file, view);
        if (previous != null && previous != view) Disposer.dispose(previous);
    }

    @Override
    public void dispose() {
        retained.values().forEach(Disposer::dispose);
        retained.clear();
    }
}
