package com.bol.spinner.editor;

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
        TYPE, RELATIONSHIP, PROGRAM
    }
}
