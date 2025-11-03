package com.bol.spinner.editor.spinner;

import cn.hutool.core.io.FileUtil;
import com.bol.spinner.customize.CellCopyTransferHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

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
            int modelRowIndex = table.convertRowIndexToModel(selectedRow);
            JComponent component = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this)
                    .setProject(project).build(headers, tableModel.getDataVector().get(modelRowIndex));
            recordPane.setComponentAt(0, component);
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
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        return toolbarPanel;
    }

    protected void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (int i = 1; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(240);
        }
//        TableColumnAutoResizer.autoResizeAllColumns(table);
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
