package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.ObjectsRow;
import com.bol.spinner.editor.ui.dataview.bean.PropertiesRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class ObjectsTableComponent extends AbstractDataViewTableComponent<ObjectsRow, ObjectsTableComponent> {

    public ObjectsTableComponent(VirtualFile virtualFile) {
        super(virtualFile,
                new Object[]{"Type", "Name", "Revision", "ID", "Path", "PhysicalID", "Description", "Originated", "Modified", "Vault", "Policy", "Owner", "State", "Organization", "Collaborative Space"},
                new int[]{200, 200, 100, 200, 60, 200, 200, 150, 150, 150, 200, 100, 100, 150}, "Objects Table Toolbar");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
        tableModel.addRow(new Object[]{row.getType(), row.getName(), row.getRevision(), row.getId(), row.getPath(), row.getPhysicalId(), row.getDescription(), row.getOriginated(), row.getModified(), row.getVault(), row.getPolicy(), row.getOwner(), row.getState(), row.getOrganization(), row.getCollaborativeSpace()});
    }

    @Override
    protected List<ObjectsRow> loadDataFromMatrix() throws MQLException {

        return Collections.emptyList();
    }
}
