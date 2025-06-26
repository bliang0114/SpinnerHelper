package com.bol.spinner.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SpinnerUIFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LoginForm loginForm = new LoginForm(project, toolWindow);
        JPanel loginPanel = loginForm.getMainPanel();
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(loginPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
