package cn.github.spinner.editor.spinner;

import com.intellij.openapi.progress.ProgressManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/** Parsed off the UI thread; raw rows retain missing/trailing cells for record edits. */
record SpinnerTableSnapshot(String[] headers, List<String[]> rows, Vector<Vector> tableRows) {
    static SpinnerTableSnapshot parse(String text) {
        String[] lines = text.split("\\r\\n|\\r|\\n", -1);
        String[] headers = text.isEmpty() ? new String[0] : lines[0].split("\t", -1);
        List<String[]> rows = new ArrayList<>();
        Vector<Vector> tableRows = new Vector<>();
        int end = lines.length;
        // A final line terminator is not another record. Interior blank lines are records.
        if (end > 1 && lines[end - 1].isEmpty()) end--;
        for (int i = 1; i < end; i++) {
            ProgressManager.checkCanceled();
            String[] cells = lines[i].split("\t", -1);
            rows.add(cells);
            Vector<String> row = new Vector<>(Arrays.asList(Arrays.copyOf(cells, headers.length)));
            row.replaceAll(value -> value == null ? "" : value);
            tableRows.add(row);
        }
        return new SpinnerTableSnapshot(headers, rows, tableRows);
    }
}
