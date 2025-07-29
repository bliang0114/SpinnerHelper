package com.bol.spinner.ui;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

import javax.swing.text.Segment;

public class MQLTokenMaker extends AbstractTokenMaker {

    protected final String operators = "=~|><&+-/";

    private int currentTokenStart, currentTokenType;

    public MQLTokenMaker() {
        super();
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        switch (tokenType) {
            case Token.IDENTIFIER:
                var value = wordsToHighlight.get(segment, start, end);
                if (value != -1)
                    tokenType = value;
                break;

        }
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{"#", null};
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int type) {
        return type == Token.IDENTIFIER || type == Token.VARIABLE;
    }

    @Override
    public TokenMap getWordsToHighlight() {

        var tokenMap = new TokenMap(true); // Ignore case.
        var reservedWord1 = Token.DATA_TYPE;
        var reservedWord2 = Token.RESERVED_WORD;
        var reservedWord3 = Token.RESERVED_WORD_2;

        tokenMap.put("application", reservedWord1);
        tokenMap.put("association", reservedWord1);
        tokenMap.put("attribute", reservedWord1);
        tokenMap.put("bus", reservedWord1);
        tokenMap.put("businessobject", reservedWord1);
        tokenMap.put("channel", reservedWord1);
        tokenMap.put("command", reservedWord1);
        tokenMap.put("config", reservedWord1);
        tokenMap.put("connection", reservedWord1);
        tokenMap.put("context", reservedWord1);
        tokenMap.put("cue", reservedWord1);
        tokenMap.put("dataobject", reservedWord1);
        tokenMap.put("dimension", reservedWord1);
        tokenMap.put("expression", reservedWord1);
        tokenMap.put("filter", reservedWord1);
        tokenMap.put("form", reservedWord1);
        tokenMap.put("format", reservedWord1);
        tokenMap.put("group", reservedWord1);
        tokenMap.put("history", reservedWord1);
        tokenMap.put("index", reservedWord1);
        tokenMap.put("inquiry", reservedWord1);
        tokenMap.put("interface", reservedWord1);
        tokenMap.put("location", reservedWord1);
        tokenMap.put("mail", reservedWord1);
        tokenMap.put("memory", reservedWord1);
        tokenMap.put("menu", reservedWord1);
        tokenMap.put("page", reservedWord1);
        tokenMap.put("password", reservedWord1);
        tokenMap.put("person", reservedWord1);
        tokenMap.put("policy", reservedWord1);
        tokenMap.put("portal", reservedWord1);
        tokenMap.put("portlet", reservedWord1);
        tokenMap.put("product", reservedWord1);
        tokenMap.put("program", reservedWord1);
        tokenMap.put("property", reservedWord1);
        tokenMap.put("query", reservedWord1);
        tokenMap.put("rel", reservedWord1);
        tokenMap.put("relationship", reservedWord1);
        tokenMap.put("resource", reservedWord1);
        tokenMap.put("role", reservedWord1);
        tokenMap.put("rule", reservedWord1);
        tokenMap.put("searchindex", reservedWord1);
        tokenMap.put("server", reservedWord1);
        tokenMap.put("sessions", reservedWord1);
        tokenMap.put("set ", reservedWord1);
        tokenMap.put("site", reservedWord1);
        tokenMap.put("store", reservedWord1);
        tokenMap.put("table", reservedWord1);
        tokenMap.put("thread", reservedWord1);
        tokenMap.put("tip", reservedWord1);
        tokenMap.put("toolset", reservedWord1);
        tokenMap.put("trace", reservedWord1);
        tokenMap.put("transaction", reservedWord1);
        tokenMap.put("type", reservedWord1);
        tokenMap.put("uniquekey", reservedWord1);
        tokenMap.put("user", reservedWord1);
        tokenMap.put("vault", reservedWord1);
        tokenMap.put("view", reservedWord1);
        tokenMap.put("webreport", reservedWord1);
        tokenMap.put("wizard", reservedWord1);

        tokenMap.put("add", reservedWord2);
        tokenMap.put("check", reservedWord2);
        tokenMap.put("checkin", reservedWord2);
        tokenMap.put("checkout", reservedWord2);
        tokenMap.put("clear", reservedWord2);
        tokenMap.put("connect", reservedWord2);
        tokenMap.put("convert", reservedWord2);
        tokenMap.put("copy", reservedWord2);
        tokenMap.put("del", reservedWord2);
        tokenMap.put("delete", reservedWord2);
        tokenMap.put("disable", reservedWord2);
        tokenMap.put("disconnect", reservedWord2);
        tokenMap.put("download", reservedWord2);
        tokenMap.put("enable", reservedWord2);
        tokenMap.put("eval", reservedWord2);
        tokenMap.put("evaluate", reservedWord2);
        tokenMap.put("expand", reservedWord2);
        tokenMap.put("export", reservedWord2);
        tokenMap.put("freeze", reservedWord2);
        tokenMap.put("help", reservedWord2);
        tokenMap.put("import", reservedWord2);
        tokenMap.put("index", reservedWord2);
        tokenMap.put("inventory", reservedWord2);
        tokenMap.put("kill", reservedWord2);
        tokenMap.put("list", reservedWord2);
        tokenMap.put("lock", reservedWord2);
        tokenMap.put("mod", reservedWord2);
        tokenMap.put("modify", reservedWord2);
        tokenMap.put("monitor", reservedWord2);
        tokenMap.put("pop", reservedWord2);
        tokenMap.put("print", reservedWord2);
        tokenMap.put("purge", reservedWord2);
        tokenMap.put("push", reservedWord2);
        tokenMap.put("query", reservedWord2);
        tokenMap.put("rechecksum", reservedWord2);
        tokenMap.put("resume", reservedWord2);
        tokenMap.put("revise", reservedWord2);
        tokenMap.put("send", reservedWord2);
        tokenMap.put("set", reservedWord2);
        tokenMap.put("sort", reservedWord2);
        tokenMap.put("start", reservedWord2);
        tokenMap.put("status", reservedWord2);
        tokenMap.put("stop", reservedWord2);
        tokenMap.put("synch", reservedWord2);
        tokenMap.put("synchronize", reservedWord2);
        tokenMap.put("temp", reservedWord2);
        tokenMap.put("temporary", reservedWord2);
        tokenMap.put("tidy", reservedWord2);
        tokenMap.put("transition", reservedWord2);
        tokenMap.put("unlock", reservedWord2);
        tokenMap.put("update", reservedWord2);
        tokenMap.put("upload", reservedWord2);
        tokenMap.put("val", reservedWord2);
        tokenMap.put("validate", reservedWord2);
        tokenMap.put("zip", reservedWord2);

        tokenMap.put("dump", reservedWord3);
        tokenMap.put("from", reservedWord3);
        tokenMap.put("major", reservedWord3);
        tokenMap.put("minor", reservedWord3);
        tokenMap.put("output", reservedWord3);
        tokenMap.put("recordsep", reservedWord3);
        tokenMap.put("select", reservedWord3);
        tokenMap.put("selectable", reservedWord3);
        tokenMap.put("to", reservedWord3);
        tokenMap.put("tcl", reservedWord3);
        tokenMap.put("where", reservedWord3);

        return tokenMap;
    }

