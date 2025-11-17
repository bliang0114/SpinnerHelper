package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ObjectBusConnectionsComponent extends AbstractObjectDetailsTableComponent {
    private static final Logger logger = Logger.getInstance(ObjectBusConnectionsComponent.class);
    public static final String YES = UIUtil.replaceMnemonicAmpersand("✓");

    private volatile boolean isLoading = false;

    public ObjectBusConnectionsComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"Direction", "Relationship", "Connection ID", "Type", "Name", "Revision",
                "Object ID", "Path", "Description", "Originated", "Modified", "Owner", "State", "Policy", "Vault"};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{
                280, 280, 300, 280, 280, 110,
                300, 180, 280, 280, 280, 160,
                280, 280, 280
        };
    }

    @Override
    protected Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class, String.class};
    }

    @Override
    protected String componentId() {
        return ObjectBusConnectionsComponent.class.getSimpleName();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getEmptyText().setText("Loading bus connections...");
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isLoading) return;

                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = table.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0 || modelRowIndex >= tableModel.getRowCount()) return;

                        int columnIndex = table.columnAtPoint(e.getPoint());
                        try {
                            if (columnIndex <= 3) {
                                String connectionId = String.valueOf(tableModel.getValueAt(modelRowIndex, 2));
                                if (StrUtil.isNotEmpty(connectionId)) {
                                    ConnectionDetailsWindow.showWindow(project, connectionId);
                                }
                            } else {
                                String objectId = String.valueOf(tableModel.getValueAt(modelRowIndex, 6));
                                if (StrUtil.isNotEmpty(objectId)) {
                                    ObjectDetailsWindow.showWindow(project, objectId);
                                }
                            }
                        } catch (Exception ex) {
                            logger.error(ex);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void loadData() {
        if (isLoading) return;
        new Task.Backgroundable(project, "Loading bus connections", false) {
            private List<String[]> loadedData;
            private String errorMessage;

            @Override
            public void onSuccess() {
                isLoading = false;
                tableModel.setRowCount(0);
                if (errorMessage != null) {
                    table.getEmptyText().setText(errorMessage);
                    return;
                }
                if (loadedData != null && !loadedData.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        for (String[] row : loadedData) {
                            tableModel.addRow(row);
                        }
                        table.getEmptyText().setText("No bus connections found");
                    });
                } else {
                    table.getEmptyText().setText("No bus connections found");
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                isLoading = false;
                errorMessage = "Error loading bus connections: " + t.getMessage();
                SwingUtilities.invokeLater(() -> table.getEmptyText().setText(errorMessage));
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                isLoading = true;
                loadedData = new ArrayList<>();
                errorMessage = null;
                try {
                    indicator.setText("Fetching bus connections...");
                    indicator.setIndeterminate(true);
                    getDataForBus(id, indicator, loadedData);

                } catch (MQLException e) {
                    errorMessage = "Error: print " + id + " error. " + e.getMessage();
                } catch (Exception e) {
                    errorMessage = "Unexpected error: " + e.getMessage();
                } finally {
                    indicator.setIndeterminate(false);
                }
            }
        }.queue();
    }

    /**
     * 异步数据获取逻辑，增加进度指示器支持
     */
    private void getDataForBus(String id, ProgressIndicator indicator, List<String[]> result) throws MQLException {
        String res;
        String[] a;
        StringBuilder where = new StringBuilder();
        indicator.setText("Fetching self connections...");
        res = MQLUtil.execute(project, "print bus " + id + " select to.id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;
                res = MQLUtil.execute(project, "print connection " + connection + " select type id toall fromall dump");
                a = res.split(",");
                if (a.length >= 4 && a[2].equals(a[3])) {
                    where.append("&&id!=").append(a[1]);
                    res = MQLUtil.execute(project, "print connection " + connection + " select from.type from.name from.revision from.id from.description from.originated from.modified from.owner from.current from.policy from.lattice from.paths[].path dump \001");
                    addRow(res, a, "Self", result);
                }
            }
        }

        indicator.setText("Fetching 'From' connections...");
        if (!StrUtil.isEmpty(where.toString())) {
            where = new StringBuilder("[|" + where.substring(2) + "]");
        }
        res = MQLUtil.execute(project, "print bus " + id + " select to" + where + ".id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;

                res = MQLUtil.execute(project, "print connection " + connection + " select type id dump");
                a = res.split(",");
                res = MQLUtil.execute(project, "print connection " + connection + " select from.type from.name from.revision from.id from.description from.originated from.modified from.owner from.current from.policy from.lattice from.paths[].path dump \001");
                addRow(res, a, "From", result);
            }
        }

        indicator.setText("Fetching 'To' connections...");
        res = MQLUtil.execute(project, "print bus " + id + " select from" + where + ".id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;

                res = MQLUtil.execute(project, "print connection " + connection + " select type id dump");
                a = res.split(",");
                res = MQLUtil.execute(project, "print connection " + connection + " select to.type to.name to.revision to.id to.description to.originated to.modified to.owner to.current to.policy to.lattice to.paths[].path dump \001");
                addRow(res, a, "To", result);
            }
        }

        indicator.setText("Fetching middle connections...");
        res = MQLUtil.execute(project, "print bus " + id + " select to.id from.id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (!StrUtil.isEmpty(connection)) {
                    if (indicator.isCanceled()) return;
                    getMidConnections(connection, indicator, result);
                }
            }
        }
    }

    /**
     * 异步获取中间连接
     */
    void getMidConnections(String id, ProgressIndicator indicator, List<String[]> result) throws MQLException {
        String res;
        String[] a;
        StringBuilder where = new StringBuilder();

        indicator.setText("Processing middle connection: " + id);
        var relation = MQLUtil.execute(project, "print connection " + id + " select type dump");

        res = MQLUtil.execute(project, "print connection " + id + " select frommid.id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;

                res = MQLUtil.execute(project, "print connection " + connection + " select type id fromall toall dump");
                a = res.split(",");
                if (a.length > 3 && a[2].equals(a[3])) {
                    where.append("&&id!=").append(a[1]);
                }
            }
        }

        if (!where.isEmpty()) {
            where = new StringBuilder("[|" + where.substring(2) + "]");
        }

        res = MQLUtil.execute(project, "print connection " + id + " select frommid" + where + ".id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;

                res = MQLUtil.execute(project, "print connection " + connection + " select type id dump");
                a = res.split(",");
                res = MQLUtil.execute(project, "print connection " + connection + " select to.type to.name to.revision to.id to.description to.originated to.modified to.owner to.current to.policy to.lattice to.paths[].path dump \001");
                addRow(res, a, "To FromMid " + relation + " " + id, result);
            }
        }

        res = MQLUtil.execute(project, "print connection " + id + " select tomid" + where + ".id dump");
        if (!StrUtil.isEmpty(res)) {
            var connections = res.split(",");
            for (var connection : connections) {
                if (StrUtil.isEmpty(connection)) continue;
                if (indicator.isCanceled()) return;

                res = MQLUtil.execute(project, "print connection " + connection + " select type id dump");
                a = res.split(",");
                res = MQLUtil.execute(project, "print connection " + connection + " select from.type from.name from.revision from.id from.description from.originated from.modified from.owner from.current from.policy from.lattice to.paths[].path dump \001");
                addRow(res, a, "From ToMid " + relation + " " + id, result);
            }
        }
    }

    /**
     * 线程安全的添加行数据
     */
    private synchronized void addRow(String busInfo, String[] connectionArray, String text, List<String[]> result) {
        if (StrUtil.isEmpty(busInfo) || connectionArray == null || connectionArray.length < 2) return;
        var b = busInfo.split("\001");
        String pathFlag;
        if (b.length >= 11) {
            pathFlag = (b.length > 11) ? YES : StrUtil.EMPTY;
            String[] row = new String[15];
            row[0] = text;                          // Direction
            row[1] = connectionArray[0];            // Relationship
            row[2] = connectionArray[1];            // Connection ID
            row[3] = getSafeValue(b, 0);            // Type
            row[4] = getSafeValue(b, 1);            // Name
            row[5] = getSafeValue(b, 2);            // Revision
            row[6] = getSafeValue(b, 3);            // Object ID
            row[7] = pathFlag;                      // Path
            row[8] = getSafeValue(b, 4);            // Description
            row[9] = getSafeValue(b, 5);            // Originated
            row[10] = getSafeValue(b, 6);           // Modified
            row[11] = getSafeValue(b, 7);           // Owner
            row[12] = getSafeValue(b, 8);           // State
            row[13] = getSafeValue(b, 9);           // Policy
            row[14] = getSafeValue(b, 10);          // Vault

            result.add(row);
        }
    }

    /**
     * 安全获取数组元素，避免数组越界
     */
    private String getSafeValue(String[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return StrUtil.EMPTY;
        }
        String value = array[index];
        return StrUtil.isEmpty(value) ? StrUtil.EMPTY : value.trim();
    }
}