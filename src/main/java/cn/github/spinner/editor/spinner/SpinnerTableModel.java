package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.RowNumberTableModel;
import java.util.Arrays;
import java.util.Vector;

final class SpinnerTableModel extends RowNumberTableModel {
    SpinnerTableModel() { super(new String[0], 0); }

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
