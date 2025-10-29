package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixObjectQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.ObjectWhereExpression;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.editor.ui.dataview.bean.ObjectsRow;
import com.bol.spinner.ui.ObjectWhereExpressionBuilderDialog;
import com.bol.spinner.util.MQLUtil;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class ObjectsTableComponent extends AbstractDataViewTableComponent<ObjectsRow, ObjectsTableComponent> {
    private JBTextField limitField;
    private JBTextField totalField;
    private JButton whereBtn;

    public ObjectsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new DefaultTableModel(new Object[]{"Type", "Name", "Revision", "ID", "Path", "PhysicalID", "Description", "Originated", "Modified", "Vault", "Policy", "Owner", "State", "Organization", "Collaborative Space"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) {
                    return Boolean.class;
                }
                return String.class;
            }
        }, new int[]{200, 200, 100, 200, 60, 200, 200, 150, 100, 150, 200, 100, 100, 300}, "Objects Table Toolbar");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        limitField = new JBTextField("100", 6);
        totalField = new JBTextField(6);
        totalField.setEnabled(false);
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

                rowList.clear();
                tableModel.setRowCount(0);

                table.getEmptyText().setText("Loading Data...");
                executor.schedule(() -> {
                    try {
                        MatrixObjectQuery objectQuery = new MatrixObjectQuery();
                        objectQuery.setType(name);
                        objectQuery.setName(whereExpression.getName());
                        objectQuery.setRevision(whereExpression.getRevision());
                        objectQuery.setExpandType(true);
                        objectQuery.setLimit(Short.parseShort(limitField.getText()));
                        objectQuery.setWhereExpression(whereExpression.build());
                        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
                        rowList.addAll(queryFromMatrix(connection, objectQuery));
                        for (ObjectsRow row : rowList) {
                            addRow(row);
                        }
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
    protected JComponent getToolbarComponent() {
        JComponent toolbarComponent = super.getToolbarComponent();
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
        gbc.gridx = 4;
        panel.add(whereBtn);
        container.add(panel);
        toolbarComponent.add(container, BorderLayout.CENTER);
        return toolbarComponent;
    }

    @Override
    protected List<Function<ObjectsRow, String>> getFilterFunctions() {
        return List.of(ObjectsRow::getType,
                ObjectsRow::getName,
                ObjectsRow::getRevision,
                ObjectsRow::getId,
                ObjectsRow::getPhysicalId,
                ObjectsRow::getDescription,
                ObjectsRow::getOriginated,
                ObjectsRow::getModified,
                ObjectsRow::getVault,
                ObjectsRow::getPolicy,
                ObjectsRow::getOwner,
                ObjectsRow::getState,
                ObjectsRow::getOrganization,
                ObjectsRow::getCollaborativeSpace);
    }

    @Override
    protected void addRow(ObjectsRow row) {
        tableModel.addRow(new Object[]{row.getType(), row.getName(), row.getRevision(), row.getId(), row.isPath(), row.getPhysicalId(), row.getDescription(), row.getOriginated(), row.getModified(), row.getVault(), row.getPolicy(), row.getOwner(), row.getState(), row.getOrganization(), row.getCollaborativeSpace()});
    }

    @Override
    protected List<ObjectsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixObjectQuery objectQuery = new MatrixObjectQuery();
        objectQuery.setType(name);
        objectQuery.setExpandType(true);
        objectQuery.setLimit(Short.parseShort(limitField.getText()));
        return queryFromMatrix(connection, objectQuery);
    }

    private List<ObjectsRow> queryFromMatrix(MatrixConnection connection, MatrixObjectQuery objectQuery) throws MQLException {
        String countQuery = CharSequenceUtil.format("eval expr 'count TRUE' on temp query bus '{}' '{}' '{}'", objectQuery.getType(), objectQuery.getName(), objectQuery.getRevision());
        String countQueryWithWhere = "";
        if (!objectQuery.getWhereExpression().isEmpty()) {
            countQueryWithWhere = countQuery + " where \"" + objectQuery.getWhereExpression() + "\"";
        }
        String totalCount = MQLUtil.execute(project, countQuery);
        if (!countQueryWithWhere.isEmpty()) {
            String withWhereCount = MQLUtil.execute(project, countQueryWithWhere);
            totalField.setText(withWhereCount+ "(" + totalCount + ")");
        } else {
            totalField.setText(totalCount);
        }
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
