package com.bol.spinner.ui;

import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.SpinnerToken;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredText;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class EnvironmentTreeCellRenderer extends ColoredTreeCellRenderer {
    private final Project project;

    public EnvironmentTreeCellRenderer(Project project) {
        this.project = project;
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
                String environmentName = SpinnerToken.getEnvironmentName(project);
                if (CharSequenceUtil.isNotEmpty(environmentName)) {
                    append(ColoredText.singleFragment(" - [" + environmentName + "]", SimpleTextAttributes.ERROR_ATTRIBUTES));
                }
            }
        }
    }
}
