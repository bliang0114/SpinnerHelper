package cn.github.spinner.action.editor;

import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.ui.MQLEditorSettingsDialog;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

import java.util.Locale;
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
            spinnerSettings.setTimeoutMinutes(parsePositiveInt(dialog.getTimeoutMinutes().getText(), 10));
            spinnerSettings.setMqlResultMaxSizeMb(parsePositiveInt(dialog.getResultMaxSizeMb().getText(), 5));
            spinnerSettings.setAdminDefinitionsCachePath(dialog.getCachePath().getText().trim());
        }
    }

    private int parsePositiveInt(@NotNull String rawValue, int defaultValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("mb")) {
            normalized = normalized.substring(0, normalized.length() - 2).trim();
        } else if (normalized.endsWith("m")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            int value = Integer.parseInt(normalized);
            return Math.max(1, value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
