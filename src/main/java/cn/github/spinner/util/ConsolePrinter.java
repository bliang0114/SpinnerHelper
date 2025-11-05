package cn.github.spinner.util;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;

public class ConsolePrinter {
    private final ConsoleView consoleView;
    private final Project project;

    public ConsolePrinter(Project project, ConsoleView consoleView) {
        this.project = project;
        this.consoleView = consoleView;
    }

    public void print(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 确保在EDT线程中执行UI操作
            ConsoleViewContentType contentType = ConsoleViewContentType.NORMAL_OUTPUT;
            consoleView.print(message + "\n", contentType);
            // 自动滚动到底部
            scrollToBottom();
        });
    }

    public void print(String message, ConsoleViewContentType contentType) {
        ApplicationManager.getApplication().invokeLater(() -> {
            consoleView.print(message + "\n", contentType);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        JComponent component = consoleView.getComponent();
        if (component instanceof JScrollPane scrollPane) {
            JViewport viewport = scrollPane.getViewport();
            JViewport realViewport = findRealViewport(viewport);
            if (realViewport != null) {
                JComponent view = (JComponent) realViewport.getView();
                Rectangle bounds = view.getBounds();
                realViewport.setViewPosition(new Point(0, bounds.height));
            }
        }
    }

    private JViewport findRealViewport(JViewport viewport) {
        if (viewport.getView() instanceof JScrollPane innerScrollPane) {
            return innerScrollPane.getViewport();
        }
        return viewport;
    }

    public void clear() {
        ApplicationManager.getApplication().invokeLater(() -> {
            consoleView.clear();
        });
    }
}
