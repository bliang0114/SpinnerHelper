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
        setToolTipText(null);
        if (value instanceof DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();
            if (userObject instanceof MQLExecutionEntry entry) {
                setIcon(entry.success() ? AllIcons.General.InspectionsOK : AllIcons.General.Error);
                setToolTipText(entry.success() ? null : entry.message());
                append("L" + (entry.lineNumber() + 1) + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                append(entry.command(), entry.success() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.ERROR_ATTRIBUTES);
                return;
            }

            String nodeName = String.valueOf(userObject);
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
