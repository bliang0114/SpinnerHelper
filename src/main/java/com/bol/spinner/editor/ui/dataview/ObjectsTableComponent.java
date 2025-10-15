package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixObjectQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.ObjectsRow;
import com.bol.spinner.editor.ui.dataview.bean.PropertiesRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class ObjectsTableComponent extends AbstractDataViewTableComponent<ObjectsRow, ObjectsTableComponent> {
    private JBTextField limitField;
    private JBTextField totalField;

    public ObjectsTableComponent(VirtualFile virtualFile) {
        super(virtualFile, new DefaultTableModel(new Object[]{"Type", "Name", "Revision", "ID", "Path", "PhysicalID", "Description", "Originated", "Modified", "Vault", "Policy", "Owner", "State", "Organization", "Collaborative Space"}, 0) {
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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
    protected List<ObjectsRow> loadDataFromMatrix() throws MQLException {
        String totalCount = MQLUtil.execute("eval expr 'count TRUE' on temp query bus '{}' * *", name);
        totalField.setText(totalCount);
        MatrixObjectQuery objectQuery = new MatrixObjectQuery();
        objectQuery.setType(name);
        objectQuery.setExpandType(true);
        objectQuery.setLimit(Short.parseShort(limitField.getText()));
        MatrixQueryResult queryResult = SpinnerToken.connection.queryObject(objectQuery, List.of("type", "name", "revision", "id", "paths", "physicalid", "description", "originated", "modified", "lattice", "policy", "owner", "current", "organization", "project"));
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
