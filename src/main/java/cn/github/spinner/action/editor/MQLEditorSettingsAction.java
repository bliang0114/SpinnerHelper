package cn.github.spinner.action.editor;

import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.ui.MQLEditorSettingsDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MQLEditorSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        MQLEditorSettingsDialog dialog = new MQLEditorSettingsDialog(project);
        if (dialog.showAndGet()) {
            SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
            spinnerSettings.setKeepMQLExecuteHistory(dialog.getKeepExecHistory().isSelected());
            spinnerSettings.setLineDelimiter(dialog.getLineDelimiter().getText());
        }
    }
}
