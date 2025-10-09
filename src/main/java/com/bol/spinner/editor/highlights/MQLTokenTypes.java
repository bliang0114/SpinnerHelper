package com.bol.spinner.editor.highlights;

import com.bol.spinner.editor.MQLLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MQLTokenTypes extends IElementType {
    public static final IElementType KEYWORD = new MQLTokenTypes("KEYWORD");
    public static final IElementType FUNCTION = new MQLTokenTypes("FUNCTION");
    public static final IElementType NUMBER = new MQLTokenTypes("NUMBER");
    public static final IElementType STRING = new MQLTokenTypes("STRING");
    public static final IElementType COMMENT = new MQLTokenTypes("COMMENT");
    public static final IElementType PREPROCESSOR = new MQLTokenTypes("PREPROCESSOR");
    public static final IElementType TYPE = new MQLTokenTypes("TYPE");
    public static final IElementType IDENTIFIER = new MQLTokenTypes("IDENTIFIER");
    public static final IElementType OPERATOR = new MQLTokenTypes("OPERATOR");
    public static final IElementType CONSTANT = new MQLTokenTypes("CONSTANT");
    public static final IElementType PAREN = new MQLTokenTypes("PAREN");
    public static final IElementType BRACE = new MQLTokenTypes("BRACE");
    public static final IElementType BRACKET = new MQLTokenTypes("BRACKET");
    public static final IElementType SEMICOLON = new MQLTokenTypes("SEMICOLON");
    public static final IElementType COMMA = new MQLTokenTypes("COMMA");
    public static final IElementType DOT = new MQLTokenTypes("DOT");

    public MQLTokenTypes(@NotNull @NonNls String debugName) {
        super(debugName, MQLLanguage.INSTANCE);
    }

    public static class Factory {
        public static com.intellij.psi.PsiElement createElement(ASTNode node) {
            IElementType type = node.getElementType();
            // 简化实现，返回基础PSI元素
            return new LeafPsiElement(type, type.toString());
        }
    }
}