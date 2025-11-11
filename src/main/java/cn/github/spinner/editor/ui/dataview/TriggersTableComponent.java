package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.MatrixDataViewFileType;
import cn.github.spinner.editor.ui.dataview.bean.TriggersRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class TriggersTableComponent extends AbstractDataViewTableComponent<TriggersRow, TriggersTableComponent> {
    private static final Object[] COLUMNS = new Object[]{"Trigger Name", "Inherited", "Check", "Override", "Action"};
    private static final int[] COLUMN_WIDTHS = new int[]{200, 100, 300, 300, 300};

    public TriggersTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) {
                    return Boolean.class;
                }
                return String.class;
            }
        }, COLUMN_WIDTHS, "Triggers Table Toolbar");
    }

    @Override
    protected void addRow(TriggersRow row) {
        tableModel.addRow(new Object[]{row.getTriggerName(), row.isInherited(), row.getCheck(), row.getOverride(), row.getAction()});
    }

    @Override
    protected List<TriggersRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
            var tiggerMQL = "print type '" + name + "' select trigger dump";
            var derivedTiggerMQL = "print type '" + name + "' select derived.trigger dump";
            return loadFromMatrix(tiggerMQL, derivedTiggerMQL);
        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
            var tiggerMQL = "print relationship '" + name + "' select trigger dump";
            var derivedTiggerMQL = "print relationship '" + name + "' select derived.trigger dump";
            return loadFromMatrix(tiggerMQL, derivedTiggerMQL);
        }
        return Collections.emptyList();
    }

    private List<TriggersRow> loadFromMatrix(String tiggerMQL, String derivedTiggerMQL) throws MQLException {
        var result = MQLUtil.execute(project, tiggerMQL);
        var derived = MQLUtil.execute(project, derivedTiggerMQL);
        var list = CharSequenceUtil.split(result, ",");
        list = list.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<TriggersRow> dataList = new ArrayList<>();
        for (String str : list) {
            var strSplit = str.split(":");
            var prog = strSplit[1].substring(0, strSplit[1].indexOf("("));
            var param = strSplit[1].substring(strSplit[1].indexOf("(") + 1, strSplit[1].indexOf(")"));
            String[] params;
            if (prog.equals("emxTriggerManager")) {
                prog = "TM";
                params = param.split(" ");
            } else {
                params = new String[]{param};
            }
            for (var param1 : params) {
                if (param1.trim().isEmpty()) {
                    continue;
                }
                var row = new TriggersRow();
                if (strSplit[0].endsWith("Check")) {
                    row.setTriggerName(strSplit[0].substring(0, strSplit[0].length() - 5));
                    row.setCheck(prog + ": " + param1);
                } else if (strSplit[0].endsWith("Action")) {
                    row.setTriggerName(strSplit[0].substring(0, strSplit[0].length() - 6));
                    row.setAction(prog + ": " + param1);
                } else if (strSplit[0].endsWith("Override")) {
                    row.setTriggerName(strSplit[0].substring(0, strSplit[0].length() - 8));
                    row.setOverride(prog + ": " + param1);
                }
                row.setInherited(derived.matches(".*" + strSplit[0] + ":.*[^\\w]" + param1 + "[^\\w].*"));
                dataList.add(row);
            }
        }
        return dataList;
    }
}
