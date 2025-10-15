package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.AttributesRow;
import com.bol.spinner.editor.ui.dataview.bean.InterfacesRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class InterfacesTableComponent extends AbstractDataViewTableComponent<InterfacesRow, InterfacesTableComponent> {
    private JBCheckBox checkBox;

    public InterfacesTableComponent(VirtualFile virtualFile) {
        super(virtualFile,
                new Object[]{"Interface Name", "Attribute Name", "Attribute Owner", "Type", "Default", "Range"},
                new int[]{260, 260, 260, 150, 150, 500},
                "Interfaces Table Toolbar");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected JComponent getToolbarComponent() {
        checkBox = new JBCheckBox("Include non-automatic interface");
        JComponent toolbarComponent = super.getToolbarComponent();
        toolbarComponent.add(checkBox, BorderLayout.CENTER);
        return toolbarComponent;
    }

    @Override
    protected List<Function<InterfacesRow, String>> getFilterFunctions() {
        return List.of(InterfacesRow::getInterfaceName, InterfacesRow::getAttributeName, InterfacesRow::getAttributeOwner, InterfacesRow::getType, InterfacesRow::getDefaultValue, InterfacesRow::getRange);
    }

    @Override
    protected void addRow(InterfacesRow row) {
        tableModel.addRow(new Object[]{row.getInterfaceName(), row.getAttributeName(), row.getAttributeOwner(), row.getType(), row.getDefaultValue(), row.getRange()});
    }

    @Override
    protected List<InterfacesRow> loadDataFromMatrix() throws MQLException {
        boolean includeAutomatic = checkBox.isSelected();
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
            var listWhereExpression = "(type == '" + name + "' || type.derivative == '" + name + "')";
            listWhereExpression += includeAutomatic ? "" : " && property[IPML.Automatic].value == Yes";
            return loadInterfacesFromMatrix(listWhereExpression);
        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
            var listWhereExpression = "(relationship == '" + name + "' || relationship.derivative == '" + name + "')";
            listWhereExpression += includeAutomatic ? "" : " && property[IPML.Automatic].value == Yes";
            return loadInterfacesFromMatrix(listWhereExpression);
        }
        return Collections.emptyList();
    }

    private List<InterfacesRow> loadInterfacesFromMatrix(String whereExpression) throws MQLException {
        List<InterfacesRow> list = new ArrayList<>();
        var result = MQLUtil.execute("list interface * where \"" + whereExpression + "\" select name dump");
        var interfaceList = CharSequenceUtil.split(result, "\n");
        interfaceList = interfaceList.stream().filter(CharSequenceUtil::isNotBlank).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        for (var interfaceName : interfaceList) {
            result = MQLUtil.execute("print interface '" + interfaceName + "' select attribute.owner attribute dump");
            var attributes = CharSequenceUtil.split(result, ",");
            var midIndex = attributes.size() / 2;
            List<InterfacesRow> singleList = new ArrayList<>();
            for (var i = 0; i < midIndex; i++) {
                singleList.add(new InterfacesRow(interfaceName, attributes.get(i + midIndex), attributes.get(i)));
            }
            list.addAll(singleList.stream().sorted(Comparator.comparing(row -> row.getAttributeOwner() + "." + row.getAttributeName(), String.CASE_INSENSITIVE_ORDER)).toList());
        }
        for (InterfacesRow row : list) {
            var attribute = CharSequenceUtil.isNotBlank(row.getAttributeOwner()) ? row.getAttributeOwner() + "." + row.getAttributeName() : row.getAttributeName();
            result = MQLUtil.execute("print attribute '" + attribute + "' select name owner type default multiline range dump");
            var s = result.split(",", 6);
            if (s[2].equals("string") && s[4].equalsIgnoreCase("TRUE")) {
                s[2] = "string multiline";
            }
            if (s.length > 5) {
                s[4] = s[5];
            } else {
                s[4] = "";
            }
            row.setType(s[2]);
            row.setDefaultValue(s[3]);
            row.setRange(s[4]);
        }
        return list;
    }
}
