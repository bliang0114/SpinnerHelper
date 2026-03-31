package cn.github.spinner.util;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MQLExecutionGutterManager {
    private static final Key<List<RangeHighlighter>> GUTTER_HIGHLIGHTERS_KEY = Key.create("spinner.mql.execution.gutter");

    private MQLExecutionGutterManager() {
    }

    public static void clear(@NotNull Project project, @Nullable VirtualFile file) {
        if (file == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            for (Editor editor : getTextEditors(project, file)) {
                List<RangeHighlighter> highlighters = editor.getUserData(GUTTER_HIGHLIGHTERS_KEY);
                if (highlighters == null || highlighters.isEmpty()) {
                    continue;
                }
                MarkupModel markupModel = editor.getMarkupModel();
                for (RangeHighlighter highlighter : highlighters) {
                    markupModel.removeHighlighter(highlighter);
                }
                editor.putUserData(GUTTER_HIGHLIGHTERS_KEY, new ArrayList<>());
                editor.getComponent().repaint();
            }
        });
    }

    public static void markResult(@NotNull Project project,
                                  @Nullable VirtualFile file,
                                  int lineNumber,
                                  boolean success,
                                  @NotNull String message) {
        if (file == null || lineNumber < 0) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            for (Editor editor : getTextEditors(project, file)) {
                if (lineNumber >= editor.getDocument().getLineCount()) {
                    continue;
                }
                MarkupModel markupModel = editor.getMarkupModel();
                RangeHighlighter highlighter = markupModel.addLineHighlighter(lineNumber, HighlighterLayer.SELECTION - 1, null);
                highlighter.setGutterIconRenderer(new ExecutionResultGutterIconRenderer(success, message));
                List<RangeHighlighter> highlighters = editor.getUserData(GUTTER_HIGHLIGHTERS_KEY);
                if (highlighters == null) {
                    highlighters = new ArrayList<>();
                    editor.putUserData(GUTTER_HIGHLIGHTERS_KEY, highlighters);
                }
                highlighters.add(highlighter);
                editor.getComponent().repaint();
            }
        });
    }

    private static List<Editor> getTextEditors(@NotNull Project project, @NotNull VirtualFile file) {
        List<Editor> editors = new ArrayList<>();
        FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof TextEditor textEditor) {
                editors.add(textEditor.getEditor());
            }
        }
        return editors;
    }

    private static final class ExecutionResultGutterIconRenderer extends GutterIconRenderer {
        private final boolean success;
        private final String message;

        private ExecutionResultGutterIconRenderer(boolean success, @NotNull String message) {
            this.success = success;
            this.message = message;
        }

        @Override
        public @NotNull Icon getIcon() {
            return success ? AllIcons.General.InspectionsOK : AllIcons.General.Error;
        }

        @Override
        public @Nullable @Nls String getTooltipText() {
            return success ? null : message;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ExecutionResultGutterIconRenderer other)) {
                return false;
            }
            return success == other.success && Objects.equals(message, other.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(success, message);
        }
    }
}
