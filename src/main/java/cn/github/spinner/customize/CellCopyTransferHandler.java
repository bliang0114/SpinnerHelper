package cn.github.spinner.customize;

import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class CellCopyTransferHandler extends TransferHandler {
    private final JBTable table;

    public CellCopyTransferHandler(JBTable table) {
        this.table = table;
    }

    @Override
    protected @Nullable Transferable createTransferable(JComponent c) {
        int[] selectedRows = table.getSelectedRows();
        int[] selectedColumns = table.getSelectedColumns();

        if (selectedRows.length == 0 || selectedColumns.length == 0) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
            if (rowIndex > 0) {
                content.append('\n');
            }
            for (int columnIndex = 0; columnIndex < selectedColumns.length; columnIndex++) {
                if (columnIndex > 0) {
                    content.append('\t');
                }
                Object value = table.getValueAt(selectedRows[rowIndex], selectedColumns[columnIndex]);
                if (value != null) {
                    content.append(value);
                }
            }
        }
        return new StringSelection(content.toString());
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return false;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }
}
