package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixConnectionQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.github.spinner.components.RowNumberTableModel;
import cn.github.spinner.editor.ui.dataview.bean.ConnectionsRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConnectionsTableComponent extends AbstractDataViewTableComponent<ConnectionsRow, ConnectionsTableComponent> {
    private static final Object[] COLUMNS = new Object[]{"Type", "ID", "Path", "PhysicalID", "From Type", "From Name", "From Revision", "From Id", "From Rel Type", "From Rel Id", "To Type", "To Name", "To Revision", "To Id", "To Rel Type", "To Id"};
    private static final int[] COLUMN_WIDTHS = new int[]{200, 200, 100, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200};
    private JBTextField limitField;
    private JBTextField totalField;

    public ConnectionsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new RowNumberTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) {
                    return Boolean.class;
                }
                return String.class;
            }
        }, COLUMN_WIDTHS, "Connections Table Toolbar");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        limitField = new JBTextField("100", 6);
        totalField = new JBTextField(6);
        totalField.setEnabled(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected Component[] createToolbarComponent() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = JBUI.emptyInsets();
        limitField.setPreferredSize(JBUI.size(-1, 30));
        totalField.setPreferredSize(JBUI.size(-1, 30));
        gbc.gridx = 0;
        panel.add(new JBLabel("Count"), gbc);
        gbc.gridx = 1;
        panel.add(limitField);
        gbc.gridx = 2;
        panel.add(new JBLabel("/"));
        gbc.gridx = 3;
        panel.add(totalField);
        container.add(panel);
        return new Component[] { table.getFilterComponent(), container };
    }

    @Override
    protected void addRow(ConnectionsRow row) {
        tableModel.addRow(new Object[]{row.getType(), row.getId(), row.isPath(), row.getPhysicalId(), row.getFromType(), row.getFromName(), row.getFromRevision(), row.getFromId(), row.getFromRelType(), row.getFromRelId(), row.getToType(), row.getToName(), row.getToRevision(), row.getToId(), row.getToRelType(), row.getToRelId()});
    }

    @Override
    protected List<ConnectionsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        String result = MQLUtil.execute(project, "query connection rel '{}' dump", name);
        String totalCount = "0";
        if (CharSequenceUtil.isNotBlank(result)) {
            totalCount = String.valueOf(result.split("\n").length);
        }
        totalField.setText(totalCount);
        MatrixConnectionQuery connectionQuery = new MatrixConnectionQuery();
        connectionQuery.setType(name);
        connectionQuery.setLimit(Short.parseShort(limitField.getText()));
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
