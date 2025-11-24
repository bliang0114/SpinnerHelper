package cn.github.spinner.execution;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class ConsoleTreeCellRenderer extends ColoredTreeCellRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value,
                                      boolean selected, boolean expanded,
                                      boolean leaf, int row, boolean hasFocus) {
        if (value instanceof DefaultMutableTreeNode node) {
            String nodeName = String.valueOf(node.getUserObject());
            if ("Consoles".equals(nodeName)) {
                setIcon(AllIcons.Nodes.Folder);
                append(nodeName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else {
                setIcon(AllIcons.Nodes.Method);
                append(nodeName);
            }
        }
    }
}
