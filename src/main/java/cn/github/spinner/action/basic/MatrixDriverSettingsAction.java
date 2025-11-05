package cn.github.spinner.action.basic;

import cn.github.spinner.ui.MatrixDriversDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MatrixDriverSettingsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        MatrixDriversDialog dialog = new MatrixDriversDialog(project);
        dialog.show();
    }
}
