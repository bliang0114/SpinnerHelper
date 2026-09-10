package cn.github.spinner.execution;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MQLResultTabbedPane extends JBTabbedPane {
    private static final int ROW_NUMBER_COLUMN_WIDTH = 48;
    private static final int FORM_FIELD_COLUMN_WIDTH = 240;
    private static final int FORM_VALUE_COLUMN_WIDTH = 520;
    private static final int TABLE_COLUMN_WIDTH = 140;
    private final JComponent defaultView;

    public MQLResultTabbedPane(@NotNull JComponent defaultView,
                               @NotNull MQLResultViewData initialData) {
        this.defaultView = createFillContainer(defaultView);
        setTabPlacement(JTabbedPane.TOP);
        addTab(tabTitle(MQLResultViewData.Style.DEFAULT), this.defaultView);
        updateResult(initialData);
    }

    private static @NotNull JComponent createFillContainer(@NotNull JComponent component) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(JBUI.Borders.empty());
        container.setMinimumSize(JBUI.emptySize());
        container.add(component, BorderLayout.CENTER);
        return container;
    }

    public void updateResult(@NotNull MQLResultViewData data) {
        MQLResultViewData.Style selectedStyle = selectedStyle();
        if (data.style() == MQLResultViewData.Style.DEFAULT) {
            removeStructuredTab();
        } else {
            JComponent structuredContent = switch (data.style()) {
                case FORM -> createFormView(data.rows());
                case TABLE -> createTableView(data);
                case TREE_TABLE -> createTreeTableView(data);
                case DEFAULT -> throw new IllegalStateException("Default view has no structured content");
            };
            JComponent structuredView = createFillContainer(structuredContent);
            if (getTabCount() == 1) {
                addTab(tabTitle(data.style()), structuredView);
            } else {
                setTitleAt(1, tabTitle(data.style()));
                setComponentAt(1, structuredView);
            }
        }
        selectStyle(selectedStyle == data.style() ? selectedStyle : MQLResultViewData.Style.DEFAULT);
        revalidate();
        repaint();
    }

    private void removeStructuredTab() {
        if (getTabCount() > 1) {
            removeTabAt(1);
        }
    }

    private @NotNull MQLResultViewData.Style selectedStyle() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) {
            return MQLResultViewData.Style.DEFAULT;
        }
        String title = getTitleAt(selectedIndex);
        for (MQLResultViewData.Style style : MQLResultViewData.Style.values()) {
            if (tabTitle(style).equals(title)) {
                return style;
            }
        }
        return MQLResultViewData.Style.DEFAULT;
    }

    private void selectStyle(@NotNull MQLResultViewData.Style style) {
        String title = tabTitle(style);
        for (int i = 0; i < getTabCount(); i++) {
            if (title.equals(getTitleAt(i))) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(0);
    }

    private @NotNull JComponent createFormView(@NotNull List<List<String>> rows) {
        return createFilteredTableView(
                List.of(
                        SpinnerBundle.message("table.column.result.field"),
                        SpinnerBundle.message("table.column.value")
                ),
                rows,
                List.of(FORM_FIELD_COLUMN_WIDTH, FORM_VALUE_COLUMN_WIDTH)
        );
    }

    private @NotNull JComponent createTableView(@NotNull MQLResultViewData data) {
        List<String> columns = data.columns().isEmpty()
                ? genericColumns(data.rows().getFirst().size())
                : data.columns();
        return createFilteredTableView(columns, data.rows(), java.util.Collections.nCopies(columns.size(), TABLE_COLUMN_WIDTH));
    }

    private @NotNull JComponent createTreeTableView(@NotNull MQLResultViewData data) {
        int columnCount = data.rows().getFirst().size();
        List<String> columns = data.columns().isEmpty() ? genericColumns(columnCount) : data.columns();
        ExpandTreeNode root = buildExpandTree(data.rows());
        List<Integer> columnOrder = treeColumnOrder(columnCount);
        ColumnInfo<ExpandTreeNode, String>[] columnInfos = createTreeColumns(columns, columnOrder);
        TreeTable treeTable = new TreeTable(new ListTreeTableModelOnColumns(root, columnInfos));
        treeTable.setRootVisible(false);
        treeTable.getTree().setShowsRootHandles(true);
        treeTable.setRowHeight(28);
        treeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        treeTable.setFillsViewportHeight(true);
        treeTable.setShowGrid(true);
        treeTable.getTableHeader().setReorderingAllowed(false);
        configureTreeColumnWidths(treeTable, columnOrder);
        expandAll(treeTable);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(treeTable);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private @NotNull ExpandTreeNode buildExpandTree(@NotNull List<List<String>> rows) {
        ExpandTreeNode root = new ExpandTreeNode(List.of());
        List<ExpandTreeNode> path = new ArrayList<>();
        for (List<String> row : rows) {
            int level = parseLevel(row.getFirst());
            while (path.size() >= level) {
                path.removeLast();
            }
            ExpandTreeNode parent = path.isEmpty() ? root : path.getLast();
            ExpandTreeNode node = new ExpandTreeNode(row);
            parent.add(node);
            path.add(node);
        }
        return root;
    }

    private int parseLevel(@NotNull String value) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private @NotNull List<Integer> treeColumnOrder(int columnCount) {
        Set<Integer> orderedIndexes = new LinkedHashSet<>();
        if (columnCount > 4) {
            orderedIndexes.add(4); // Name is the expandable tree column.
        }
        for (int index : List.of(0, 1, 2, 3, 5)) {
            if (index < columnCount) {
                orderedIndexes.add(index);
            }
        }
        for (int index = 6; index < columnCount; index++) {
            orderedIndexes.add(index);
        }
        return List.copyOf(orderedIndexes);
    }

    @SuppressWarnings("unchecked")
    private @NotNull ColumnInfo<ExpandTreeNode, String>[] createTreeColumns(@NotNull List<String> columns,
                                                                            @NotNull List<Integer> columnOrder) {
        ColumnInfo<ExpandTreeNode, String>[] result = new ColumnInfo[columnOrder.size()];
        for (int displayIndex = 0; displayIndex < columnOrder.size(); displayIndex++) {
            int dataIndex = columnOrder.get(displayIndex);
            boolean treeColumn = displayIndex == 0;
            result[displayIndex] = new ColumnInfo<>(columns.get(dataIndex)) {
                @Override
                public String valueOf(ExpandTreeNode node) {
                    return node.valueAt(dataIndex);
                }

                @Override
                public Class<?> getColumnClass() {
                    return treeColumn ? TreeTableModel.class : String.class;
                }
            };
        }
        return result;
    }

    private void configureTreeColumnWidths(@NotNull TreeTable treeTable, @NotNull List<Integer> columnOrder) {
        for (int displayIndex = 0; displayIndex < columnOrder.size(); displayIndex++) {
            int dataIndex = columnOrder.get(displayIndex);
            int width = dataIndex == 4 ? 320
                    : dataIndex == 0 ? 72
                    : dataIndex == 2 ? 90
                    : dataIndex == 5 ? 100
                    : dataIndex < 6 ? 160
                    : TABLE_COLUMN_WIDTH;
            treeTable.getColumnModel().getColumn(displayIndex).setPreferredWidth(JBUI.scale(width));
        }
    }

    private void expandAll(@NotNull TreeTable treeTable) {
        for (int row = 0; row < treeTable.getTree().getRowCount(); row++) {
            treeTable.getTree().expandRow(row);
        }
    }

    private @NotNull JComponent createFilteredTableView(@NotNull List<String> columns,
                                                         @NotNull List<List<String>> rows,
                                                         @NotNull List<Integer> columnWidths) {
        RowNumberTableModel model = new RowNumberTableModel(columns.toArray(), 0);
        for (List<String> row : rows) {
            model.addRow(row.toArray());
        }

        FilterTable table = new FilterTable(model);
        table.setAutoResizeMode(columns.size() <= 8 ? JTable.AUTO_RESIZE_LAST_COLUMN : JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMinWidth(JBUI.scale(ROW_NUMBER_COLUMN_WIDTH));
        table.getColumnModel().getColumn(0).setMaxWidth(JBUI.scale(ROW_NUMBER_COLUMN_WIDTH));
        table.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(ROW_NUMBER_COLUMN_WIDTH));
        for (int i = 0; i < columnWidths.size(); i++) {
            table.getColumnModel().getColumn(i + 1).setPreferredWidth(JBUI.scale(columnWidths.get(i)));
        }

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, com.intellij.ui.JBColor.border()));
        toolbar.add(table.getFilterComponent(), BorderLayout.WEST);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(JBUI.Borders.empty());
        content.add(toolbar, BorderLayout.NORTH);
        content.add(scrollPane, BorderLayout.CENTER);
        return content;
    }

    private @NotNull List<String> genericColumns(int columnCount) {
        return java.util.stream.IntStream.range(0, columnCount)
                .mapToObj(index -> SpinnerBundle.message("table.column.result.generic", index + 1))
                .toList();
    }

    private @NotNull String tabTitle(@NotNull MQLResultViewData.Style style) {
        return switch (style) {
            case DEFAULT -> SpinnerBundle.message("tab.result.default");
            case FORM -> SpinnerBundle.message("tab.result.form");
            case TABLE -> SpinnerBundle.message("tab.result.table");
            case TREE_TABLE -> SpinnerBundle.message("tab.result.tree.table");
        };
    }

    private static final class ExpandTreeNode extends DefaultMutableTreeNode {
        private final List<String> values;

        private ExpandTreeNode(@NotNull List<String> values) {
            super(values.size() > 4 ? values.get(4) : "");
            this.values = values;
        }

        private @NotNull String valueAt(int index) {
            return index < values.size() ? values.get(index) : "";
        }
    }

}
