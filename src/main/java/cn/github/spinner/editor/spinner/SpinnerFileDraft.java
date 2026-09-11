package cn.github.spinner.editor.spinner;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** An in-memory, document-wide draft shared by every Spinner view of the file. EDT only. */
final class SpinnerFileDraft {
    private static final Key<SpinnerFileDraft> KEY = Key.create("spinner.file.draft");
    final Document document;
    final List<AbstractSpinnerViewComponent> views = new ArrayList<>();
    private String baseline;
    private String[] lines;
    private long stamp;
    private final TreeMap<Integer, String> edits = new TreeMap<>();
    private final java.util.Set<Integer> insertedLines = new java.util.HashSet<>();
    private boolean applying;
    private long revision;
    private boolean externalChange;
    private boolean unresolvedInput;
    private final java.util.Deque<Snapshot> undo = new java.util.ArrayDeque<>();
    private boolean batchEdit;
    private record Snapshot(String[] lines, TreeMap<Integer, String> edits, java.util.Set<Integer> inserted) {}

    private void remember() {
        undo.push(new Snapshot(lines, new TreeMap<>(edits), java.util.Set.copyOf(insertedLines)));
    }

    boolean undo() {
        if (undo.isEmpty()) return false;
        Snapshot snapshot = undo.pop();
        lines = snapshot.lines();
        edits.clear(); edits.putAll(snapshot.edits());
        insertedLines.clear(); insertedLines.addAll(snapshot.inserted());
        revision++;
        return true;
    }

    static SpinnerFileDraft find(Document document) { return document.getUserData(KEY); }

    static SpinnerFileDraft acquire(Document document, AbstractSpinnerViewComponent view) {
        SpinnerFileDraft draft = document.getUserData(KEY);
        if (draft == null) {
            draft = new SpinnerFileDraft(document);
            document.putUserData(KEY, draft);
        }
        draft.views.add(view);
        return draft;
    }

    private SpinnerFileDraft(Document document) {
        this.document = document;
        reset();
    }

    void release(AbstractSpinnerViewComponent view) {
        views.remove(view);
        if (views.isEmpty()) document.putUserData(KEY, null);
    }

    boolean isDirty() { return !edits.isEmpty() || !insertedLines.isEmpty() || unresolvedInput; }
    void retainUnresolvedInput() { unresolvedInput = true; }
    boolean isConflict() { return isDirty() && (externalChange || stamp != document.getModificationStamp()); }
    boolean isBaselineCurrent() { return !externalChange && stamp == document.getModificationStamp(); }
    void externalChange() { if (isDirty()) externalChange = true; }
    boolean isApplying() { return applying; }
    int changedRows() { return insertedLines.size() + (int) edits.keySet().stream().filter(line -> !insertedLines.contains(line)).count(); }
    long revision() { return revision; }

    boolean isCellModified(int row, int column) {
        if (insertedLines.contains(row + 1)) return true;
        String edited = edits.get(row + 1);
        if (edited == null) return false;
        if (column == 0) return true;
        String[] before = lines[row + 1].split("\t", -1);
        String[] after = edited.split("\t", -1);
        int index = column - 1;
        return !java.util.Objects.equals(index < before.length ? before[index] : "", index < after.length ? after[index] : "");
    }

    boolean edit(int row, String expected, String value) {
        int line = row + 1;
        if (line >= lines.length || line < 1 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return false;
        if (!edits.getOrDefault(line, lines[line]).equals(expected)) return false;
        if (expected.equals(value)) return true;
        if (!batchEdit) remember();
        if (value.equals(lines[line])) edits.remove(line);
        else edits.put(line, value);
        revision++;
        return true;
    }

    boolean editRows(java.util.Map<Integer, String> expected, java.util.Map<Integer, String> replacement) {
        if (!expected.keySet().equals(replacement.keySet())) return false;
        for (var entry : replacement.entrySet()) {
            int line = entry.getKey() + 1;
            if (line < 1 || line >= lines.length || entry.getValue().indexOf('\n') >= 0 || entry.getValue().indexOf('\r') >= 0
                    || !edits.getOrDefault(line, lines[line]).equals(expected.get(entry.getKey()))) return false;
        }
        if (replacement.equals(expected)) return true;
        remember();
        batchEdit = true;
        try { replacement.forEach((row, value) -> edit(row, expected.get(row), value)); }
        finally { batchEdit = false; }
        return true;
    }

    boolean insertAfter(int row, String expectedText, List<String> records) {
        int position = row + 2;
        int recordEnd = lines.length - (lines[lines.length - 1].isEmpty() ? 1 : 0);
        if (!text().equals(expectedText) || row < 0 || position > recordEnd || records.isEmpty()
                || records.stream().anyMatch(value -> value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)) return false;
        int count = records.size();
        remember();
        var next = new ArrayList<>(java.util.Arrays.asList(lines));
        next.addAll(position, records);
        var shifted = new TreeMap<Integer, String>();
        edits.forEach((line, value) -> shifted.put(line >= position ? line + count : line, value));
        var inserted = new java.util.HashSet<Integer>();
        for (int line : insertedLines) inserted.add(line >= position ? line + count : line);
        for (int line = position; line < position + count; line++) inserted.add(line);
        lines = next.toArray(String[]::new);
        edits.clear(); edits.putAll(shifted);
        insertedLines.clear(); insertedLines.addAll(inserted);
        revision++;
        return true;
    }

    String text() {
        if (!isDirty()) return baseline;
        String[] result = lines.clone();
        edits.forEach((line, value) -> result[line] = value);
        return String.join("\n", result);
    }

    void documentChanged() {
        if (applying) return;
        // Keep the old baseline and row identities whenever pending edits exist.
        if (!isDirty()) reset();
    }

    void reset() {
        baseline = document.getText();
        lines = baseline.split("\n", -1);
        stamp = document.getModificationStamp();
        edits.clear();
        insertedLines.clear();
        undo.clear();
        externalChange = false;
        unresolvedInput = false;
        revision++;
    }

    boolean apply(Project project) {
        if (!isDirty()) return true;
        if (isConflict() || !document.isWritable()) return false;
        String replacement = text();
        boolean[] success = {false};
        applying = true;
        try {
            WriteCommandAction.writeCommandAction(project).withName("Apply Spinner draft").run(() -> {
                if (stamp != document.getModificationStamp() || !document.isWritable()) return;
                // One replacement, one command: no partially applied set of rows.
                document.replaceString(0, document.getTextLength(), replacement);
                success[0] = true;
            });
        } finally {
            applying = false;
        }
        if (success[0]) reset();
        return success[0];
    }
}
