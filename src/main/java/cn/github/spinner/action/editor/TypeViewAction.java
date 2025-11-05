package cn.github.spinner.action.editor;

import cn.github.spinner.editor.MatrixDataViewFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class TypeViewAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 创建一个虚拟文件用于标识编辑器
        LightVirtualFile file = new LightVirtualFile("Type View");
        file.setFileType(MatrixDataViewFileType.TYPE);
        FileEditorManager.getInstance(project).openFile(file, true);
    }
}
