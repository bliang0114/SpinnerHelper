package cn.github.spinner.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MatrixDataViewFileType implements FileType {
    public static final MatrixDataViewFileType TYPE = new MatrixDataViewFileType(ViewType.TYPE);
    public static final MatrixDataViewFileType RELATIONSHIP = new MatrixDataViewFileType(ViewType.RELATIONSHIP);
    public static final MatrixDataViewFileType PROGRAM = new MatrixDataViewFileType(ViewType.PROGRAM);
    public static final MatrixDataViewFileType FORM_TABLE = new MatrixDataViewFileType(ViewType.FORM_TABLE);
    public static final MatrixDataViewFileType MENU_COMMAND = new MatrixDataViewFileType(ViewType.MENU_COMMAND);
    public static final MatrixDataViewFileType OBJECT_BROWSER = new MatrixDataViewFileType(ViewType.OBJECT_BROWSER);

    @Getter
    private final ViewType viewType;

    public MatrixDataViewFileType(ViewType viewType) {
        this.viewType = viewType;
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Data View";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Matrix Data View";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "matrix_data";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Debugger.Db_db_object;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    public enum ViewType {
        TYPE, RELATIONSHIP, PROGRAM,FORM_TABLE,MENU_COMMAND, OBJECT_BROWSER
    }
}
