package com.bol.spinner.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public class SpinnerNotifier {
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
