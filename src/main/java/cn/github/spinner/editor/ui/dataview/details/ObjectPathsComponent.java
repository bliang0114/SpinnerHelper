package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.customize.CellCopyTransferHandler;
import cn.github.spinner.util.MQLUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ObjectPathsComponent extends JPanel {
    private static final String[] BUS_HEADERS = {"Direction", "Path Type", "Path ID", "Type", "Name", "Revision", "Object ID", "Object PhysicalID", "Description", "Originated", "Modified", "Owner", "Policy", "State", "Vault"};
    private static final int[] BUS_COLUMN_WIDTHS = {120, 240, 300, 240, 240, 120, 300, 300, 300, 240, 240, 240, 240, 180, 240};
    private static final String[] CONNECTION_HEADERS = {"Direction", "Path Type", "Path ID", "Target Relationship", "Target Connection ID", "Target Connection PhysicalID", "Originated", "Modified"};
    private static final int[] CONNECTION_COLUMN_WIDTHS = {120, 240, 300, 240, 300, 300, 240, 240};

    private final Project project;
    private final String id;
    private JBTable busTable;
    private DefaultTableModel busTableModel;
    private TableRowSorter<TableModel> busRowSorter;
    private JBTable connectionTable;
    private DefaultTableModel connectionTableModel;
    private TableRowSorter<TableModel> connectionRowSorter;
    private FilterComponent filterComponent;
    @Setter
    private boolean loaded = false;

    public ObjectPathsComponent(Project project, String id) {
        this.project = project;
        this.id = id;
        initComponents();
        setupListener();
        setupLayout();
    }

    private void initComponents() {
        initBusTable();
        initConnectionTable();
        filterComponent = new FilterComponent("ObjectPaths_TABLE_FILTER_HISTORY", 10) {
            @Override
            public void filter() {
                String filterText = filterComponent.getFilter();
                UIUtil.applyProfessionalFilter(busRowSorter, filterText);
                UIUtil.applyProfessionalFilter(connectionRowSorter, filterText);
            }
        };
        filterComponent.reset();
        filterComponent.setPreferredSize(JBUI.size(300, 30));
    }

    private void initBusTable() {
        busTableModel = createTableModel(BUS_HEADERS);
        busRowSorter = new TableRowSorter<>();
        busTable = new JBTable();
        initTable(busTableModel, busTable, busRowSorter);
    }

    private void initConnectionTable() {
        connectionTableModel = createTableModel(CONNECTION_HEADERS);
        connectionRowSorter = new TableRowSorter<>();
        connectionTable = new JBTable();
        initTable(connectionTableModel, connectionTable, connectionRowSorter);
    }

    private void initTable(DefaultTableModel tableModel, JBTable table, TableRowSorter<TableModel> rowSorter) {
        rowSorter.setModel(tableModel);
        table.setModel(tableModel);
        table.setRowSorter(rowSorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        var header = table.getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
        table.setTransferHandler(new CellCopyTransferHandler(table));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(JBColor.background());
        table.setForeground(JBColor.foreground());
        table.setShowGrid(true);
        table.setRowHeight(28);
        table.setGridColor(JBColor.border());
    }

    private DefaultTableModel createTableModel(String[] headers) {
        return new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 统一设置单元格不可编辑
            }
        };
    }

    private JComponent getToolbarComponent() {
        var toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        var componentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        componentPanel.add(filterComponent);
        toolbarPanel.add(componentPanel, BorderLayout.WEST);
        var rightActionGroup = new DefaultActionGroup();
        rightActionGroup.add(new RefreshAction());
        var rightToolbar = ActionManager.getInstance().createActionToolbar("ObjectPathsComponent.RightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(this);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    private void setupListener() {
        busTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = busTable.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = busTable.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0) return;

                        int columnIndex = busTable.columnAtPoint(e.getPoint());
                        if (columnIndex > 2) {
                            String id = String.valueOf(busTableModel.getValueAt(modelRowIndex, 6));
                            ObjectDetailsWindow.showWindow(project, id);
                        }
                    }
                }
            }
        });
        connectionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = connectionTable.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = connectionTable.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0) return;

                        int columnIndex = connectionTable.columnAtPoint(e.getPoint());
                        if (columnIndex > 2) {
                            String id = String.valueOf(connectionTableModel.getValueAt(modelRowIndex, 4));
                            ConnectionDetailsWindow.showWindow(project, id);
                        }
                    }
                }
            }
        });
    }

    private void setupLayout() {
        for (var i = 0; i < BUS_COLUMN_WIDTHS.length; i++) {
            busTable.getColumnModel().getColumn(i).setPreferredWidth(BUS_COLUMN_WIDTHS[i]);
        }
        for (var i = 0; i < CONNECTION_COLUMN_WIDTHS.length; i++) {
            connectionTable.getColumnModel().getColumn(i).setPreferredWidth(CONNECTION_COLUMN_WIDTHS[i]);
        }
        setLayout(new BorderLayout());
        add(getToolbarComponent(), BorderLayout.NORTH);

        var splitter = new JBSplitter();
        var busScrollPane = ScrollPaneFactory.createScrollPane(busTable);
        busScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        busScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        splitter.setFirstComponent(busScrollPane);
        var connectionScrollPane = ScrollPaneFactory.createScrollPane(connectionTable);
        connectionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        connectionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        splitter.setSecondComponent(connectionScrollPane);
        splitter.setOrientation(true);
        splitter.setProportion(0.5f);

        add(splitter, BorderLayout.CENTER);
    }

    public void reload() {
        if (loaded) return;

        loadData();
        loaded = true;
    }

    private void loadData() {
        busTableModel.setRowCount(0);
        connectionTableModel.setRowCount(0);

        try {
            var result = MQLUtil.execute(project, "print bus {} select paths[].path.type paths[].path.id dump |", id);
            if (CharSequenceUtil.isNotBlank(result)) {
                var array = result.split("\\|");
                var groupCount = array.length / 2;
                for (var i = 0; i < groupCount; i++) {
                    result = MQLUtil.execute(project, "print path {} select element[] dump |", array[i + groupCount]);
                    var arrayElement = result.split("\\|");
                    for (var element : arrayElement) {
                        var elements = element.split(",");
                        if ("connection".equals(elements[0])) {
                            result = MQLUtil.execute(project, "print connection {} select type id physicalid originated modified dump \001", elements[2]);
                            if (result.isEmpty()) continue;

                            var connectionInfos = result.split("\001");
                            String[] data = {"To", array[i], array[i + groupCount], connectionInfos[0], connectionInfos[1], connectionInfos[2], connectionInfos[3], connectionInfos[4]};
                            connectionTableModel.addRow(data);
                        } else {
                            result = MQLUtil.execute(project, "print bus {} select type name revision id physicalid description originated modified owner policy current lattice dump \001", elements[2]);
                            if (result.isEmpty()) continue;

                            var busInfos = result.split("\001");
                            String[] data = {"To", array[i], array[i + groupCount], busInfos[0], busInfos[1], busInfos[2], busInfos[3], busInfos[4], busInfos[5], busInfos[6], busInfos[7], busInfos[8], busInfos[9], busInfos[10], busInfos[11]};
                            busTableModel.addRow(data);
                        }
                    }
                }
            }
        } catch (MQLException e) {
            busTable.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
            connectionTable.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
        }
    }

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            filterComponent.reset();
            loaded = false;
            reload();
        }
    }
}
