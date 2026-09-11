package cn.github.spinner.editor.spinner;

import java.util.ArrayList;
import java.util.List;

/** Excel-style tabular text. Embedded record delimiters cannot be represented in Spinner fields. */
final class SpinnerClipboard {
    private record Rows(String text) implements java.io.Serializable {}
    private static final java.awt.datatransfer.DataFlavor ROWS_FLAVOR =
            new java.awt.datatransfer.DataFlavor(Rows.class, "SpinnerHelper rows");

    static java.awt.datatransfer.Transferable rowSelection(String text) {
        return new java.awt.datatransfer.StringSelection(text) {
            @Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                return new java.awt.datatransfer.DataFlavor[]{ROWS_FLAVOR, java.awt.datatransfer.DataFlavor.stringFlavor};
            }
            @Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
                return ROWS_FLAVOR.equals(flavor) || java.awt.datatransfer.DataFlavor.stringFlavor.equals(flavor);
            }
            @Override public Object getTransferData(java.awt.datatransfer.DataFlavor flavor)
                    throws java.awt.datatransfer.UnsupportedFlavorException, java.io.IOException {
                return ROWS_FLAVOR.equals(flavor) ? new Rows(text) : super.getTransferData(flavor);
            }
        };
    }

    static String copiedRows(java.awt.datatransfer.Transferable contents) {
        try {
            if (contents == null || !contents.isDataFlavorSupported(ROWS_FLAVOR)) return null;
            Object value = contents.getTransferData(ROWS_FLAVOR);
            return value instanceof Rows rows && rows.text().equals(contents.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor))
                    ? rows.text() : null;
        } catch (java.awt.datatransfer.UnsupportedFlavorException | java.io.IOException | IllegalStateException ex) {
            return null;
        }
    }

    static List<List<String>> parse(String text) {
        List<List<String>> rows = parseRows(text);
        int width = rows.getFirst().size();
        for (List<String> cells : rows) if (cells.size() != width) throw new IllegalArgumentException("spinner.paste.ragged");
        return rows;
    }

    static List<List<String>> parseRows(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false, closed = false, atStart = true, endedRow = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            endedRow = false;
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else cell.append(c);
                continue;
            }
            if (atStart && c == '"') { quoted = true; atStart = false; continue; }
            if (c == '\t' || c == '\r' || c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                closed = false;
                atStart = true;
                if (c != '\t') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    rows.add(row);
                    row = new ArrayList<>();
                    endedRow = true;
                }
            } else {
                if (closed) throw new IllegalArgumentException("spinner.paste.quotes");
                cell.append(c);
                atStart = false;
            }
        }
        if (quoted) throw new IllegalArgumentException("spinner.paste.quotes");
        if (!endedRow) { row.add(cell.toString()); rows.add(row); }
        for (List<String> cells : rows) {
            for (String value : cells) if (value.indexOf('\t') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
                throw new IllegalArgumentException("spinner.paste.embedded");
        }
        return rows;
    }

    static String format(List<List<String>> rows) {
        String text = String.join("\r\n", rows.stream().map(row -> String.join("\t", row.stream().map(value ->
                value.indexOf('"') >= 0 || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                        ? "\"" + value.replace("\"", "\"\"") + "\"" : value).toList())).toList());
        if (!rows.isEmpty() && rows.getLast().size() == 1 && rows.getLast().getFirst().isEmpty()) text += "\r\n";
        return text;
    }
}
