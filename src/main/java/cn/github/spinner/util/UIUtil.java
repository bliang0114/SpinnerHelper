package cn.github.spinner.util;

import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.ui.EnvironmentToolWindow;
import cn.github.spinner.ui.SpinnerDataView;
import cn.hutool.core.util.NumberUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    public static void applyProfessionalFilter(TableRowSorter<TableModel> sorter, String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        try {
            // 支持高级筛选语法
            if (filterText.contains(":")) {
                // 按列筛选 例如: "name:test type:file"
                applyColumnSpecificFilter(sorter, filterText);
            } else {
                // 全局筛选
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
            }
        } catch (Exception e) {
            // 筛选语法错误时使用全局筛选
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
            } catch (PatternSyntaxException ex) {
                sorter.setRowFilter(null);
            }
        }
    }

    public static void applyColumnSpecificFilter(TableRowSorter<TableModel> sorter, String filterText) {
        String[] conditions = filterText.split("\\s+");
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        for (String condition : conditions) {
            String[] parts = condition.split(":", 2);
            if (parts.length == 2) {
                String columnIndexStr = parts[0].trim();
                String value = parts[1].trim();
                // 查找列索引
                int columnIndex = NumberUtil.isInteger(columnIndexStr) ? Integer.parseInt(columnIndexStr) : 1;
                if (columnIndex >= 0 && !value.isEmpty()) {
                    RowFilter<Object, Object> filter = RowFilter.regexFilter("(?i)" + Pattern.quote(value), columnIndex);
                    filters.add(filter);
                }
            }
        }
        if (!filters.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        } else {
            sorter.setRowFilter(null);
        }
    }
}
