package cn.github.spinner.editor.parser;

import cn.github.spinner.editor.MQLFileType;
import cn.github.spinner.editor.MQLLanguage;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class MQLFile extends PsiFileBase {
    public MQLFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MQLLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MQLFileType.INSTANCE;
    }
}