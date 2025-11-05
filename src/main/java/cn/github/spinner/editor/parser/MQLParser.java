package cn.github.spinner.editor.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class MQLParser implements PsiParser {
    @NotNull
    @Override
    public ASTNode parse(@NotNull IElementType root, PsiBuilder builder) {
        // 简化解析器实现
        PsiBuilder.Marker rootMarker = builder.mark();

        while (!builder.eof()) {
            IElementType token = builder.getTokenType();
            if (token != null) {
                builder.advanceLexer();
            } else {
                break;
            }
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}