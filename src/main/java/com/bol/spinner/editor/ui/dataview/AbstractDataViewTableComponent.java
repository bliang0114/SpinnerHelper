package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.customize.CellCopyTransferHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public abstract class AbstractDataViewTableComponent<T, E extends JBPanel<E>> extends JBPanel<E> {
    protected final Project project;
    protected final VirtualFile virtualFile;
    protected JBTable table;
    protected DefaultTableModel tableModel;
    protected SearchTextField searchTextField;
    protected DefaultActionGroup toolbarActionGroup;
    protected Object[] columns;
    protected int[] columnWidths;
    @Getter
    @Setter
    protected String name;
    protected String toolbarId;
    protected final List<T> rowList = new ArrayList<>();
    protected final ScheduledExecutorService executor;

    public AbstractDataViewTableComponent(@NotNull Project project, VirtualFile virtualFile, @NotNull Object[] columns, int @NotNull [] columnWidths, String toolbarId) {
        this.project = project;
        this.virtualFile = virtualFile;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.columns = columns;
        this.columnWidths = columnWidths;
        this.toolbarId = toolbarId;
        initComponents();
        setupListener();
        setupLayout();
    }

    public AbstractDataViewTableComponent(@NotNull Project project, VirtualFile virtualFile, @NotNull DefaultTableModel tableModel, int @NotNull [] columnWidths, String toolbarId) {
        this.project = project;
        this.virtualFile = virtualFile;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.tableModel = tableModel;
        this.columnWidths = columnWidths;
        this.toolbarId = toolbarId;
        initComponents();
        setupListener();
        setupLayout();
    }

    protected void initComponents() {
        if (this.tableModel == null) {
            this.tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return String.class;
                }
            };
        }
        table = new JBTable(tableModel);
        table.setTransferHandler(new CellCopyTransferHandler(table));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setBackground(JBColor.background());
        table.setForeground(JBColor.foreground());
        table.setShowGrid(true);
        table.setRowHeight(28);
        table.setGridColor(JBColor.border());
        table.setFont(Font.getFont(Font.MONOSPACED));

        searchTextField = new SearchTextField(true);
        searchTextField.setHistorySize(10);
        toolbarActionGroup = new DefaultActionGroup();
        toolbarActionGroup.add(new RefreshAction());
    }

    protected void setupListener() {
        searchTextField.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterData();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterData();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterData();
            }
        });
    }

    private void setupLayout() {
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        // 设置表头
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        setLayout(new BorderLayout());
        JComponent toolbarPanel = getToolbarComponent();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(toolbarId, toolbarActionGroup, true);
        toolbar.setTargetComponent(table);
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.EAST);

        add(toolbarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    protected JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        searchTextField.setPreferredSize(JBUI.size(300, 30));
        toolbarPanel.add(searchTextField, BorderLayout.WEST);
        return toolbarPanel;
    }

    @SuppressWarnings("unchecked")
    protected void filterData() {
        String text = searchTextField.getText();
        List<T> filterList;
        if (CharSequenceUtil.isNotEmpty(text)) {
            List<Function<T, String>> filterFunctions = getFilterFunctions();
            filterList = filter(rowList, text, filterFunctions.toArray(new Function[0]));
        } else {
            filterList = rowList;
        }
        tableModel.setRowCount(0);
        for (T row : filterList) {
            addRow(row);
        }
    }

    protected void reloadData() {
        rowList.clear();
        tableModel.setRowCount(0);
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null || CharSequenceUtil.isBlank(name)) {
            table.getEmptyText().setText("Connection is closed");
            return;
        }
        table.getEmptyText().setText("Loading Data...");
        executor.schedule(() -> {
            try {
                rowList.addAll(loadDataFromMatrix(connection));
                for (T row : rowList) {
                    addRow(row);
                }
                table.getEmptyText().setText("Nothing to show");
            } catch (MQLException e) {
                log.error(e.getLocalizedMessage(), e);
                table.getEmptyText().setText(e.getLocalizedMessage());
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    protected abstract List<Function<T, String>> getFilterFunctions();

    protected abstract void addRow(T row);

    protected abstract List<T> loadDataFromMatrix(MatrixConnection connection) throws MQLException;

    @SafeVarargs
    public static <T> List<T> filter(Collection<T> collection, String searchStr, Function<T, String>... extractors) {
        List<T> result = new ArrayList<>();
        // 空值处理
        if (collection == null || collection.isEmpty() || searchStr == null || extractors == null || extractors.length == 0) {
            return result;
        }
        for (T item : collection) {
            if (item == null) continue;
            // 检查所有指定的属性
            for (Function<T, String> extractor : extractors) {
                String value = extractor.apply(item);
                if (value != null && CharSequenceUtil.containsIgnoreCase(value, searchStr)) {
                    result.add(item);
                    break; // 找到一个匹配的属性就可以停止检查其他属性
                }
            }
        }
        return result;
    }

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            searchTextField.reset();
            searchTextField.setText("");
            reloadData();
        }
    }
}
