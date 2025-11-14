package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.components.FilterTable;
import cn.github.spinner.util.MQLUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;

public class ObjectHistoryComponent extends JPanel {
    private static final String[] COLUMNS = {"Date", "User", "Action", "State", "Description"};
    private final Project project;
    private final String id;
    private FilterTable table;
    private DefaultTableModel tableModel;
    @Setter
    protected boolean loaded = false;

    public ObjectHistoryComponent(Project project, String id) {
        this.project = project;
        this.id = id;
        initComponents();
        setupLayout();
    }

    private void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(600);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        setLayout(new BorderLayout());
        add(getToolbarComponent(), BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        JPanel componentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        componentPanel.add(table.getFilterComponent());
        toolbarPanel.add(componentPanel, BorderLayout.WEST);
        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(new RefreshAction());
        ActionToolbar rightToolbar = ActionManager.getInstance().createActionToolbar("ObjectHistoryComponent.RightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(table);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    private void initComponents() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new FilterTable(tableModel);
    }

    public void reload() {
        if (loaded) return;

        tableModel.setRowCount(0);
        try {
            String result = MQLUtil.execute(project, "print bus {} select history.time history.user history.event history.state history.description dump |", id);
            String[] array = result.split("\\|");
            int groupCount = array.length / 5;
            for (int i = 0; i < groupCount; i++) {
                String time = array[i];
                String user = array[i + groupCount];
                String action = array[i + 2 * groupCount];
                String state = array[i + 3 * groupCount];
                String description = array[i + 4 * groupCount];
                tableModel.addRow(new String[]{time, user, action, state, description});
            }
        } catch (MQLException e) {
            table.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
        }
        loaded = true;
    }

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            table.getFilterComponent().reset();
            loaded = false;
            reload();
        }
    }
}
