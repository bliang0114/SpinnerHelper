package cn.github.spinner.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

public class EditorUtil {

    public static String getSelectedText(Editor editor) {
        return editor.getSelectionModel().getSelectedText();
    }

    public static String getLineContent(Editor editor) {
        int lineNumber = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
        if (lineNumber < 0) {
            return "";
        }
        return getLineContent(editor.getDocument(), lineNumber);
    }

    public static String getLineContent(Editor editor, int lineNumber) {
        return getLineContent(editor.getDocument(), lineNumber);
    }

    public static List<String> getSelectedLines(Editor editor) {
        List<String> selectedLines = new ArrayList<>();
        SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.hasSelection()) {
            Document document = editor.getDocument();
            int startOffset = selectionModel.getSelectionStart();
            int endOffset = selectionModel.getSelectionEnd();
            int startLine = document.getLineNumber(startOffset);
            int endLine = document.getLineNumber(endOffset);
            for (int i = startLine; i <= endLine; i++) {
                selectedLines.add(getLineContent(document, i));
            }
        }
        return selectedLines;
    }

    public static List<String> getAllLines(Editor editor) {
        List<String> lines = new ArrayList<>();
        Document document = editor.getDocument();
        int lineCount = document.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            lines.add(getLineContent(document, i));
        }
        return lines;
    }

    private static String getLineContent(Document document, int lineNumber) {
        try {
            int lineStart = document.getLineStartOffset(lineNumber);
            int lineEnd = document.getLineEndOffset(lineNumber);
            return document.getText(new TextRange(lineStart, lineEnd));
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
