package cn.github.spinner.components;

import javax.swing.table.DefaultTableModel;

public class RowNumberTableModel extends DefaultTableModel {

    public RowNumberTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    @Override
    public int getColumnCount() {
        return super.getColumnCount() + 1;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "";
        }
        return super.getColumnName(column - 1);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return row + 1;
        }
        return super.getValueAt(row, column - 1);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
