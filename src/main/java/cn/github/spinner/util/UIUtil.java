package cn.github.spinner.util;

import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.ui.SpinnerDataView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

public class UIUtil {

    public static EnvironmentToolWindow getEnvironmentToolWindow(Project project) {
        if (project == null) return null;

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SpinnerConfig");
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        if (content == null) return null;

        return (EnvironmentToolWindow) content.getComponent();
    }

    public static SpinnerDataView getSpinnerDataViewWindow(Project project) {
        if (project == null) return null;

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SpinnerDataView");
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        if (content == null) return null;

        return (SpinnerDataView) content.getComponent();
    }
    public static ToolWindow getToolWindow(Project project, String id) {
        if (project == null) return null;

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(id);
    }

    public static MQLExecutorToolWindow getMQLExecutorToolWindow(Project project) {
        if (project == null) return null;

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("MQLExecutor");
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        if (content == null) return null;

        return (MQLExecutorToolWindow) content.getComponent();
    }

    public static void showNotification(Project project, String title, String content) {
        Notification notification = new Notification("spinnerNotifier",
                title,
                content,
                NotificationType.INFORMATION
        );
        Notifications.Bus.notify(notification, project);
    }

    public static void showErrorNotification(Project project, String title, String content) {
        Notification notification = new Notification("spinnerNotifier",
                title,
                content,
                NotificationType.ERROR
        );
        Notifications.Bus.notify(notification, project);
    }

    public static void showWarningNotification(Project project, String title, String content) {
        Notification notification = new Notification("spinnerNotifier",
                title,
                content,
                NotificationType.WARNING
        );
        Notifications.Bus.notify(notification, project);
    }
}
