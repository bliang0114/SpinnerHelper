package cn.github.spinner.editor.spinner;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePreCloseCheck;
import org.jetbrains.annotations.NotNull;

/** Native file-close and normal project/IDE-exit vetoes. */
public final class SpinnerDraftCloseCheck implements VirtualFilePreCloseCheck, ProjectCloseHandler {
    @Override
    public boolean canCloseFile(@NotNull VirtualFile file) {
        var document = FileDocumentManager.getInstance().getCachedDocument(file);
        var draft = document == null ? null : SpinnerFileDraft.find(document);
        return draft == null || draft.views.isEmpty() || draft.views.getFirst().prepareToLeave();
    }

    @Override
    public boolean canClose(@NotNull Project project) {
        for (var editor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (editor instanceof SpinnerDataViewEditor spinner && !spinner.prepareToLeave()) return false;
        }
        var recovery = project.getServiceIfCreated(SpinnerDraftCloseRecovery.class);
        return recovery == null || recovery.prepareToClose();
    }
}
