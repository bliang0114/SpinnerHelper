package com.bol.spinner.editor.highlights;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class MQLSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey KEYWORD = createTextAttributesKey("MQL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey FUNCTION = createTextAttributesKey("MQL_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION);
    public static final TextAttributesKey NUMBER = createTextAttributesKey("MQL_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = createTextAttributesKey("MQL_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey COMMENT = createTextAttributesKey("MQL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey PREPROCESSOR = createTextAttributesKey("MQL_PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey TYPE = createTextAttributesKey("MQL_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey CONSTANT = createTextAttributesKey("MQL_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("MQL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
    private static final TextAttributesKey[] FUNCTION_KEYS = new TextAttributesKey[]{FUNCTION};
    private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
    private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] PREPROCESSOR_KEYS = new TextAttributesKey[]{PREPROCESSOR};
    private static final TextAttributesKey[] TYPE_KEYS = new TextAttributesKey[]{TYPE};
    private static final TextAttributesKey[] CONSTANT_KEYS = new TextAttributesKey[]{CONSTANT};
    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MQLLexerAdapter();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(MQLTokenTypes.KEYWORD)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.FUNCTION)) {
            return FUNCTION_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.NUMBER)) {
            return NUMBER_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.COMMENT)) {
            return COMMENT_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.PREPROCESSOR)) {
            return PREPROCESSOR_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.TYPE)) {
            return TYPE_KEYS;
        } else if (tokenType.equals(MQLTokenTypes.CONSTANT)) {
            return CONSTANT_KEYS;
        } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        } else {
            return EMPTY_KEYS;
        }
    }

    public static class MQLLexerAdapter extends com.intellij.lexer.FlexAdapter {
        public MQLLexerAdapter() {
            super(new _MQLLexer(null));
        }
    }
}
