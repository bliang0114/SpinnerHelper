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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsolePrinter {
    private final Project project;
    private final ConsoleView consoleView;
    private final Document resultDocument;
    private final List<StyledRange> styledRanges = new ArrayList<>();
    private final List<EditorEx> resultEditors = new CopyOnWriteArrayList<>();
    private volatile boolean softWrapsEnabled;

    public ConsolePrinter(Project project, ConsoleView consoleView) {
        this.project = project;
        this.consoleView = consoleView;
        this.resultDocument = EditorFactory.getInstance().createDocument("");
        this.softWrapsEnabled = false;
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
            for (EditorEx resultEditor : resultEditors) {
                resultEditor.getCaretModel().moveToOffset(targetOffset);
                resultEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }
        });
    }

    public boolean isSoftWrapsEnabled() {
        return softWrapsEnabled;
    }

    public void setSoftWrapsEnabled(boolean enabled) {
        softWrapsEnabled = enabled;
        ApplicationManager.getApplication().invokeLater(() -> {
            for (EditorEx resultEditor : resultEditors) {
                resultEditor.getSettings().setUseSoftWraps(enabled);
                resultEditor.getComponent().revalidate();
                resultEditor.getComponent().repaint();
            }
        });
    }

    public @NotNull JComponent createResultComponent() {
        AtomicInteger ready = new AtomicInteger();
        final EditorEx[] editorRef = new EditorEx[1];
        Runnable task = () -> {
            editorRef[0] = createResultEditor();
            ready.set(1);
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(task);
        }
        if (ready.get() == 1 && editorRef[0] != null) {
            return editorRef[0].getComponent();
        }
        throw new IllegalStateException("Failed to create result component");
    }

    public void releaseResultComponent(@Nullable JComponent component) {
        if (component == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            EditorEx targetEditor = null;
            for (EditorEx resultEditor : resultEditors) {
                if (resultEditor.getComponent() == component) {
                    targetEditor = resultEditor;
                    break;
                }
            }
            if (targetEditor != null) {
                resultEditors.remove(targetEditor);
                EditorFactory.getInstance().releaseEditor(targetEditor);
            }
        });
    }

    public Editor getConsoleEditor() {
        if (consoleView instanceof ConsoleViewImpl consoleViewImpl) {
            return consoleViewImpl.getEditor();
        }
        return null;
    }

    private EditorEx createResultEditor() {
        EditorEx resultEditor = (EditorEx) EditorFactory.getInstance().createViewer(resultDocument, project);
        resultEditor.setHorizontalScrollbarVisible(true);
        resultEditor.setVerticalScrollbarVisible(true);
        resultEditor.setCaretEnabled(true);
        resultEditor.setEmbeddedIntoDialogWrapper(false);
        resultEditor.setBorder(JBUI.Borders.empty());
        resultEditor.getSettings().setLineMarkerAreaShown(false);
        resultEditor.getSettings().setFoldingOutlineShown(false);
        resultEditor.getSettings().setRightMarginShown(false);
        resultEditor.getSettings().setAdditionalColumnsCount(1);
        resultEditor.getSettings().setAdditionalLinesCount(1);
        resultEditor.getSettings().setUseSoftWraps(softWrapsEnabled);
        resultEditor.setBackgroundColor(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
        for (StyledRange styledRange : styledRanges) {
            addHighlighter(resultEditor, styledRange);
        }
        resultEditors.add(resultEditor);
        return resultEditor;
    }

    private void appendToResultEditor(String message, ConsoleViewContentType contentType) {
        int startOffset = resultDocument.getTextLength();
        resultDocument.insertString(startOffset, message + "\n");
        int endOffset = resultDocument.getTextLength();
        StyledRange styledRange = new StyledRange(startOffset, endOffset, textAttributesFor(contentType));
        styledRanges.add(styledRange);
        for (EditorEx resultEditor : resultEditors) {
            addHighlighter(resultEditor, styledRange);
            resultEditor.getCaretModel().moveToOffset(endOffset);
            resultEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        }
    }

    private void clearResultHighlighters() {
        styledRanges.clear();
        for (EditorEx resultEditor : resultEditors) {
            RangeHighlighter[] highlighters = resultEditor.getMarkupModel().getAllHighlighters();
            for (RangeHighlighter highlighter : highlighters) {
                resultEditor.getMarkupModel().removeHighlighter(highlighter);
            }
        }
    }

    private void addHighlighter(@NotNull EditorEx resultEditor, @NotNull StyledRange styledRange) {
        resultEditor.getMarkupModel().addRangeHighlighter(
                styledRange.startOffset(),
                styledRange.endOffset(),
                HighlighterLayer.ADDITIONAL_SYNTAX,
                styledRange.attributes().clone(),
                HighlighterTargetArea.EXACT_RANGE
        );
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

    private record StyledRange(int startOffset, int endOffset, @NotNull TextAttributes attributes) {
    }
}
