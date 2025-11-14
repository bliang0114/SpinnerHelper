package cn.github.spinner.editor.ui.dataview.details;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.util.MQLUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;

public class ObjectBasicInformationComponent extends JPanel {
    private final Project project;
    private final String id;
    private FilterTable table;
    private DefaultTableModel tableModel;
    @Setter
    protected boolean loaded = false;

    public ObjectBasicInformationComponent(Project project, String id) {
        this.project = project;
        this.id = id;
        initComponents();
        setupLayout();
    }

    private void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        configureFirstColumnAsHeader();

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
        ActionToolbar rightToolbar = ActionManager.getInstance().createActionToolbar("ObjectBasicInformationComponent.RightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(table);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    private void initComponents() {
        tableModel = new DefaultTableModel(new String[]{"", ""}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new FilterTable(tableModel);
    }

    private void configureFirstColumnAsHeader() {
        if (table.getColumnCount() == 0) return;

        table.setTableHeader(null);
        TableColumn headerColumn = table.getColumnModel().getColumn(0);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // 表头样式：加粗、背景色与 IDE 主题匹配
                setFont(getFont().deriveFont(Font.BOLD));
                setBorder(JBUI.Borders.compound(
                        JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 1),
                        JBUI.Borders.empty(2, 8)
                )); // 边框与内边距
                return this;
            }
        };
        headerColumn.setCellRenderer(headerRenderer);
    }

    private String formatAttributeName(String attributeName) {
        attributeName = attributeName.replaceAll(" {4}", "");
        attributeName = attributeName.replaceAll("^type$", "Type");
        attributeName = attributeName.replaceAll("^name$", "Name");
        attributeName = attributeName.replaceAll("^revision$", "Revision");
        attributeName = attributeName.replaceAll("^id$", "ID");
        attributeName = attributeName.replaceAll("^physicalid$", "PhysicalID");
        attributeName = attributeName.replaceAll("^description", "Description");
        attributeName = attributeName.replaceAll("^originated$", "Originated");
        attributeName = attributeName.replaceAll("^modified", "Modified");
        attributeName = attributeName.replaceAll("^lattice$", "Vault (lattice)");
        attributeName = attributeName.replaceAll("^policy$", "Policy");
        attributeName = attributeName.replaceAll("^current$", "State (current)");
        attributeName = attributeName.replaceAll("^owner$", "Owner");
        attributeName = attributeName.replaceAll("^organization$", "Organization");
        attributeName = attributeName.replaceAll("^project$", "Collaborative Space (project)");
        attributeName = attributeName.replaceAll("attribute\\[", "");
        attributeName = attributeName.replaceAll("].value", "");
        return attributeName;
    }

    public void reload() {
        if (loaded) return;

        tableModel.setRowCount(0);
        try {
            String result = MQLUtil.execute(project, "print bus {} select type name revision id physicalid description originated modified lattice policy current owner organization project attribute.value", id);
            String[] array = result.split("\n");
            for (int i = 1; i < array.length; i++) {
                String item = array[i];
                String[] attribute = item.split(" = ");
                String attributeName = formatAttributeName(attribute[0]);
                String attributeValue = attribute.length > 1 ? attribute[1] : "";
                tableModel.addRow(new String[]{attributeName, attributeValue});
            }
        } catch (Exception e) {
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
