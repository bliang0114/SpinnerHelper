package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixObjectQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.github.spinner.config.ObjectWhereExpression;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.editor.ui.dataview.bean.ObjectsRow;
import cn.github.spinner.ui.ObjectWhereExpressionBuilderDialog;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ObjectsTableComponent extends AbstractDataViewTableComponent<ObjectsRow> {
    private JButton whereBtn;

    public ObjectsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new ObjectsRow(), "Objects Table");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        whereBtn = new JButton("Where");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        whereBtn.addActionListener(e -> {
            ObjectWhereExpressionBuilderDialog dialog = new ObjectWhereExpressionBuilderDialog(project);
            if (dialog.showAndGet()) {
                ObjectWhereExpression whereExpression = dialog.getWhereExpression();
                SpinnerToken.putObjectWhereExpression(project, whereExpression);
                String tooltip = CharSequenceUtil.format("""
                                <table>
                                    <tr>
                                        <td><strong>Name</strong></td>
                                        <td>{}</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Revision</strong></td>
                                        <td>{}</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Where</strong></td>
                                        <td>{}</td>
                                    </tr>
                                </table>
                                """,
                        whereExpression.getName(),
                        whereExpression.getRevision(),
                        whereExpression.build());
                whereBtn.setToolTipText(tooltip);

                tableModel.setRowCount(0);
                table.getEmptyText().setText("Loading Data...");
                executor.schedule(() -> {
                    try {
                        MatrixObjectQuery objectQuery = new MatrixObjectQuery();
                        objectQuery.setType(name);
                        objectQuery.setName(whereExpression.getName());
                        objectQuery.setRevision(whereExpression.getRevision());
                        objectQuery.setExpandType(true);
                        objectQuery.setWhereExpression(whereExpression.build());
                        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
                        setTableData(queryFromMatrix(connection, objectQuery));
                        table.getEmptyText().setText("Nothing to show");
                    } catch (MQLException ex) {
                        log.error(ex.getLocalizedMessage(), ex);
                        table.getEmptyText().setText(ex.getLocalizedMessage());
                    }
                }, 100, TimeUnit.MILLISECONDS);
            }
        });
    }

    @Override
    protected Component[] createToolbarComponent() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = JBUI.emptyInsets();
        gbc.gridx = 0;
        panel.add(whereBtn);
        container.add(panel);
        return new Component[] { table.getFilterComponent(), container };
    }

    @Override
    protected List<ObjectsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixObjectQuery objectQuery = new MatrixObjectQuery();
        objectQuery.setType(name);
        objectQuery.setExpandType(true);
        return queryFromMatrix(connection, objectQuery);
    }

    private List<ObjectsRow> queryFromMatrix(MatrixConnection connection, MatrixObjectQuery objectQuery) throws MQLException {
        MatrixQueryResult queryResult = connection.queryObject(objectQuery, List.of("type", "name", "revision", "id", "paths", "physicalid", "description", "originated", "modified", "lattice", "policy", "owner", "current", "organization", "project"));
        List<ObjectsRow> dataList = new ArrayList<>();
        if (!queryResult.isEmpty()) {
            for (Map<String, String> map : queryResult.getData()) {
                ObjectsRow row = new ObjectsRow();
                row.setType(MapUtil.getStr(map, "type"));
                row.setName(MapUtil.getStr(map, "name"));
                row.setRevision(MapUtil.getStr(map, "revision"));
                row.setId(MapUtil.getStr(map, "id"));
                String path = MapUtil.getStr(map, "paths");
                row.setPath(CharSequenceUtil.isNotBlank(path) && !path.equalsIgnoreCase("FALSE"));
                row.setPhysicalId(MapUtil.getStr(map, "physicalid"));
                row.setDescription(MapUtil.getStr(map, "description"));
                row.setOriginated(MapUtil.getStr(map, "originated"));
                row.setModified(MapUtil.getStr(map, "modified"));
                row.setVault(MapUtil.getStr(map, "lattice"));
                row.setPolicy(MapUtil.getStr(map, "policy"));
                row.setOwner(MapUtil.getStr(map, "owner"));
                row.setState(MapUtil.getStr(map, "current"));
                row.setOrganization(MapUtil.getStr(map, "organization"));
                row.setCollaborativeSpace(MapUtil.getStr(map, "project"));
                dataList.add(row);
            }
        }
        return dataList;
    }
}
