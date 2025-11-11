package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.MatrixDataViewFileType;
import cn.github.spinner.editor.ui.dataview.bean.PropertiesRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class PropertiesTableComponent extends AbstractDataViewTableComponent<PropertiesRow> {
    public PropertiesTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new PropertiesRow(), "Properties Table");
    }

    @Override
    protected List<PropertiesRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
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
        var result = MQLUtil.execute(project, listPropertyMQL);
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
