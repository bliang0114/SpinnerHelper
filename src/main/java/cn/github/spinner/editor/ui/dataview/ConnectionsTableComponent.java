package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixConnectionQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.github.spinner.editor.ui.dataview.bean.ConnectionsRow;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
    protected List<ConnectionsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixConnectionQuery connectionQuery = new MatrixConnectionQuery();
        connectionQuery.setType(name);
        MatrixQueryResult queryResult = connection.queryConnection(connectionQuery, List.of("type", "id", "paths", "physicalid", "from.type", "from.name", "from.revision", "from.id", "fromrel.type", "fromrel.id", "to.type", "to.name", "to.revision", "to.id", "torel.type", "torel.id"));
        List<ConnectionsRow> dataList = new ArrayList<>();
        if (!queryResult.isEmpty()) {
            for (Map<String, String> map : queryResult.getData()) {
                ConnectionsRow row = new ConnectionsRow();
                row.setType(MapUtil.getStr(map, "type"));
                row.setId(MapUtil.getStr(map, "id"));
                String path = MapUtil.getStr(map, "paths");
                row.setPath(CharSequenceUtil.isNotBlank(path) && !path.equalsIgnoreCase("FALSE"));
                row.setPhysicalId(MapUtil.getStr(map, "physicalid"));
                row.setFromType(MapUtil.getStr(map, "from.type"));
                row.setFromName(MapUtil.getStr(map, "from.name"));
                row.setFromRevision(MapUtil.getStr(map, "from.revision"));
                row.setFromId(MapUtil.getStr(map, "from.id"));
                row.setFromRelType(MapUtil.getStr(map, "fromrel.type"));
                row.setFromRelId(MapUtil.getStr(map, "fromrel.id"));
                row.setToType(MapUtil.getStr(map, "to.type"));
                row.setToName(MapUtil.getStr(map, "to.name"));
                row.setToRevision(MapUtil.getStr(map, "to.revision"));
                row.setToId(MapUtil.getStr(map, "to.id"));
                row.setToRelType(MapUtil.getStr(map, "torel.type"));
                row.setToRelId(MapUtil.getStr(map, "torel.id"));
                dataList.add(row);
            }
        }
        return dataList;
    }
}
