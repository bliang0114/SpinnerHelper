package com.bol.spinner.util;

import com.bol.spinner.editor.ui.MQLConsoleEditor;
import com.bol.spinner.ui.EnvironmentToolWindow;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;

public class UIUtil {

    public static EnvironmentToolWindow getToolWindow(Project project) {
        if (project == null) return null;

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SpinnerConfig");
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        if (content == null) return null;

        return (EnvironmentToolWindow) content.getComponent();
    }

    public static MQLConsoleEditor getMQLEditor(Project project) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor activeEditor = editorManager.getSelectedEditor();

        // 直接检查类型
        if (activeEditor instanceof MQLConsoleEditor mqlEditor) {
            return mqlEditor;
        }

        // 如果没有找到，尝试从所有编辑器中查找
        VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
        for (VirtualFile file : selectedFiles) {
            FileEditor[] editors = editorManager.getAllEditors(file);
            for (FileEditor editor : editors) {
                if (editor instanceof MQLConsoleEditor mqlEditor) {
                    return mqlEditor;
                }
            }
        }
        return null;
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
