package cn.github.spinner.editor.ui.dataview.details;

import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ObjectBasicInformationComponent extends AbstractObjectDetailsTableComponent {

    public ObjectBasicInformationComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"", ""};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{400, 600};
    }

    @Override
    protected String componentId() {
        return ObjectBasicInformationComponent.class.getSimpleName();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        configureFirstColumnAsHeader();
    }

    private void configureFirstColumnAsHeader() {
        if (table.getColumnCount() == 0) return;

        table.setTableHeader(null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
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

    @Override
    protected void loadData() {
        tableModel.setRowCount(0);
        new TrackedBackgroundTask(project, SpinnerBundle.message("message.loading.data"), true) {
            private final List<String[]> rows = new ArrayList<>();
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String result = MQLUtil.execute(project, "print bus {} select type name revision id physicalid description originated modified lattice policy current owner organization project attribute.value", id);
                    String[] array = result.split("\n");
                    for (int i = 1; i < array.length; i++) {
                        String[] attribute = array[i].split(" = ");
                        rows.add(new String[]{formatAttributeName(attribute[0]), attribute.length > 1 ? attribute[1] : ""});
                    }
                } catch (Exception e) {
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                tableModel.setRowCount(0);
                if (error != null) {
                    table.getEmptyText().setText(SpinnerBundle.message("message.error.print", id, error.getMessage()));
                    return;
                }
                rows.forEach(tableModel::addRow);
            }
        }.queue();
    }
}
