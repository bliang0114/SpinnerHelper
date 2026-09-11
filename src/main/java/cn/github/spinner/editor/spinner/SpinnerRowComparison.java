package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SpinnerRowComparison implements Disposable {
    record Entry(String label, List<String> values) {}
    private final List<Entry> entries = new ArrayList<>();
    private String[] headers = new String[0];
    private FrameWrapper frame;
    private javax.swing.event.TableModelListener widthListener;
    final AbstractTableModel model = new AbstractTableModel() {
        public int getRowCount() { return Math.max(headers.length, entries.stream().mapToInt(entry -> entry.values().size()).max().orElse(0)); }
        public int getColumnCount() { return entries.size() + 1; }
        public String getColumnName(int column) { return column == 0 ? SpinnerBundle.message("spinner.compare.field") : entries.get(column - 1).label(); }
        public Object getValueAt(int row, int column) {
            if (column == 0) return row < headers.length ? headers[row] : "#" + (row + 1);
            var values = entries.get(column - 1).values();
            return row < values.size() ? values.get(row) : "";
        }
    };

    void add(String[] header, List<Entry> selected, boolean append) {
        if (!append || !Arrays.equals(headers, header)) entries.clear();
        headers = header.clone();
        for (Entry entry : selected) if (!entries.contains(entry)) entries.add(entry);
        model.fireTableStructureChanged();
    }

    boolean differs(int row) {
        Object first = entries.isEmpty() ? "" : model.getValueAt(row, 1);
        for (int column = 2; column < model.getColumnCount(); column++) if (!first.equals(model.getValueAt(row, column))) return true;
        return false;
    }

    void show(Project project) {
        if (frame != null) { frame.getFrame().setVisible(true); frame.getFrame().toFront(); return; }
        var table = new FilterTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                component.setBackground(isCellSelected(row, column) ? getSelectionBackground() : differs(convertRowIndexToModel(row))
                        ? JBColor.namedColor("Table.modifiedCellBackground", new JBColor(0xFFF2CC, 0x51452B)) : getBackground());
                return component;
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Runnable widths = () -> { for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setPreferredWidth(JBUI.scale(240)); };
        widthListener = e -> SwingUtilities.invokeLater(widths);
        model.addTableModelListener(widthListener);
        widths.run();
        JPanel panel = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(table.getFilterComponent());
        JButton clear = new JButton(SpinnerBundle.message("button.clear"));
        clear.addActionListener(e -> { entries.clear(); model.fireTableStructureChanged(); });
        toolbar.add(clear);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        frame = new FrameWrapper(project, "Spinner.RowComparison", false);
        frame.setTitle(SpinnerBundle.message("spinner.compare.rows"));
        frame.setComponent(panel);
        frame.setSize(JBUI.size(1000, 600));
        frame.setOnCloseHandler(() -> { model.removeTableModelListener(widthListener); widthListener = null; frame = null; return true; });
        frame.show();
    }

    @Override public void dispose() {
        if (widthListener != null) { model.removeTableModelListener(widthListener); widthListener = null; }
        if (frame != null) { frame.setOnCloseHandler(() -> true); com.intellij.openapi.util.Disposer.dispose(frame); frame = null; }
    }
}
