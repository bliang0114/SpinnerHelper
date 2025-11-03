package com.bol.spinner.editor.spinner;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import com.bol.spinner.customize.CellCopyTransferHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
public abstract class AbstractSpinnerViewComponent extends JPanel {
    protected final Project project;
    protected final VirtualFile virtualFile;
    protected JBTable table;
    protected DefaultTableModel tableModel;
    protected DefaultActionGroup toolbarActionGroup;
    protected RecordPaneVisibleAction recordPaneVisibleAction;
    protected JBTabbedPane recordPane;
    protected String[] headers;
    private TableRowSorter<TableModel> sorter;
    private FilterComponent filterComponent;
    protected final List<String[]> dataList = new ArrayList<>();

    public AbstractSpinnerViewComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        try {
            readFile();
            initComponents();
            setupListener();
            setupLayout();
            setValue();
        } catch (Exception e) {
            table = new JBTable();
            table.getEmptyText().setText(e.getMessage());
            add(table);
        }
    }

    protected void initComponents() {
        tableModel = new DefaultTableModel(headers, 0){
            @Override
            public int getColumnCount() {
                return super.getColumnCount() + 1;
            }

            @Override
            public String getColumnName(int column) {
                if (column == 0) {
                    return "";
                }
                return super.getColumnName(column - 1);
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (column == 0) {
                    return row + 1;
                }
                return super.getValueAt(row, column - 1);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JBTable(tableModel) {
            @Override
            public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (column == 0) { // 行号列特殊处理
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
        };
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        // 创建筛选组件
        filterComponent = new FilterComponent("TABLE_FILTER_HISTORY", 10) {
            @Override
            public void filter() {
                applyProfessionalFilter();
            }
        };
        filterComponent.reset();
        recordPane = new JBTabbedPane();
        recordPane.add("Record", new JPanel());
        toolbarActionGroup = new DefaultActionGroup();
        recordPaneVisibleAction = new RecordPaneVisibleAction();
        toolbarActionGroup.add(recordPaneVisibleAction);
    }

    protected void setupListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        recordPane.setVisible(true);
                    }
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;

            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) return;

            int modelRowIndex = table.convertRowIndexToModel(selectedRow);
            if (modelRowIndex < 0) return;
            JComponent component = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this)
                    .setProject(project).build(headers, tableModel.getDataVector().get(modelRowIndex));
            recordPane.setComponentAt(0, component);
        });
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        assert document != null;
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                tableModel.setRowCount(0);
                dataList.clear();
                String text = event.getDocument().getText();
                if (text.contains("\n")) {
                    List<String> lines = CharSequenceUtil.split(text, "\n");
                    if (!lines.isEmpty()) {
                        lines.removeFirst();
                    }
                    dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
                    setValue();
                    repaint();
                }
            }
        });
    }

    protected JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner Data View.Toolbar", toolbarActionGroup, true);
        toolbar.setTargetComponent(table);
        filterComponent.setPreferredSize(JBUI.size(260, filterComponent.getHeight()));
        toolbarPanel.add(filterComponent, BorderLayout.WEST);
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
        return toolbarPanel;
    }

    protected void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (int i = 1; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(240);
        }
        // 设置表头
        JBFont font = JBUI.Fonts.create("JetBrains Mono", 14);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
        header.setFont(font);
        table.setTransferHandler(new CellCopyTransferHandler(table));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setBackground(JBColor.background());
        table.setForeground(JBColor.foreground());
        table.setShowGrid(true);
        table.setRowHeight(28);
        table.setGridColor(JBColor.border());
        table.setFont(font);
        recordPane.setPreferredSize(JBUI.size(300, -1));
        recordPane.setVisible(false);

        setLayout(new BorderLayout());
        JComponent toolbarPanel = getToolbarComponent();
        add(toolbarPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JBSplitter splitter = new JBSplitter();
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(recordPane);
        add(splitter, BorderLayout.CENTER);
    }

    protected void setValue() {
        for (String[] row : dataList) {
            tableModel.addRow(row);
        }
    }

    public void reloadValue(int rowIndex, String line) {
        int columnCount = tableModel.getColumnCount();
        String[] values = line.split("\t");
        dataList.set(rowIndex, values);
        for (int i = 0; i < columnCount; i++) {
            String value = i >= values.length ? "" : values[i];
            tableModel.setValueAt(value, rowIndex, i);
        }
    }

    protected void readFile() throws Exception {
        String extension = this.virtualFile.getExtension();
        if (!"xls".equals(extension)) {
            throw new Exception("Error: invalid spinner file");
        }
        List<String> lines;
        try {
            lines = FileUtil.readLines(virtualFile.getPath(), virtualFile.getCharset());
            if (lines == null || lines.isEmpty()) {
                throw new Exception("Error: file is empty");
            }
        } catch (Exception e) {
            throw new Exception("Error: error while reading file");
        }
        String header = lines.getFirst();
        headers = header.split("\t");
        lines.removeFirst();
        dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
    }

    private void applyProfessionalFilter() {
        String filterText = filterComponent.getFilter();
        if (filterText == null || filterText.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        try {
            // 支持高级筛选语法
            if (filterText.contains(":")) {
                // 按列筛选 例如: "name:test type:file"
                applyColumnSpecificFilter(filterText);
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

    private void applyColumnSpecificFilter(String filterText) {
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

    private int findColumnIndex(String columnName) {
        TableModel model = table.getModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    public class RecordPaneVisibleAction extends ToggleAction {
        public RecordPaneVisibleAction() {
            super("Show / Hide Record View", "Show / Hide Record View", AllIcons.Nodes.Record);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            recordPane.setVisible(!recordPane.isVisible());
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return recordPane.isVisible();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean b) {
            recordPane.setVisible(b);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }
}
