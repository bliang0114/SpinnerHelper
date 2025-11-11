package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.MatrixDataViewFileType;
import cn.github.spinner.editor.ui.dataview.bean.AttributesRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBCheckBox;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class AttributesTableComponent extends AbstractDataViewTableComponent<AttributesRow, AttributesTableComponent> {
    private static final Object[] COLUMNS = new Object[]{"Name", "Owner", "Type", "Default", "Range"};
    private static final int[] COLUMN_WIDTHS = new int[]{260, 260, 100, 150, 500};
    private JBCheckBox checkBox;

    public AttributesTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, COLUMNS, COLUMN_WIDTHS, "Attributes Table Toolbar");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected Component[] createToolbarComponent() {
        checkBox = new JBCheckBox("Include non-automatic interface");
        return new Component[] { table.getFilterComponent(), checkBox };
    }

    @Override
    protected void addRow(AttributesRow row) {
        tableModel.addRow(new Object[]{row.getName(), row.getOwner(), row.getType(), row.getDefaultValue(), row.getRange()});
    }

    @Override
    protected List<AttributesRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        boolean includeAutomatic = checkBox.isSelected();
        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
            var printAttributeMQL = "print type '" + name + "' select attribute.owner attribute dump";
            var listWhereExpression = "(type == '" + name + "' || type.derivative == '" + name + "')";
            listWhereExpression += includeAutomatic ? "" : " && property[IPML.Automatic].value == Yes";
            return loadAttributesFromMatrix(printAttributeMQL, listWhereExpression);
        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
            var printAttributeMQL = "print relationship '" + name + "' select attribute.owner attribute dump";
            var listWhereExpression = "(relationship == '" + name + "' || relationship.derivative == '" + name + "')";
            listWhereExpression += includeAutomatic ? "" : " && property[IPML.Automatic].value == Yes";
            return loadAttributesFromMatrix(printAttributeMQL, listWhereExpression);
        }
        return Collections.emptyList();
    }

    private List<AttributesRow> loadAttributesFromMatrix(String printAttributeMQL, String listWhereExpression) throws MQLException {
        List<AttributesRow> dataList = new ArrayList<>(printAttributes(printAttributeMQL));
        dataList.addAll(listInterfaceAttributes(listWhereExpression));
        dataList = dataList.stream().distinct().toList();
        for (var row : dataList) {
            var attribute = CharSequenceUtil.isNotBlank(row.getOwner()) ? row.getOwner() + "." + row.getName() : row.getName();
            var result = MQLUtil.execute(project, "print attribute '" + attribute + "' select name owner type default multiline range dump");
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
        return dataList;
    }

    private List<AttributesRow> printAttributes(String printMQL) throws MQLException {
        List<AttributesRow> list = new ArrayList<>();
        var result = MQLUtil.execute(project, printMQL);
        var resultSplit = CharSequenceUtil.split(result, ",");
        var midIndex = resultSplit.size() / 2;
        for (var i = 0; i < midIndex; i++) {
            list.add(new AttributesRow(resultSplit.get(i + midIndex), resultSplit.get(i)));
        }
        list = list.stream().sorted(Comparator.comparing(AttributesRow::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        return list;
    }

    private List<AttributesRow> listInterfaceAttributes(String whereExpression) throws MQLException {
        List<AttributesRow> list = new ArrayList<>();
        var result = MQLUtil.execute(project, "list interface * where \"" + whereExpression + "\" select attribute.owner attribute dump");
        var resultSplit = CharSequenceUtil.split(result, "\n");
        for (var item : resultSplit) {
            var itemSplit = CharSequenceUtil.split(item, ",");
            var midIndex = itemSplit.size() / 2;
            for (var i = 0; i < midIndex; i++) {
                list.add(new AttributesRow(itemSplit.get(i + midIndex), itemSplit.get(i)));
            }
        }
        list = list.stream().sorted(Comparator.comparing(AttributesRow::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        return list;
    }
}
