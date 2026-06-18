package cn.github.spinner.action.editor;

import cn.github.spinner.ui.MQLFileSaveAsDialog;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.Charset;

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
                String content = editor.getDocument().getText();
                Charset charset = editor.getVirtualFile().getCharset();
                File targetFile = new File(path, name + ".mql");
                new TrackedBackgroundTask(project, SpinnerBundle.message("progress.save.mql.file"), false) {
                    @Override
                    protected void runTracked(@NotNull ProgressIndicator indicator) {
                        try {
                            FileUtil.writeString(content, targetFile, charset);
                            UIUtil.showNotification(project, SpinnerBundle.message("notification.title.mql.console"), SpinnerBundle.message("message.save.mql.success"));
                        } catch (Exception ex) {
                            UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.mql.console"), SpinnerBundle.message("message.save.mql.failed", ex.getLocalizedMessage()));
                        }
                    }
                }.queue();
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