    @Override
    public Token getTokenList(Segment text, int startTokenType, final int startOffset) {

        resetTokenList();

        var array = text.array;
        var offset = text.offset;
        var count = text.count;
        var end = offset + count;
        var newStartOffset = startOffset - offset;
        currentTokenStart = offset;
        currentTokenType = startTokenType;

        for (var i = offset; i < end; i++) {

            var c = array[i];

            switch (currentTokenType) {

                case Token.NULL:

                    currentTokenStart = i;	// Starting a new token here.

                    switch (c) {

                        case ' ':
                        case '"':
                        case '\t':
                            currentTokenType = Token.WHITESPACE;
                            break;

                        case '\'':
                            currentTokenType = Token.ERROR_STRING_DOUBLE;
                            break;

                        // The "separators".
                        case '(':
                        case ')':
                        case '[':
                        case ']':
                        case '.':
                            addToken(text, currentTokenStart, i, Token.SEPARATOR, newStartOffset + currentTokenStart);
                            currentTokenType = Token.NULL;
                            break;

                        // The "separators2".
                        case ',':
                        case ';':
                            addToken(text, currentTokenStart, i, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            currentTokenType = Token.NULL;
                            break;

                        default:

                            // Just to speed things up a tad, as this will usually be the case (if spaces above failed).
                            if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\') {
                                currentTokenType = Token.IDENTIFIER;
                                break;
                            }

                            var indexOf = operators.indexOf(c, 0);
                            if (indexOf > -1) {
                                addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                                currentTokenType = Token.NULL;
                                break;
                            } else {
                                currentTokenType = Token.IDENTIFIER;
                                break;
                            }

                    } // End of switch (c).

                    break;

                case Token.WHITESPACE:

                    switch (c) {

                        case ' ':
                        case '"':
                        case '\t':
                            break;	// Still whitespace.

                        case '\'':
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = Token.ERROR_STRING_DOUBLE;
                            break;

                        // The "separators".
                        case '(':
                        case ')':
                        case '[':
                        case ']':
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                            addToken(text, i, i, Token.SEPARATOR, newStartOffset + i);
                            currentTokenType = Token.NULL;
                            break;

                        // The "separators2".
                        case ',':
                        case ';':
                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                            addToken(text, i, i, Token.IDENTIFIER, newStartOffset + i);
                            currentTokenType = Token.NULL;
                            break;

                        default:	// Add the whitespace token and start anew.

                            addToken(text, currentTokenStart, i - 1, Token.WHITESPACE, newStartOffset + currentTokenStart);
                            currentTokenStart = i;

                            // Just to speed things up a tad, as this will usually be the case (if spaces above failed).
                            if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\') {
                                currentTokenType = Token.IDENTIFIER;
                                break;
                            }

                            var indexOf = operators.indexOf(c, 0);
                            if (indexOf > -1) {
                                addToken(text, currentTokenStart, i, Token.OPERATOR, newStartOffset + currentTokenStart);
                                currentTokenType = Token.NULL;
                                break;
                            } else {
                                currentTokenType = Token.IDENTIFIER;
                            }

                    } // End of switch (c).

                    break;

                default: // Should never happen

                case Token.IDENTIFIER:

                    switch (c) {

                        case ' ':
                        case '"':
                        case '\t':
                            addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = Token.WHITESPACE;
                            break;

                        case '\'':
                            addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            currentTokenStart = i;
                            currentTokenType = Token.ERROR_STRING_DOUBLE;
                            break;

                        // The "separators".
                        case '(':
                        case ')':
                        case '[':
                        case ']':
                            addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            addToken(text, i, i, Token.SEPARATOR, newStartOffset + i);
                            currentTokenType = Token.NULL;
                            break;

                        case '.':
                            if (i > 0 && RSyntaxUtilities.isDigit(array[i - 1])
                                    || i < array.length - 1 && RSyntaxUtilities.isDigit(array[i + 1]))
                                break;

                            addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            addToken(text, i, i, Token.SEPARATOR, newStartOffset + i);
                            currentTokenType = Token.NULL;
                            break;

                        // The "separators2".
                        case ',':
                        case ';':
                            addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                            addToken(text, i, i, Token.IDENTIFIER, newStartOffset + i);
                            currentTokenType = Token.NULL;
                            break;

                        default:

                            // Just to speed things up a tad, as this will usually be the case.
                            if (RSyntaxUtilities.isLetterOrDigit(c) || c == '\\')
                                break;

                            var indexOf = operators.indexOf(c);
                            if (indexOf > -1) {
                                addToken(text, currentTokenStart, i - 1, Token.IDENTIFIER, newStartOffset + currentTokenStart);
                                addToken(text, i, i, Token.OPERATOR, newStartOffset + i);
                                currentTokenType = Token.NULL;
                                break;
                            }

                        // Otherwise, fall through and assume we're still okay as an IDENTIFIER...
                    } // End of switch (c).

                    break;

                case Token.COMMENT_EOL:
                    i = end - 1;
                    addToken(text, currentTokenStart, i, Token.COMMENT_EOL, newStartOffset + currentTokenStart);
                    // We need to set token type to null so at the bottom we don't add one more token.
                    currentTokenType = Token.NULL;
                    break;

                case Token.PREPROCESSOR: // Used for labels
                    i = end - 1;
                    addToken(text, currentTokenStart, i, Token.PREPROCESSOR, newStartOffset + currentTokenStart);
                    // We need to set token type to null so at the bottom we don't add one more token.
                    currentTokenType = Token.NULL;
                    break;

                case Token.ERROR_STRING_DOUBLE:
                    if (c == '\'') {
                        addToken(text, currentTokenStart, i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset + currentTokenStart);
                        currentTokenStart = i + 1;
                        currentTokenType = Token.NULL;
                    }
                    // Otherwise, we're still an unclosed string...
                    break;

            } // End of switch (currentTokenType).

        } // End of for (int i=offset; i<end; i++).

        // Deal with the (possibly there) last token.
        if (currentTokenType != Token.NULL) {
            addToken(text, currentTokenStart, end - 1, currentTokenType, newStartOffset + currentTokenStart);
        }

        addNullToken();

        // Return the first token in our linked list.
        return firstToken;

    }

}
