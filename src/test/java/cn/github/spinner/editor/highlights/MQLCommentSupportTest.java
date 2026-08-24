package cn.github.spinner.editor.highlights;

import cn.github.spinner.editor.MQLCommenter;
import cn.github.spinner.editor.MQLFileType;
import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

public class MQLCommentSupportTest extends BasePlatformTestCase {
    public void testHighlightsSlashLineAndBlockComments() {
        Lexer lexer = new MQLSyntaxHighlighter().getHighlightingLexer();
        lexer.start("// line\n/* block */\n/** documentation */");

        List<IElementType> nonWhitespaceTokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                nonWhitespaceTokens.add(lexer.getTokenType());
            }
            lexer.advance();
        }

        assertEquals(List.of(
                MQLTokenTypes.COMMENT,
                MQLTokenTypes.COMMENT,
                MQLTokenTypes.COMMENT
        ), nonWhitespaceTokens);
    }

    public void testExposesSlashCommentDelimitersToEditorActions() {
        MQLCommenter commenter = new MQLCommenter();

        assertEquals("//", commenter.getLineCommentPrefix());
        assertEquals("/*", commenter.getBlockCommentPrefix());
        assertEquals("*/", commenter.getBlockCommentSuffix());
    }

    public void testGeneratesDocumentationCommentBodyOnEnter() {
        myFixture.configureByText(MQLFileType.INSTANCE, "/**<caret>");

        myFixture.type('\n');

        myFixture.checkResult("/**\n * <caret>\n */");
    }

    public void testGeneratesBlockCommentBodyOnEnter() {
        myFixture.configureByText(MQLFileType.INSTANCE, "    /*<caret>");

        myFixture.type('\n');

        myFixture.checkResult("    /*\n     * <caret>\n     */");
    }

    public void testContinuesBlockCommentLineOnEnter() {
        myFixture.configureByText(MQLFileType.INSTANCE, "/**\n * description<caret>\n */");

        myFixture.type('\n');

        assertEquals("/**\n * description\n * \n */", myFixture.getEditor().getDocument().getText());
        assertEquals(22, myFixture.getEditor().getCaretModel().getOffset());
    }
}
