package com.bol.spinner.customize;

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

        // 如果只选中了一个单元格，复制该单元格的值
        if (selectedRows.length == 1 && selectedColumns.length == 1) {
            Object value = table.getValueAt(selectedRows[0], selectedColumns[0]);
            String stringValue = value != null ? value.toString() : "";
            return new StringSelection(stringValue);
        }

        // 多选时使用默认行为（复制整行）
        return super.createTransferable(c);
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
