package cn.github.spinner.editor.ui.dataview.details;

import cn.github.spinner.components.FilterTable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

@Slf4j
public abstract class AbstractObjectDetailsTableComponent extends JPanel {
    protected final Project project;
    protected final String id;
    protected FilterTable table;
    protected DefaultTableModel tableModel;
    @Setter
    protected boolean loaded = false;

    public AbstractObjectDetailsTableComponent(Project project, String id) {
        this.project = project;
        this.id = id;
        initComponents();
        setupListener();
        setupLayout();
    }

    protected abstract String[] headers();

    protected abstract int[] columnWidths();

    protected Class<?>[] columnTypes() {
        int[] widths = columnWidths();
        Class<?>[] types = new Class<?>[widths.length];
        for (int i = 0; i < widths.length; i++) {
            types[i] = String.class;
        }
        return types;
    }

    protected abstract String componentId();

    protected void initComponents() {
        tableModel = new DefaultTableModel(headers(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                Class<?>[] types = columnTypes();
                return types[columnIndex];
            }
        };
        table = new FilterTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    protected void setupListener() {};

    protected JComponent getToolbarComponent() {
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
        ActionToolbar rightToolbar = ActionManager.getInstance().createActionToolbar(componentId() + ".RightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(table);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    protected void setupLayout() {
        int[] widths = columnWidths();
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        setLayout(new BorderLayout());
        add(getToolbarComponent(), BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void reload() {
        if (loaded) return;

        loadData();
        loaded = true;
    }

    protected abstract void loadData();

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
