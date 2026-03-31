package cn.github.spinner.util;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsolePrinter {
    private final ConsoleView consoleView;
    private final Document resultDocument;
    private final EditorEx resultEditor;
    private final List<RangeHighlighter> resultHighlighters = new ArrayList<>();

    public ConsolePrinter(Project project, ConsoleView consoleView) {
        this.consoleView = consoleView;
        this.resultDocument = EditorFactory.getInstance().createDocument("");
        this.resultEditor = (EditorEx) EditorFactory.getInstance().createViewer(resultDocument, project);
        this.resultEditor.setHorizontalScrollbarVisible(true);
        this.resultEditor.setVerticalScrollbarVisible(true);
        this.resultEditor.setCaretEnabled(true);
        this.resultEditor.setEmbeddedIntoDialogWrapper(false);
        this.resultEditor.setBorder(JBUI.Borders.empty());
        this.resultEditor.getSettings().setLineMarkerAreaShown(false);
        this.resultEditor.getSettings().setFoldingOutlineShown(false);
        this.resultEditor.getSettings().setRightMarginShown(false);
        this.resultEditor.getSettings().setAdditionalColumnsCount(1);
        this.resultEditor.getSettings().setAdditionalLinesCount(1);
        this.resultEditor.setBackgroundColor(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
    }

    public void print(String message) {
        print(message, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public void print(String message, ConsoleViewContentType contentType) {
        ApplicationManager.getApplication().invokeLater(() -> runUndoTransparentWriteAction(() -> {
            consoleView.print(message + "\n", contentType);
            appendToResultEditor(message, contentType);
        }));
    }

    public void clear() {
        ApplicationManager.getApplication().invokeLater(() -> runUndoTransparentWriteAction(() -> {
            consoleView.clear();
            resultDocument.setText("");
            clearResultHighlighters();
        }));
    }

    public int printSync(String message) {
        return printSync(message, ConsoleViewContentType.NORMAL_OUTPUT);
    }

    public int printSync(String message, ConsoleViewContentType contentType) {
        AtomicInteger offset = new AtomicInteger();
        Runnable task = () -> runUndoTransparentWriteAction(() -> {
            offset.set(resultDocument.getTextLength());
            consoleView.print(message + "\n", contentType);
            appendToResultEditor(message, contentType);
        });
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(task);
        }
        return offset.get();
    }

    public int getCurrentOffset() {
        AtomicInteger offset = new AtomicInteger();
        Runnable task = () -> offset.set(resultDocument.getTextLength());
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(task);
        }
        return offset.get();
    }

    public void scrollToOffset(int offset) {
        ApplicationManager.getApplication().invokeLater(() -> {
            int targetOffset = Math.max(0, Math.min(offset, resultDocument.getTextLength()));
            resultEditor.getCaretModel().moveToOffset(targetOffset);
            resultEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        });
    }

    public boolean isSoftWrapsEnabled() {
        AtomicInteger enabled = new AtomicInteger();
        Runnable task = () -> enabled.set(resultEditor.getSettings().isUseSoftWraps() ? 1 : 0);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(task);
        }
        return enabled.get() == 1;
    }

    public void setSoftWrapsEnabled(boolean enabled) {
        ApplicationManager.getApplication().invokeLater(() -> {
            resultEditor.getSettings().setUseSoftWraps(enabled);
            resultEditor.getComponent().revalidate();
            resultEditor.getComponent().repaint();
        });
    }

    public JComponent getResultComponent() {
        return resultEditor.getComponent();
    }

    public Editor getConsoleEditor() {
        if (consoleView instanceof ConsoleViewImpl consoleViewImpl) {
            return consoleViewImpl.getEditor();
        }
        return null;
    }

    private void appendToResultEditor(String message, ConsoleViewContentType contentType) {
        int startOffset = resultDocument.getTextLength();
        resultDocument.insertString(startOffset, message + "\n");
        int endOffset = resultDocument.getTextLength();
        RangeHighlighter highlighter = resultEditor.getMarkupModel().addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                textAttributesFor(contentType),
                HighlighterTargetArea.EXACT_RANGE
        );
        resultHighlighters.add(highlighter);
        resultEditor.getCaretModel().moveToOffset(endOffset);
        resultEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    }

    private void clearResultHighlighters() {
        for (RangeHighlighter highlighter : resultHighlighters) {
            resultEditor.getMarkupModel().removeHighlighter(highlighter);
        }
        resultHighlighters.clear();
    }

    private TextAttributes textAttributesFor(ConsoleViewContentType contentType) {
        TextAttributes attributes = contentType.getAttributes();
        if (attributes != null) {
            return attributes.clone();
        }
        return new TextAttributes(colorFor(contentType), null, null, null, Font.PLAIN);
    }

    private Color colorFor(ConsoleViewContentType contentType) {
        if (contentType == ConsoleViewContentType.LOG_ERROR_OUTPUT) {
            return new Color(0xC75450);
        }
        if (contentType == ConsoleViewContentType.LOG_WARNING_OUTPUT) {
            return new Color(0xC98A2E);
        }
        if (contentType == ConsoleViewContentType.LOG_INFO_OUTPUT) {
            return new Color(0x4F8FBA);
        }
        if (contentType == ConsoleViewContentType.LOG_DEBUG_OUTPUT) {
            return new Color(0x7A7E85);
        }
        if (contentType == ConsoleViewContentType.LOG_VERBOSE_OUTPUT) {
            return new Color(0x6A737D);
        }
        return EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground();
    }

    private void runUndoTransparentWriteAction(Runnable runnable) {
        CommandProcessor.getInstance().runUndoTransparentAction(() ->
                ApplicationManager.getApplication().runWriteAction(runnable)
        );
    }
}
