package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.PropertiesRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class PropertiesTableComponent extends AbstractDataViewTableComponent<PropertiesRow, PropertiesTableComponent> {

    public PropertiesTableComponent(VirtualFile virtualFile) {
        super(virtualFile, new Object[]{"Name", "Value"}, new int[]{200, 200}, "Properties Table Toolbar");
    }

    @Override
    protected List<Function<PropertiesRow, String>> getFilterFunctions() {
        return List.of(PropertiesRow::getName, PropertiesRow::getValue);
    }

    @Override
    protected void addRow(PropertiesRow row) {
        tableModel.addRow(new Object[]{row.getName(), row.getValue()});
    }

    @Override
    protected List<PropertiesRow> loadDataFromMatrix() throws MQLException {
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
            var listPropertyMQL = "list type '" + name + "' select property dump";
            return loadPropertiesFromMatrix(listPropertyMQL);
        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
            var listPropertyMQL = "list relationship '" + name + "' select property dump";
            return loadPropertiesFromMatrix(listPropertyMQL);
        }
        return Collections.emptyList();
    }

    private List<PropertiesRow> loadPropertiesFromMatrix(String listPropertyMQL) throws MQLException {
        var result = MQLUtil.execute(listPropertyMQL);
        var list = CharSequenceUtil.split(result, ",");
        list = list.stream().filter(CharSequenceUtil::isNotBlank).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<PropertiesRow> dataList = new ArrayList<>();
        for (var item : list) {
            var s = item.split(" value ", 2);
            if (s.length == 1) {
                s = item.split(" to menu ", 2);
            }
            dataList.add(new PropertiesRow(s[0], s[1]));
        }
        return dataList;
    }
}
