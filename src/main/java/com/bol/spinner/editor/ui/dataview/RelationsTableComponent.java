package com.bol.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.editor.MatrixDataViewFileType;
import com.bol.spinner.editor.ui.dataview.bean.PropertiesRow;
import com.bol.spinner.editor.ui.dataview.bean.RelationsRow;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class RelationsTableComponent extends AbstractDataViewTableComponent<RelationsRow, RelationsTableComponent> {

    public RelationsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new Object[]{"Relationship", "Direction", "Type / Relationship"}, new int[]{200, 50, 400}, "Relations Table Toolbar");
    }

    @Override
    protected List<Function<RelationsRow, String>> getFilterFunctions() {
        return List.of(RelationsRow::getRelationship, RelationsRow::getDirection, RelationsRow::getTypeOrRelationship);
    }

    @Override
    protected void addRow(RelationsRow row) {
        tableModel.addRow(new Object[]{row.getRelationship(), row.getDirection(), row.getTypeOrRelationship()});
    }

    @Override
    protected List<RelationsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        MatrixDataViewFileType fileType = (MatrixDataViewFileType) virtualFile.getFileType();
        if (MatrixDataViewFileType.ViewType.TYPE == fileType.getViewType()) {
            return loadFromMatrixType();
        } else if (MatrixDataViewFileType.ViewType.RELATIONSHIP == fileType.getViewType()) {
            return loadFromMatrixRelationship();
        }
        return Collections.emptyList();
    }

    private List<RelationsRow> loadFromMatrixType() throws MQLException {
        var result = MQLUtil.execute(project, "list relationship select name totype[{}] totype.derivative[{}] fromtype[{}] fromtype.derivative[{}] dump", name, name, name, name);
        var list = CharSequenceUtil.split(result, "\n");
        List<String> relationshipNames = new ArrayList<>();
        for (var str : list) {
            if ((str + ",").contains(",TRUE,")) {
                relationshipNames.add(str.split(",")[0]);
            }
        }
        relationshipNames = relationshipNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<RelationsRow> dataList = new ArrayList<>();
        for (var relationshipName : relationshipNames) {
            var fromType = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select fromtype fromtype.derivative dump");
            var toType = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select totype totype.derivative dump");
            if (("," + fromType + ",").contains("," + name + ",") || fromType.equals("all")) {
                if (!toType.isEmpty()) {
                    if (toType.equals("all")) {
                        toType = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "To", toType.replace(",", ",\n")));
                }
                var toRel = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select torel torel.derivative dump");
                if (!toRel.isEmpty()) {
                    if (toRel.equals("all")) {
                        toRel = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "To Rel", toRel.replace(",", ",\n")));
                }
            }
            if (("," + toType + ",").contains("," + name + ",") || toType.equals("all")) {
                if (!fromType.isEmpty()) {
                    if (fromType.equals("all")) {
                        fromType = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "From", fromType.replace(",", ",\n")));
                }
                var fromRel = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select fromrel fromrel.derivative dump");
                if (!fromRel.isEmpty()) {
                    if (fromRel.equals("all")) {
                        fromRel = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "From Rel", fromRel.replace(",", ",\n")));
                }
            }
        }
        return dataList;
    }

    private List<RelationsRow> loadFromMatrixRelationship() throws MQLException {
        var result = MQLUtil.execute(project, "list relationship select name torel[{}] torel.derivative[{}] fromrel[{}] fromrel.derivative[{}] dump", name, name, name, name);
        var list = CharSequenceUtil.split(result, "\n");
        List<String> relationshipNames = new ArrayList<>();
        for (var str : list) {
            if ((str + ",").contains(",TRUE,")) {
                relationshipNames.add(str.split(",")[0]);
            }
        }
        relationshipNames = relationshipNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<RelationsRow> dataList = new ArrayList<>();
        for (var relationshipName : relationshipNames) {
            var fromRel = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select fromrel fromrel.derivative dump");
            var toRel = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select torel torel.derivative dump");
            if (("," + fromRel + ",").contains("," + name + ",") || fromRel.equals("all")) {
                if (!toRel.isEmpty()) {
                    if (toRel.equals("all")) {
                        toRel = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "To Rel", toRel.replace(",", ",\n")));
                }
                var toType = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select totype totype.derivative dump");
                if (!toType.isEmpty()) {
                    if (toType.equals("all")) {
                        toType = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "To", toType.replace(",", ",\n")));
                }
            }
            if (("," + toRel + ",").contains("," + name + ",") || toRel.equals("all")) {
                if (!fromRel.isEmpty()) {
                    if (fromRel.equals("all")) {
                        fromRel = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "From Rel", fromRel.replace(",", ",\n")));
                }
                var fromType = MQLUtil.execute(project, "print relationship '" + relationshipName + "' select fromtype fromtype.derivative dump");
                if (!fromType.isEmpty()) {
                    if (fromType.equals("all")) {
                        fromType = "** All **";
                    }
                    dataList.add(new RelationsRow(relationshipName, "From", fromType.replace(",", ",\n")));
                }
            }
        }
        return dataList;
    }
}
