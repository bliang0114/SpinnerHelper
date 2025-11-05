package cn.github.spinner.editor;

import cn.github.spinner.editor.icons.MQLIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MQLFileType extends LanguageFileType {
    public static final MQLFileType INSTANCE = new MQLFileType();

    private MQLFileType() {
        super(MQLLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "MQL File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "MQL language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "mql";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return MQLIcons.FILE;
    }
}
