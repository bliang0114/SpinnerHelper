package com.bol.spinner.action;

import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.ui.DependenciesSettingDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DependenciesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        DependenciesSettingDialog dialog = new DependenciesSettingDialog(project);
        if (dialog.showAndGet()) {
            List<VirtualFile> selectedJars = dialog.getSelectedJars();
            SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
            spinnerSettings.setDependencies(selectedJars.stream().map(VirtualFile::getPath).toList());
        }
    }
}
