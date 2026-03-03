package cn.github.spinner.action.basic;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MatrixDataViewFileType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ObjectBrowserAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        LightVirtualFile file = new LightVirtualFile("Matrix Object Browser");
        file.setFileType(MatrixDataViewFileType.OBJECT_BROWSER);
        FileEditorManager.getInstance(project).openFile(file, true);
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        EnvironmentConfig connectEnvironment = UserInput.getInstance().connectEnvironment.get(project);
        e.getPresentation().setEnabled(connectEnvironment != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
