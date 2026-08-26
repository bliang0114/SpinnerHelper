package cn.github.spinner.components;

import cn.github.spinner.customize.CellCopyTransferHandler;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class FilterTable extends JBTable {
    private final TableRowSorter<TableModel> sorter;
    @Getter
    private final FilterComponent filterComponent;
    private final ColumnFilterHeader columnFilterHeader;

    public FilterTable() {
        this(new DefaultTableModel());
    }

    public FilterTable(TableModel model) {
        super(model);
        sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);
        filterComponent = new FilterComponent("TABLE_FILTER_HISTORY", 10) {
            @Override
            public void filter() {
                applyFilters();
            }
        };
        filterComponent.reset();
        filterComponent.setPreferredSize(JBUI.size(300, 30));
        filterComponent.setToolTipText("Filter all columns");
        // 设置表头
        initFont();
//        JBFont font = JBUI.Fonts.create("JetBrains Mono", 14);
        JTableHeader header = getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
//        header.setFont(font);
        setTransferHandler(new CellCopyTransferHandler(this));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBackground(JBColor.background());
        setForeground(JBColor.foreground());
        setShowGrid(true);
        setRowHeight(28);
        setGridColor(JBColor.border());
        columnFilterHeader = new ColumnFilterHeader(header);
//        setFont(font);
    }

    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        Container parent = getParent();
        if (parent instanceof JViewport viewport && viewport.getParent() instanceof JScrollPane scrollPane) {
            columnFilterHeader.attachTableHeader();
            scrollPane.setColumnHeaderView(columnFilterHeader);
        }
    }

    @Override
    protected void unconfigureEnclosingScrollPane() {
        Container parent = getParent();
        if (parent instanceof JViewport viewport && viewport.getParent() instanceof JScrollPane scrollPane
                && scrollPane.getColumnHeader() != null
                && scrollPane.getColumnHeader().getView() == columnFilterHeader) {
            scrollPane.setColumnHeaderView(null);
        }
        super.unconfigureEnclosingScrollPane();
    }

    private void applyFilters() {
        Map<Integer, Set<String>> columnFilters = columnFilterHeader == null
                ? Map.of()
                : columnFilterHeader.getSelectedValues();
        TableContentFilter.apply(sorter, filterComponent.getFilter(), columnFilters);
    }

    @Override
    public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
        TableModel model = this.getModel();
        Component c = super.prepareRenderer(renderer, row, column);
        if (column == 0 && model instanceof RowNumberTableModel) { // 行号列特殊处理
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.LEFT);
                if (isRowSelected(row)) {
                    label.setBackground(getSelectionBackground());
                    label.setForeground(getSelectionForeground());
                } else {
                    label.setBackground(row % 2 == 0 ? UIUtil.getTableBackground() : UIUtil.getDecoratedRowColor());
                    label.setForeground(UIUtil.getLabelForeground());
                }
            }
        }
        return c;
    }

    private void initFont() {
        // 优先使用 JetBrains Mono 显示英文/数字/符号，中文自动 fallback 到系统字体
        Font codeFont = new Font("JetBrains Mono", Font.PLAIN, 12);
        // 验证中文支持（JetBrains Mono 会返回 false，触发系统字体 fallback）
        if (!codeFont.canDisplay('中')) {
            // 手动指定中文备用字体（适配不同系统）
            String systemFontName = getSystemDefaultChineseFont();
            Font mixedFont = new Font(systemFontName, Font.PLAIN, 12);
            setFont(mixedFont);
            getTableHeader().setFont(mixedFont.deriveFont(Font.BOLD));
        } else {
            setFont(codeFont);
            getTableHeader().setFont(codeFont.deriveFont(Font.BOLD));
        }
    }

    // 获取系统默认中文字体（适配 Windows/macOS/Linux）
    private String getSystemDefaultChineseFont() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "Microsoft YaHei"; // Windows 系统
        } else if (os.contains("mac")) {
            return "PingFang SC"; // macOS 系统
        } else {
            return "Noto Sans CJK SC"; // Linux 系统（需安装思源黑体）
        }
    }

    private final class ColumnFilterHeader extends JPanel implements TableColumnModelListener {
        private static final int BUTTON_SIZE = 18;
        private static final int BUTTON_GAP = 3;
        private static final int POPUP_WIDTH = 300;
        private static final int POPUP_MAX_HEIGHT = 360;

        private final JTableHeader tableHeader;
        private final Map<TableColumn, JToggleButton> buttons = new LinkedHashMap<>();
        private final Map<Integer, Set<String>> selectedValues = new LinkedHashMap<>();

        private ColumnFilterHeader(JTableHeader tableHeader) {
            this.tableHeader = tableHeader;
            setLayout(null);
            setOpaque(true);
            setBackground(JBColor.background());
            tableHeader.getColumnModel().addColumnModelListener(this);
            rebuildButtons();
            attachTableHeader();
        }

        private void attachTableHeader() {
            if (tableHeader.getParent() != this) {
                add(tableHeader);
            }
        }

        private void rebuildButtons() {
            for (JToggleButton button : buttons.values()) {
                remove(button);
            }
            buttons.clear();

            TableColumnModel columnModel = tableHeader.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                TableColumn column = columnModel.getColumn(i);
                int modelIndex = column.getModelIndex();
                String columnName = getModel().getColumnName(modelIndex);
                if (columnName.isBlank()) {
                    continue;
                }

                JToggleButton button = new JToggleButton(AllIcons.General.Filter);
                button.setFocusable(false);
                button.setMargin(JBUI.emptyInsets());
                button.setBorder(JBUI.Borders.empty());
                button.setToolTipText(SpinnerBundle.message("tooltip.table.column.filter", columnName));
                button.addActionListener(e -> {
                    button.setSelected(selectedValues.containsKey(modelIndex));
                    showFilterPopup(column, button);
                });
                buttons.put(column, button);
                add(button, 0);
            }
            updateButtonStates();
            revalidate();
            repaint();
        }

        private Map<Integer, Set<String>> getSelectedValues() {
            Map<Integer, Set<String>> result = new LinkedHashMap<>();
            for (Map.Entry<Integer, Set<String>> entry : selectedValues.entrySet()) {
                result.put(entry.getKey(), Set.copyOf(entry.getValue()));
            }
            return result;
        }

        private void showFilterPopup(TableColumn column, JToggleButton button) {
            int modelIndex = column.getModelIndex();
            List<String> values = collectColumnValues(modelIndex);
            Set<String> activeSelection = selectedValues.get(modelIndex);

            JPanel valuesPanel = new JPanel();
            valuesPanel.setLayout(new BoxLayout(valuesPanel, BoxLayout.Y_AXIS));
            Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();
            for (String value : values) {
                JCheckBox checkBox = new JCheckBox(displayValue(value),
                        activeSelection == null || activeSelection.contains(value));
                checkBox.setToolTipText(displayValue(value));
                checkBox.addActionListener(e -> updateSelection(modelIndex, values, checkBoxes));
                checkBoxes.put(value, checkBox);
                valuesPanel.add(checkBox);
            }

            JBTextField searchField = new JBTextField();
            searchField.getEmptyText().setText(SpinnerBundle.message("placeholder.table.column.filter.search"));
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                private void updateVisibleValues() {
                    String searchText = searchField.getText().trim().toLowerCase(Locale.ROOT);
                    for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
                        entry.getValue().setVisible(entry.getKey().toLowerCase(Locale.ROOT).contains(searchText));
                    }
                    valuesPanel.revalidate();
                    valuesPanel.repaint();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateVisibleValues();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateVisibleValues();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateVisibleValues();
                }
            });

            JButton selectAllButton = new JButton(SpinnerBundle.message("button.select.all"));
            selectAllButton.addActionListener(e -> {
                checkBoxes.values().forEach(checkBox -> checkBox.setSelected(true));
                updateSelection(modelIndex, values, checkBoxes);
            });
            JButton clearButton = new JButton(SpinnerBundle.message("button.clear"));
            clearButton.addActionListener(e -> {
                checkBoxes.values().forEach(checkBox -> checkBox.setSelected(false));
                updateSelection(modelIndex, values, checkBoxes);
            });

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(4), 0));
            actionPanel.add(selectAllButton);
            actionPanel.add(clearButton);

            JBScrollPane scrollPane = new JBScrollPane(valuesPanel);
            scrollPane.setBorder(JBUI.Borders.empty());
            int visibleRowsHeight = Math.min(POPUP_MAX_HEIGHT - 70,
                    Math.max(JBUI.scale(80), values.size() * JBUI.scale(24)));
            scrollPane.setPreferredSize(JBUI.size(POPUP_WIDTH, visibleRowsHeight));

            JPanel content = new JPanel(new BorderLayout(JBUI.scale(4), JBUI.scale(4)));
            content.setBorder(JBUI.Borders.empty(6));
            content.add(searchField, BorderLayout.NORTH);
            content.add(scrollPane, BorderLayout.CENTER);
            content.add(actionPanel, BorderLayout.SOUTH);

            JPopupMenu popup = new JPopupMenu();
            popup.setBorder(JBUI.Borders.customLine(JBColor.border()));
            popup.add(content);
            popup.show(button, 0, button.getHeight());
            SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        }

        private List<String> collectColumnValues(int modelIndex) {
            Set<String> values = new TreeSet<>((left, right) -> {
                int caseInsensitiveResult = String.CASE_INSENSITIVE_ORDER.compare(left, right);
                return caseInsensitiveResult != 0 ? caseInsensitiveResult : left.compareTo(right);
            });
            TableModel model = getModel();
            for (int row = 0; row < model.getRowCount(); row++) {
                values.add(Objects.toString(model.getValueAt(row, modelIndex), ""));
            }
            return new ArrayList<>(values);
        }

        private String displayValue(String value) {
            return value.isEmpty() ? SpinnerBundle.message("label.blank.value") : value;
        }

        private void updateSelection(int modelIndex, List<String> allValues,
                                     Map<String, JCheckBox> checkBoxes) {
            Set<String> selection = new LinkedHashSet<>();
            for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selection.add(entry.getKey());
                }
            }

            if (selection.size() == allValues.size()) {
                selectedValues.remove(modelIndex);
            } else {
                selectedValues.put(modelIndex, selection);
            }
            updateButtonStates();
            applyFilters();
        }

        private void updateButtonStates() {
            for (Map.Entry<TableColumn, JToggleButton> entry : buttons.entrySet()) {
                entry.getValue().setSelected(selectedValues.containsKey(entry.getKey().getModelIndex()));
            }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension headerSize = tableHeader.getPreferredSize();
            int tableWidth = Math.max(tableHeader.getColumnModel().getTotalColumnWidth(),
                    FilterTable.this.getPreferredSize().width);
            return new Dimension(tableWidth, headerSize.height);
        }

        @Override
        public void doLayout() {
            int headerHeight = tableHeader.getPreferredSize().height;
            int columnsWidth = tableHeader.getColumnModel().getTotalColumnWidth();
            tableHeader.setBounds(0, 0, Math.max(getWidth(), columnsWidth), headerHeight);

            int x = 0;
            TableColumnModel columnModel = tableHeader.getColumnModel();
            for (int i = 0; i < columnModel.getColumnCount(); i++) {
                TableColumn column = columnModel.getColumn(i);
                int width = column.getWidth();
                JToggleButton button = buttons.get(column);
                if (button != null) {
                    button.setBounds(x + Math.max(BUTTON_GAP, width - BUTTON_SIZE - BUTTON_GAP),
                            Math.max(0, (headerHeight - BUTTON_SIZE) / 2), BUTTON_SIZE, BUTTON_SIZE);
                }
                x += width;
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {
            rebuildButtons();
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {
            rebuildButtons();
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            revalidate();
            repaint();
        }

        @Override
        public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
            revalidate();
            repaint();
        }

        @Override
        public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {
        }
    }
}
