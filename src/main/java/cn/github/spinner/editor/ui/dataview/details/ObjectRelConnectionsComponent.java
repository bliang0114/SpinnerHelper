package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整可用的对象关联连接表格组件，修复滚动选择异常、异步加载阻塞、双击跳转错误
 */
public class ObjectRelConnectionsComponent extends AbstractObjectDetailsTableComponent {
    private static final String MQL_SEPARATOR = "\001";
    public static final String YES = UIUtil.replaceMnemonicAmpersand("✓");
    public static final String NO = "";
    private static final int CONNECTION_TYPE_ID_MIN_LENGTH = 2;
    private static final int TARGET_REL_DETAILS_MIN_LENGTH = 4;
    private static final int WHERE_SUBSTRING_OFFSET = 2;
    private boolean isLoading = false;

    public ObjectRelConnectionsComponent(Project project, String id) {
        super(project, id);
        loadData();
    }

    @Override
    protected String[] headers() {
        return new String[]{
                "Direction", "Relationship", "Connection ID",
                "Target Relationship", "Target Connection ID",
                "Originated", "Modified", "Path"
        };
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{120, 240, 300, 240, 300, 240, 240, 120};
    }

    @Override
    protected Class<?>[] columnTypes() {
        return new Class[]{
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class
        };
    }

