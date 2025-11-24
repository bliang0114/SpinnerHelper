package cn.github.spinner.action.editor;

import cn.github.spinner.ui.MQLFileSaveAsDialog;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SaveAsForMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        MQLFileSaveAsDialog dialog = new MQLFileSaveAsDialog(project);
        if (dialog.showAndGet()) {
            String name = dialog.getNameTextField().getText().trim();
            String path = dialog.getDirTextField().getText().trim();
            if (CharSequenceUtil.isNotBlank(name) && CharSequenceUtil.isNotBlank(path) && FileUtil.isDirectory(path)) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    FileUtil.writeString(editor.getDocument().getText(), path + File.separator + name + ".mql", editor.getVirtualFile().getCharset());
                    UIUtil.showNotification(project, "MQL Console", "Save MQL file successfully");
                });
            }
        }
    }
}
