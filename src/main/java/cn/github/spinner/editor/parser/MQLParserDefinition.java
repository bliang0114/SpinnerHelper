package cn.github.spinner.editor.parser;

import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.editor.highlights.MQLSyntaxHighlighter;
import cn.github.spinner.editor.highlights.MQLTokenTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class MQLParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(MQLLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new MQLSyntaxHighlighter.MQLLexerAdapter();
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.create(MQLTokenTypes.COMMENT);
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.create(MQLTokenTypes.STRING);
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        return new MQLParser();
    }

    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MQLFile(viewProvider);
    }

    @NotNull
    @Override
    public ParserDefinition.SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return MQLTokenTypes.Factory.createElement(node);
    }
}
