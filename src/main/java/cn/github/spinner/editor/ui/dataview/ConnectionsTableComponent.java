package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixConnectionQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.github.spinner.config.ObjectWhereExpression;
import cn.github.spinner.editor.ui.dataview.bean.ConnectionsRow;
import cn.github.spinner.editor.ui.dataview.bean.ObjectsRow;
import cn.github.spinner.editor.ui.dataview.details.ConnectionDetailsWindow;
import cn.github.spinner.editor.ui.dataview.details.ObjectDetailsWindow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConnectionsTableComponent extends AbstractDataViewTableComponent<ConnectionsRow> {

    public ConnectionsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new ConnectionsRow(), "Connections Table");
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
                        if (columnIndex <= 4) {
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 2));
                            ConnectionDetailsWindow.showWindow(project, id);
                        } else if (columnIndex <= 8) {
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 8));
                            ObjectDetailsWindow.showWindow(project, id);
                        } else {
                            String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 12));
                            ConnectionDetailsWindow.showWindow(project, id);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected List<ConnectionsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        String result = MQLUtil.execute(project, "query connection rel '{}' dump", name);
        if (CharSequenceUtil.isNotBlank(result) && NumberUtil.isInteger(result)) {
            totalCount = result.split("\n").length;
        }
        List<ConnectionsRow> dataList = new ArrayList<>();
        var array = new String[0];
        if (pageSize > 0) {
            array = loadPageConnection();
        } else {
            array = loadAllConnection();
        }
        for (String str : array) {
            String[] arrayInfo = str.split("\001");
            ConnectionsRow row = new ConnectionsRow();
            row.setType(arrayInfo[0]);
            row.setId(arrayInfo[1]);
            String path = arrayInfo[2];
            row.setPath(CharSequenceUtil.isNotBlank(path) && !path.equalsIgnoreCase("FALSE"));
            row.setPhysicalId(arrayInfo[3]);
            row.setFromType(arrayInfo.length > 4 ? arrayInfo[4] : "");
            row.setFromName(arrayInfo.length > 5 ? arrayInfo[5] : "");
            row.setFromRevision(arrayInfo.length > 6 ? arrayInfo[6] : "");
            row.setFromId(arrayInfo.length > 7 ? arrayInfo[7] : "");
            row.setToType(arrayInfo.length > 8 ? arrayInfo[8] : "");
            row.setToName(arrayInfo.length > 9 ? arrayInfo[9] : "");
            row.setToRevision(arrayInfo.length > 10 ? arrayInfo[10] : "");
            row.setToId(arrayInfo.length > 11 ? arrayInfo[11] : "");
            dataList.add(row);
        }
        return dataList;
    }

    private String[] loadPageConnection() throws MQLException {
        var startIndex = (currentPage - 1) * pageSize;
        var result = MQLUtil.execute(project, "query connection rel '{}' limit {} select id dump \001 recordsep \002", name, (currentPage * pageSize));
        if (CharSequenceUtil.isNotBlank(result)) {
            var array = result.split("\002");
            List<String> ids = new ArrayList<>();
            for (var i = startIndex; i < array.length; i++) {
                ids.add(array[i].split("\001")[1]);
            }
            result = MQLUtil.execute(project, "query connection rel '{}' limit {} where \"id matchlist '{}' '{}'\" select id paths physicalid from.type from.name from.revision from.id to.type to.name to.revision to.id dump \001 recordsep \002", name, ids.size(), CharSequenceUtil.join(",", ids), ",");
            array = result.split("\002");
            return array;
        }
        return new String[0];
    }

    private String[] loadAllConnection() throws MQLException {
        var result = MQLUtil.execute(project, "query connection rel '{}' select id paths physicalid from.type from.name from.revision from.id to.type to.name to.revision to.id dump \001 recordsep \002", name);
        return result.split("\002");
    }
}
