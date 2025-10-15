package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.PolicyTriggersRow;
import com.bol.spinner.editor.ui.dataview.bean.PropertiesRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class PolicyTriggersTableComponent extends AbstractDataViewTableComponent<PolicyTriggersRow, PolicyTriggersTableComponent> {

    public PolicyTriggersTableComponent(VirtualFile virtualFile) {
        super(virtualFile, new Object[]{"Policy", "State", "Trigger Name", "Check", "Override", "Action"}, new int[]{200, 100, 100, 300, 300, 300}, "Policy Triggers Table Toolbar");
    }

    @Override
    protected List<Function<PolicyTriggersRow, String>> getFilterFunctions() {
        return List.of(PolicyTriggersRow::getPolicy, PolicyTriggersRow::getState, PolicyTriggersRow::getTriggerName, PolicyTriggersRow::getCheck, PolicyTriggersRow::getOverride, PolicyTriggersRow::getAction);
    }

    @Override
    protected void addRow(PolicyTriggersRow row) {
        tableModel.addRow(new Object[]{row.getPolicy(), row.getState(), row.getTriggerName(), row.getCheck(), row.getOverride(), row.getAction()});
    }

    @Override
    protected List<PolicyTriggersRow> loadDataFromMatrix() throws MQLException {
//        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
//        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
//            var listPropertyMQL = "list type '" + name + "' select property dump";
//            return loadPropertiesFromMatrix(listPropertyMQL);
//        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
//            var listPropertyMQL = "list relationship '" + name + "' select property dump";
//            return loadPropertiesFromMatrix(listPropertyMQL);
//        }
        return Collections.emptyList();
    }
}
