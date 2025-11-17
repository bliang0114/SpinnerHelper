package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ObjectRelConnectionsComponent extends AbstractObjectDetailsTableComponent {

    public ObjectRelConnectionsComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"Direction", "Relationship", "Connection ID", "Connection PhysicalID", "Target Relationship", "Target Connection ID", "Target Connection PhysicalID", "Originated", "Modified", "Path"};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{120, 240, 300, 300, 240, 300, 300, 240, 240, 120};
    }

    @Override
    protected Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, Boolean.class};
    }

    @Override
    protected String componentId() {
        return ObjectRelConnectionsComponent.class.getSimpleName();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = table.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0) return;

                        int columnIndex = table.columnAtPoint(e.getPoint());
                        if (columnIndex <= 3) {
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 2));
                            ConnectionDetailsWindow.showWindow(project, id);
                        } else {
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 5));
                            ConnectionDetailsWindow.showWindow(project, id);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void loadData() {
        tableModel.setRowCount(0);
        try {
            List<Object[]> fromData = printFromRelData();
            List<Object[]> toData = printToRelData();
//            List<Object[]> fromMidData = printFromMidData(fromData);
//            List<Object[]> toMidData = printToMidData(toData);
            List<Object[]> allData = new ArrayList<>(fromData);
            allData.addAll(toData);
            for (Object[] row : allData) {
                tableModel.addRow(row);
            }
        } catch (MQLException e) {
            table.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
        }
    }

    private List<Object[]> printFromRelData() throws MQLException {
        String mql = """
                print bus {}
                select to.type to.id to.physicalid
                to.fromrel.type to.fromrel.id to.fromrel.physicalid
                to.fromrel.originated to.fromrel.modified to.fromrel.paths dump \001;
                """;
        String result = MQLUtil.execute(project, mql, id);
        List<Object[]> list = new ArrayList<>();
        if (CharSequenceUtil.isNotBlank(result)) {
            buildRowData(result, "FromRel", list);
        }
        return list;
    }

    private List<Object[]> printToRelData() throws MQLException {
        String mql = """
                print bus {}
                select from.type from.id from.physicalid
                from.torel.type from.torel.id from.torel.physicalid
                from.torel.originated from.torel.modified from.torel.paths dump \001;
                """;
        String result = MQLUtil.execute(project, mql, id);
        List<Object[]> list = new ArrayList<>();
        if (CharSequenceUtil.isNotBlank(result)) {
            buildRowData(result, "ToRel", list);
        }
        return list;
    }

    /*private List<Object[]> printFromRelToMidData(List<Object[]> fromData) throws MQLException {
        List<Object[]> list = new ArrayList<>();
        for (Object[] data : fromData) {
            String type = ObjectUtil.toString(data[1]);
            String connectionId = ObjectUtil.toString(data[2]);
            String mql = """
                    print connection {}
                    select tomid.type tomid.id tomid.physicalid
                    fromrel.type fromrel.id fromrel.physicalid
                    fromrel.originated fromrel.modified fromrel.paths dump \001;
                    """;
            String result = MQLUtil.execute(project, mql, connectionId);
            String direction = "FromRel ToMid " + type + " " + connectionId;
            if (CharSequenceUtil.isNotBlank(result)) {
                buildRowData(result, direction, list);
            }
        }
        return list;
    }

    private List<Object[]> printToRelFromMidData(List<Object[]> toData) throws MQLException {
        List<Object[]> list = new ArrayList<>();
        for (Object[] data : toData) {
            String type = ObjectUtil.toString(data[1]);
            String connectionId = ObjectUtil.toString(data[2]);
            String mql = """
                    print connection {}
                    select tomid.type tomid.id tomid.physicalid
                    tomid.from.type tomid.from.name tomid.from.revision tomid.from.id tomid.from.physicalid tomid.from.paths
                    tomid.from.description tomid.from.originated tomid.from.modified tomid.from.owner tomid.from.policy tomid.from.current tomid.from.lattice dump \001
                    """;
            String result = MQLUtil.execute(project, mql, connectionId);
            String direction = "From ToMid " + type + " " + connectionId;
            if (CharSequenceUtil.isNotBlank(result)) {
                buildRowData(result, direction, list);
            }
        }
        return list;
    }*/

    private void buildRowData(String result, String direction, List<Object[]> list) {
        String[] array = result.split("\001");
        int groupCount = array.length / 9;
        for (int i = 0; i < groupCount; i++) {
            Object[] row = new Object[10];
            row[0] = direction;
            row[1] = array[i];
            row[2] = array[i + groupCount];
            row[3] = array[i + 2 * groupCount];
            row[4] = array[i + 3 * groupCount];
            row[5] = array[i + 4 * groupCount];
            row[6] = array[i + 5 * groupCount];
            row[7] = array[i + 6 * groupCount];
            row[8] = array[i + 7 * groupCount];
            row[9] = CharSequenceUtil.equalsIgnoreCase(array[i + 8 * groupCount], "TRUE");
            list.add(row);
        }
    }
}
