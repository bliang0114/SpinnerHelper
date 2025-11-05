package cn.github.spinner.execution;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class MQLExecutorToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ApplicationManager.getApplication().invokeLater(() -> {
            MQLExecutorToolWindow resultPanel = new MQLExecutorToolWindow(project);
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(resultPanel, "", false);
            toolWindow.getContentManager().addContent(content);
        });
    }
}
