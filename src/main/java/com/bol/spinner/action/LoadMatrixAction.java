package com.bol.spinner.action;

import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.task.Connect3DETask;
import com.bol.spinner.util.MatrixJarLoadManager;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class LoadMatrixAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<SpinnerSettings.Dependency> dependencies = spinnerSettings.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            UIUtil.showWarningNotification(project, "Spinner Config", "未设置3DE依赖");
            return;
        }
        List<File> fileList = dependencies.stream().map(dependency -> new File(dependency.getPath())).toList();
        MatrixJarLoadManager.loadMatrixJars(fileList, UIUtil.class.getClassLoader());
        UIUtil.showNotification(project, "Spinner Config", "成功加载3DE依赖\n" + fileList.stream().map(File::getName).collect(Collectors.joining()));
    }
}
