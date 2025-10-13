package com.bol.spinner.editor.ui.dataview;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AttributesTableComponent extends JPanel {
    private JBTable table;
    private DefaultTableModel tableModel;

    public AttributesTableComponent() {
        initTable();
        setupLayout();
    }

    private void initTable() {
        tableModel = new DefaultTableModel(new Object[]{"Name", "Owner", "Type", "Default", "Range"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
        table = new JBTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        JPanel tablePanel = ToolbarDecorator.createDecorator(table)
                .setAddAction(btn -> {})
                .setRemoveAction(btn -> {})
                .disableUpDownActions()
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .createPanel();
        add(tablePanel, BorderLayout.CENTER);
    }
}
