package com.bol.spinner.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;

import java.util.ArrayList;
import java.util.List;

public class EditorUtil {

    public static String getSelectedText(Editor editor) {
        return editor.getSelectionModel().getSelectedText();
    }

    /**
     * 获取选中行的内容（支持多行选择）
     *
     * @return {@link List}
     * @author zaydenwang
     */
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
                String lineText = getLineContent(document, i);
                selectedLines.add(lineText);
            }
        }
        return selectedLines;
    }

    public static List<String> getAllLines(Editor editor) {
        List<String> lines = new ArrayList<>();
        Document document = editor.getDocument();
        int lineCount = document.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            String line = getLineContent(document, i);
            lines.add(line);
        }
        return lines;
    }

    private static String getLineContent(Document document, int lineNumber) {
        try {
            int lineStart = document.getLineStartOffset(lineNumber);
            int lineEnd = document.getLineEndOffset(lineNumber);
            return document.getText().substring(lineStart, lineEnd).trim();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }
}
