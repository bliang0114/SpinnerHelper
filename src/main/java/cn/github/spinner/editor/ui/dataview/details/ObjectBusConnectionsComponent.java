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

public class ObjectBusConnectionsComponent extends AbstractObjectDetailsTableComponent {

    public ObjectBusConnectionsComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"Direction", "Relationship", "Connection ID", "Connection PhysicalID", "Type", "Name", "Revision", "Object ID", "Object PhysicalID", "Path", "Description", "Originated", "Modified", "Owner", "Policy", "State", "Vault"};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{120, 240, 300, 300, 240, 240, 120, 300, 300, 60, 300, 240, 240, 240, 240, 180, 240};
    }

    @Override
    protected Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, Boolean.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class};
    }

    @Override
    protected String componentId() {
        return ObjectBusConnectionsComponent.class.getSimpleName();
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
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 7));
                            ObjectDetailsWindow.showWindow(project, id);
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
            List<Object[]> fromData = printFromData();
            List<Object[]> toData = printToData();
            List<Object[]> fromMidData = printToFromMidData(fromData);
            List<Object[]> toMidData = printFromToMidData(toData);
            List<Object[]> allData = new ArrayList<>(fromData);
            allData.addAll(toData);
            allData.addAll(fromMidData);
            allData.addAll(toMidData);
            for (Object[] row : allData) {
                tableModel.addRow(row);
            }
        } catch (MQLException e) {
            table.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
        }
    }

    private List<Object[]> printFromData() throws MQLException {
        String mql = """
                print bus {} select to.type to.id to.physicalid
                to.from.type to.from.name to.from.revision to.from.id to.from.physicalid to.from.paths to.from.description
                to.from.originated to.from.modified to.from.owner to.from.policy to.from.current to.from.lattice dump \001
                """;
        String result = MQLUtil.execute(project, mql, id);
        List<Object[]> list = new ArrayList<>();
        if (CharSequenceUtil.isNotBlank(result)) {
            buildRowData(result, "From", list);
        }
        return list;
    }

    private List<Object[]> printToData() throws MQLException {
        String mql = """
                print bus {} select from.type from.id from.physicalid
                from.to.type from.to.name from.to.revision from.to.id from.to.physicalid from.to.paths from.to.description
                from.to.originated from.to.modified from.to.owner from.to.policy from.to.current from.to.lattice dump \001
                """;
        String result = MQLUtil.execute(project, mql, id);
        List<Object[]> list = new ArrayList<>();
        if (CharSequenceUtil.isNotBlank(result)) {
            buildRowData(result, "To", list);
        }
        return list;
    }

    private List<Object[]> printToFromMidData(List<Object[]> fromData) throws MQLException {
        List<Object[]> list = new ArrayList<>();
        for (Object[] data : fromData) {
            String type = ObjectUtil.toString(data[1]);
            String connectionId = ObjectUtil.toString(data[2]);
            String mql = """
                    print connection {}
                    select frommid.type frommid.id frommid.physicalid
                    frommid.to.type frommid.to.name frommid.to.revision frommid.to.id frommid.to.physicalid frommid.to.paths
                    frommid.to.description frommid.to.originated frommid.to.modified frommid.to.owner frommid.to.policy frommid.to.current frommid.to.lattice dump \001
                    """;
            String result = MQLUtil.execute(project, mql, connectionId);
            String direction = "To FromMid " + type + " " + connectionId;
            if (CharSequenceUtil.isNotBlank(result)) {
                buildRowData(result, direction, list);
            }
        }
        return list;
    }

    private List<Object[]> printFromToMidData(List<Object[]> toData) throws MQLException {
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
    }

    private void buildRowData(String result, String direction, List<Object[]> list) {
        String[] array = result.split("\001");
        int groupCount = array.length / 16;
        for (int i = 0; i < groupCount; i++) {
            Object[] row = new Object[17];
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
            row[10] = array[i + 9 * groupCount];
            row[11] = array[i + 10 * groupCount];
            row[12] = array[i + 11 * groupCount];
            row[13] = array[i + 12 * groupCount];
            row[14] = array[i + 13 * groupCount];
            row[15] = array[i + 14 * groupCount];
            row[16] = array[i + 15 * groupCount];
            list.add(row);
        }
    }
}
