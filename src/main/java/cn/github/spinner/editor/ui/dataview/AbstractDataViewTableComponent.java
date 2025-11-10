package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.customize.CellCopyTransferHandler;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractDataViewTableComponent<T, E extends JBPanel<E>> extends JBPanel<E> {
    protected final Project project;
    protected final VirtualFile virtualFile;
    protected FilterTable table;
    protected DefaultTableModel tableModel;
    protected DefaultActionGroup actionGroup;
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
            this.tableModel = new RowNumberTableModel(columns, 0);
        }
        table = new FilterTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        actionGroup = new DefaultActionGroup();
    }

    protected void setupListener() {
    }

    protected Component[] createToolbarComponent() {
        return new Component[] {table.getFilterComponent()};
    }

    protected AnAction[] createToolbarAction() {
        return new AnAction[] {new RefreshAction()};
    }

    private JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        Component[] components = createToolbarComponent();
        for (Component component : components) {
            toolbarPanel.add(component);
        }
        AnAction[] actions = createToolbarAction();
        actionGroup.addAll(actions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(toolbarId, actionGroup, true);
        toolbar.setTargetComponent(table);
        toolbarPanel.add(toolbar.getComponent());
        return toolbarPanel;
    }

    private void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (int i = 1; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        setLayout(new BorderLayout());
        JComponent toolbarPanel = getToolbarComponent();
        add(toolbarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
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

    protected abstract void addRow(T row);

    protected abstract List<T> loadDataFromMatrix(MatrixConnection connection) throws MQLException;

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            table.getFilterComponent().reset();
            reloadData();
        }
    }
}
