package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.RowNumberTableModel;
import java.util.Arrays;
import java.util.Vector;

final class SpinnerTableModel extends RowNumberTableModel {
    private final AbstractSpinnerViewComponent view;
    SpinnerTableModel(AbstractSpinnerViewComponent view) {
        super(new String[0], 0);
        this.view = view;
    }

    @Override
    public boolean isCellEditable(int row, int column) { return column > 0 && view.canEditGrid(); }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column > 0) view.editGridCell(row, column, String.valueOf(value), view.rawRow(row));
    }

    void replace(SpinnerTableSnapshot snapshot) {
        Vector<String> columns = new Vector<>(Arrays.asList(snapshot.headers()));
        boolean structureChanged = !columnIdentifiers.equals(columns);
        dataVector = snapshot.tableRows();
        columnIdentifiers = columns;
        if (structureChanged) fireTableStructureChanged();
        else fireTableDataChanged();
    }

    void replaceRow(int rowIndex, String[] cells) {
        Vector<String> row = new Vector<>(Arrays.asList(Arrays.copyOf(cells, columnIdentifiers.size())));
        row.replaceAll(value -> value == null ? "" : value);
        dataVector.set(rowIndex, row);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }
}
