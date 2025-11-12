package cn.github.spinner.components;

import cn.github.spinner.components.bean.TableRowBean;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PaginatedFilterTableComponent<T extends TableRowBean> extends JPanel {
    private final String componentId;
    private final String PLACE_PAGINATION = "pagination";
    @Getter
    protected FilterTable table;
    protected DefaultTableModel tableModel;
    protected Object[] columns;
    protected int[] columnWidths;
    protected Class<?>[] columnTypes;
    protected List<T> tableData = new ArrayList<>();
    // 分页配置
    protected int currentPage = 1;
    protected int pageSize = 100;
    private PageSizeActionGroup pageSizeActionGroup;
    private TotalSizeAction totalSizeAction;

    public PaginatedFilterTableComponent(@NotNull T entity, String componentId) {
        this.columns = entity.headers();
        this.columnWidths = entity.widths();
        this.columnTypes = entity.columnTypes();
        this.componentId = componentId;
        initComponents();
        setupListener();
        setupLayout();
    }

    protected void initComponents() {
        tableModel = new PaginatedTableModel(columns, 0);
        table = new FilterTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        pageSizeActionGroup = new PageSizeActionGroup();
        totalSizeAction = new TotalSizeAction();
    }

    protected void setupListener() {
    }

    /**
     * 创建分页控件
     */
    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new HomePageAction());
        actionGroup.add(new PrevPageAction());
        actionGroup.add(pageSizeActionGroup);
        actionGroup.add(totalSizeAction);
        actionGroup.add(new NextPageAction());
        actionGroup.add(new LastPageAction());
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PLACE_PAGINATION, actionGroup, true);
        panel.setBackground(JBColor.WHITE);
        toolbar.setTargetComponent(this);
        panel.add(toolbar.getComponent());
        return panel;
    }

    protected Component[] createToolbarComponent() {
        return new Component[] {table.getFilterComponent()};
    }

    protected AnAction[] createLeftToolbarAction() {
        return new AnAction[] {};
    }

    protected AnAction[] createRightToolbarAction() {
        return new AnAction[] {};
    }

    private JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        JPanel componentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        Component[] components = createToolbarComponent();
        for (Component component : components) {
            componentPanel.add(component);
        }
        DefaultActionGroup leftActionGroup = new DefaultActionGroup();
        leftActionGroup.addAll(createLeftToolbarAction());
        ActionToolbar leftToolbar = ActionManager.getInstance().createActionToolbar(componentId + ".LeftToolbar", leftActionGroup, true);
        leftToolbar.setTargetComponent(table);
        componentPanel.add(leftToolbar.getComponent());
        toolbarPanel.add(componentPanel, BorderLayout.WEST);

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.addAll(createRightToolbarAction());
        ActionToolbar rightToolbar = ActionManager.getInstance().createActionToolbar(componentId + ".RightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(table);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    private void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (int i = 1; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i - 1]);
        }
        setLayout(new BorderLayout());
        JComponent toolbarPanel = getToolbarComponent();
        add(toolbarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        JPanel paginationPanel = createPaginationPanel();
        add(paginationPanel, BorderLayout.SOUTH);
    }

    /**
     * 刷新表格数据（显示当前页内容）
     */
    private void refreshTableData() {
//        pageSizeActionGroup.setText(pageDataTitle());
        pageSizeActionGroup.updateText();
        totalSizeAction.setText(" of " + tableData.size());
        tableModel.setRowCount(0); // 清空表格
        int total = tableData.size();
        int start = pageSize == 0 ? 0 : (currentPage - 1) * pageSize;
        int end = pageSize == 0 ? total : Math.min(start + pageSize, total);
        // 反射获取实体字段值，添加到表格
        for (int i = start; i < end; i++) {
            T entity = tableData.get(i);
            tableModel.addRow(entity.rowValues());
        }
    }

    public void setTableData(List<T> entities) {
        this.tableData = new ArrayList<>(entities);
        this.currentPage = 1;
        refreshTableData();
    }

    private String pageDataTitle() {
        if (this.pageSize == 0) {
            return "1 - " + this.tableData.size();
        }
        int startRowIndex = 1 + (this.currentPage - 1) * this.pageSize;
        int endRowIndex = this.currentPage * this.pageSize;
        endRowIndex = Math.min(endRowIndex, this.tableData.size());
        return startRowIndex + " - " + endRowIndex;
    }

    public class HomePageAction extends AnAction {
        public HomePageAction() {
            super("Home Page", "Home Page", AllIcons.Actions.Play_first);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            currentPage = 1;
            refreshTableData();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean enabled = currentPage != 1 && !tableData.isEmpty() && pageSize != 0;
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class PrevPageAction extends AnAction {
        public PrevPageAction() {
            super("Previous Page", "Previous Page", AllIcons.Actions.Play_back);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            currentPage = currentPage - 1;
            refreshTableData();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean enabled = currentPage != 1 && !tableData.isEmpty() && pageSize != 0;
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class NextPageAction extends AnAction {
        public NextPageAction() {
            super("Next Page", "Next Page", AllIcons.Actions.Play_forward);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            currentPage = currentPage + 1;
            refreshTableData();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean enabled = currentPage * pageSize < tableData.size() && !tableData.isEmpty() && pageSize != 0;
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class LastPageAction extends AnAction {
        public LastPageAction() {
            super("Last Page", "Last Page", AllIcons.Actions.Play_last);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            int endPage = tableData.size() / pageSize;
            endPage = tableData.size() % pageSize == 0 ? endPage : endPage + 1;
            currentPage = endPage;
            refreshTableData();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            boolean enabled = currentPage * pageSize < tableData.size() && !tableData.isEmpty() && pageSize != 0;
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class PageSizeAction extends AnAction {
        private final int pageSize;

        public PageSizeAction(int pageSize) {
            super(String.valueOf(pageSize == 0 ? "All" : pageSize));
            this.pageSize = pageSize;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            PaginatedFilterTableComponent.this.currentPage = 1;
            PaginatedFilterTableComponent.this.pageSize = pageSize;
            refreshTableData();
        }
    }

    public class PageSizeActionGroup extends ComboBoxAction {
        private ComboBoxButton comboBoxButton;

        @Override
        protected @NotNull DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
            DefaultActionGroup group = new DefaultActionGroup();
            group.add(new PageSizeAction(100));
            group.add(new PageSizeAction(200));
            group.add(new PageSizeAction(500));
            group.add(new PageSizeAction(1000));
            group.add(new PageSizeAction(2000));
            group.add(new PageSizeAction(0));
            return group;
        }

        @Override
        protected @NotNull ComboBoxButton createComboBoxButton(@NotNull Presentation presentation) {
            comboBoxButton = super.createComboBoxButton(presentation);
            return comboBoxButton;
        }

        public void updateText() {
            String text = pageDataTitle();
            comboBoxButton.getPresentation().setText(text);
            SwingUtilities.invokeLater(() -> {
                comboBoxButton.updateUI();
                comboBoxButton.repaint();
            });
        }
    }

    public class TotalSizeAction extends AnAction implements CustomComponentAction {
        private String text;
        private JBLabel label;

        public TotalSizeAction() {
            this.text = "";
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
        }

        @Override
        public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
            label = new JBLabel(text);
            return label;
        }

        public void setText(String newText) {
            if (newText.equals(text)) return; // 文本未变化则不更新
            text = newText;

            // 确保在 UI 线程中更新组件（IDEA 要求所有 UI 操作必须在 EDT 线程执行）
            SwingUtilities.invokeLater(() -> {
                if (label != null) {
                    label.setText(text);
                    label.repaint(); // 强制重绘，避免文本未刷新
                }
            });
        }
    }

    public class PaginatedTableModel extends DefaultTableModel {

        public PaginatedTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

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
                if (PaginatedFilterTableComponent.this.pageSize == 0) {
                    return row + 1;
                } else {
                    return row + 1 + (PaginatedFilterTableComponent.this.currentPage - 1) * PaginatedFilterTableComponent.this.pageSize;
                }
            }
            return super.getValueAt(row, column - 1);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            }
            return columnTypes[columnIndex - 1];
        }
    }
}
