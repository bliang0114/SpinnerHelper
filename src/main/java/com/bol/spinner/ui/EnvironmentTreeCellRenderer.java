package com.bol.spinner.ui;

import com.bol.spinner.config.EnvironmentConfig;
import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class EnvironmentTreeCellRenderer extends ColoredTreeCellRenderer {

    private final EnvironmentToolWindow toolWindow;

    public EnvironmentTreeCellRenderer(EnvironmentToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value,
                                      boolean selected, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();
            if (node instanceof EnvironmentTreeNode envNode) {
                setIcon(AllIcons.Nodes.Folder);
                append(envNode.getEnvironment().getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else if (node instanceof DetailTreeNode detailNode) {
                // 详细信息节点
                setIcon(AllIcons.Nodes.Property);
                // 分开显示键和值
                append(detailNode.getKey() + ": ", SimpleTextAttributes.GRAY_ATTRIBUTES);
                append(detailNode.getValue(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else if (userObject instanceof String) {
                // 普通字符串节点
                setIcon(AllIcons.Nodes.AbstractClass);
                append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                EnvironmentConfig environment = toolWindow.getEnvironment();
                if (environment != null && environment.isConnected()) {
                    append("\t - [" + environment.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }
        }
    }
}