    @Override
    protected String componentId() {
        return ObjectRelConnectionsComponent.class.getSimpleName();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(null);
        table.setRowHeight(26);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        for (int i = 0; i < columnWidths().length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths()[i]);
            table.getColumnModel().getColumn(i).setMinWidth(50);
            table.getColumnModel().getColumn(i).setMaxWidth(1000);
        }
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isLoading || e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol < 0) {
                    return;
                }

                int modelRow = table.convertRowIndexToModel(viewRow);
                String targetId = getTargetId(modelRow, viewCol);
                if (StrUtil.isNotEmpty(targetId)) {
                    ConnectionDetailsWindow.showWindow(project, targetId);
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || isLoading) {
                return;
            }
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                return;
            }
            table.convertRowIndexToModel(viewRow);
        });
    }

    @Override
    protected void loadData() {
        if (isLoading) return;
        isLoading = true;
        SwingUtilities.invokeLater(() -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            table.getEmptyText().setText("Loading connections...");
            table.setEnabled(false);
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<String[]> loadedData = new ArrayList<>();
            Exception exception = null;

            try {
                loadedData = fetchAllConnectionData(id);
            } catch (MQLException e) {
                exception = new RuntimeException("MQL query failed", e);
            } catch (Exception e) {
                exception = new RuntimeException("Data processing failed", e);
            } finally {
                Exception finalException = exception;
                List<String[]> finalLoadedData = loadedData;
                SwingUtilities.invokeLater(() -> {
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    try {
                        if (finalException != null) {
                            table.getEmptyText().setText(String.format(
                                    "Error: %s (Bus ID: %s)",
                                    finalException.getMessage(), id
                            ));
                        } else {
                            for (String[] row : finalLoadedData) {
                                model.addRow(row);
                            }
                            if (finalLoadedData.isEmpty()) {
                                table.getEmptyText().setText("No connections found.");
                            }
                        }
                    } finally {
                        table.setEnabled(true);
                        isLoading = false;
                    }
                });
            }
        });
    }

    private List<String[]> fetchAllConnectionData(String busId) throws MQLException {
        List<String[]> data = new ArrayList<>();
        String whereCondition = buildWhereCondition(busId);
        fetchDirectionConnections(data, busId, "to", whereCondition, "FromRel");
        fetchDirectionConnections(data, busId, "from", whereCondition, "ToRel");
        fetchMidConnections(data, busId);
        return data;
    }

    private String buildWhereCondition(String busId) throws MQLException {
        StringBuilder where = new StringBuilder();
        String toIds = MQLUtil.execute(project, "print bus " + busId + " select to.id dump");

        if (StrUtil.isNotEmpty(toIds)) {
            for (String connId : toIds.split(",")) {
                if (StrUtil.isEmpty(connId)) continue;
                String connInfo = MQLUtil.execute(project,
                        "print connection " + connId + " select type id toall fromall dump");
                String[] parts = connInfo.split(",");
                if (parts.length >= 4 && StrUtil.equals(parts[2], parts[3])) {
                    where.append("&&id!=").append(parts[1]);
                }
            }
        }
        return where.isEmpty() ? "" : "[|" + where.substring(WHERE_SUBSTRING_OFFSET) + "]";
    }

    private void fetchDirectionConnections(List<String[]> data, String busId, String direction,
                                           String where, String directionDesc) throws MQLException {
        String query = "print bus " + busId + " select " + direction + where + ".id dump";
        String connIds = MQLUtil.execute(project, query);
        if (StrUtil.isNotEmpty(connIds)) {
            for (String connId : connIds.split(",")) {
                if (StrUtil.isEmpty(connId)) continue;
                String typeIdStr = MQLUtil.execute(project, "print connection " + connId + " select type id dump");
                String[] typeId = typeIdStr.split(",");
                if (typeId.length < CONNECTION_TYPE_ID_MIN_LENGTH) continue;
                String relQuery = direction.equals("to")
                        ? "select fromrel.type fromrel.id fromrel.originated fromrel.modified fromrel.paths[].path dump " + MQL_SEPARATOR
                        : "select torel.type torel.id torel.originated torel.modified torel.paths[].path dump " + MQL_SEPARATOR;
                String relInfo = MQLUtil.execute(project, "print connection " + connId + " " + relQuery);
                addRow(data, relInfo, typeId, directionDesc);
            }
        }
    }

    private void fetchMidConnections(List<String[]> data, String busId) throws MQLException {
        String connIds = MQLUtil.execute(project, "print bus " + busId + " select to.id from.id dump");
        if (StrUtil.isEmpty(connIds)) return;
        for (String connId : connIds.split(",")) {
            if (StrUtil.isEmpty(connId)) continue;
            fetchSingleMidConnection(data, connId);
        }
    }

    private void fetchSingleMidConnection(List<String[]> data, String connId) throws MQLException {
        StringBuilder where = new StringBuilder();
        String relationType = MQLUtil.execute(project, "print connection " + connId + " select type dump");
        String fromMidIds = MQLUtil.execute(project, "print connection " + connId + " select frommid.id dump");
        if (StrUtil.isNotEmpty(fromMidIds)) {
            for (String midConnId : fromMidIds.split(",")) {
                if (StrUtil.isEmpty(midConnId)) continue;
                String midConnInfo = MQLUtil.execute(project,
                        "print connection " + midConnId + " select type id fromall toall dump");
                String[] parts = midConnInfo.split(",");

                if (parts.length > 3 && StrUtil.equals(parts[2], parts[3])) {
                    where.append("&&id!=").append(parts[1]);
                    String relInfo = MQLUtil.execute(project,
                            "print connection " + midConnId + " select fromrel.type fromrel.id fromrel.originated fromrel.modified fromrel.paths[].path dump " + MQL_SEPARATOR);
                    addRow(data, relInfo, parts, "MidSelf");
                }
            }
        }

        String whereStr = where.isEmpty() ? "" : "[|" + where.substring(WHERE_SUBSTRING_OFFSET) + "]";
        fetchMidDirectionConnections(data, connId, "frommid", whereStr, "ToRel FromMid " + relationType + " " + connId, true);
        fetchMidDirectionConnections(data, connId, "tomid", whereStr, "FromRel ToMid " + relationType + " " + connId, false);
    }
    private void fetchMidDirectionConnections(List<String[]> data, String connId, String midDirection,
                                              String where, String directionDesc, boolean isToRel) throws MQLException {
        String query = "print connection " + connId + " select " + midDirection + where + ".id dump";
        String midConnIds = MQLUtil.execute(project, query);

        if (StrUtil.isNotEmpty(midConnIds)) {
            for (String midConnId : midConnIds.split(",")) {
                if (StrUtil.isEmpty(midConnId)) continue;

                String typeIdStr = MQLUtil.execute(project, "print connection " + midConnId + " select type id dump");
                String[] typeId = typeIdStr.split(",");
                if (typeId.length < CONNECTION_TYPE_ID_MIN_LENGTH) continue;

                String relQuery = isToRel
                        ? "select torel.type torel.id torel.originated torel.modified torel.paths[].path dump " + MQL_SEPARATOR
                        : "select fromrel.type fromrel.id fromrel.originated fromrel.modified fromrel.paths[].path dump " + MQL_SEPARATOR;

                String relInfo = MQLUtil.execute(project, "print connection " + midConnId + " " + relQuery);
                addRow(data, relInfo, typeId, directionDesc);
            }
        }
    }

    private void addRow(List<String[]> data, String relInfo, String[] typeId, String directionDesc) {
        if (StrUtil.isEmpty(relInfo) || typeId == null || typeId.length < CONNECTION_TYPE_ID_MIN_LENGTH) {
            return;
        }

        String[] relDetails = relInfo.split(MQL_SEPARATOR);
        if (relDetails.length < TARGET_REL_DETAILS_MIN_LENGTH) {
            return;
        }
        data.add(new String[]{
                directionDesc,
                typeId[0].trim(),          // Relationship
                typeId[1].trim(),          // Connection ID
                relDetails[0].trim(),      // Target Relationship
                relDetails[1].trim(),      // Target Connection ID
                relDetails[2].trim(),      // Originated
                relDetails[3].trim(),      // Modified
                relDetails.length > TARGET_REL_DETAILS_MIN_LENGTH ? YES : NO
        });
    }

    private String getTargetId(int modelRow, int viewCol) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (modelRow < 0 || modelRow >= model.getRowCount()) {
            return "";
        }

        int colIndex = viewCol <= 3 ? 2 : 4;
        Object value = model.getValueAt(modelRow, colIndex);
        return value != null ? value.toString().trim() : "";
    }

}
