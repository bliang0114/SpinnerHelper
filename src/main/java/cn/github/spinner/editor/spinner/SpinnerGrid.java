package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Editable view of the file draft; view coordinates never become source row identities. */
final class SpinnerGrid extends FilterTable {
    private final AbstractSpinnerViewComponent view;
    private final Map<Integer, TableColumn> columns = new LinkedHashMap<>();
    private JScrollPane scrollPane;
    private SpinnerGrid frozen;
    private boolean pinned;
    private int nameViewIndex = 1;
    private String expectedRow;
    private String initialValue;
    private int sourceRow;
    private int sourceColumn;
    private boolean committing;
    private SpinnerGrid layoutOwner = this;

    SpinnerGrid(AbstractSpinnerViewComponent view, SpinnerTableModel model) {
        super(model);
        this.view = view;
        setPreserveColumnFiltersOnDataChange(true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        ExpandableTextField field = new ExpandableTextField();
        view.bindUndo(field);
        view.bindUndo(this);
        DefaultCellEditor editor = new DefaultCellEditor(field) {
            @Override
            public boolean stopCellEditing() {
                if (committing) return true;
                committing = true;
                try {
                    if (!view.editGridCell(sourceRow, sourceColumn, field.getText(), expectedRow)) {
                        field.setToolTipText(SpinnerBundle.message("spinner.grid.invalid"));
                        return false;
                    }
                    return super.stopCellEditing();
                } finally { committing = false; }
            }
        };
        editor.setClickCountToStart(2);
        setDefaultEditor(Object.class, editor);
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
        });
        bind(this, "F2", "spinner.edit", () -> startEditing(getSelectedRow(), getSelectedColumn()));
        bind(this, "ENTER", "spinner.enter", () -> advance(false, false));
        bind(this, "TAB", "spinner.tab", () -> advance(true, false));
        bind(this, "shift TAB", "spinner.backtab", () -> advance(true, true));
        bind(this, "ESCAPE", "spinner.cancel", this::cancelEditing);
        bind(field, "ENTER", "spinner.enter", () -> advance(false, false));
        bind(field, "TAB", "spinner.tab", () -> advance(true, false));
        bind(field, "shift TAB", "spinner.backtab", () -> advance(true, true));
        bind(field, "ESCAPE", "spinner.cancel", this::cancelEditing);
        field.setFocusTraversalKeysEnabled(false);
        setFocusTraversalKeysEnabled(false);
        bind(this, "ctrl shift C", "spinner.copy.rows", this::copyRows);
        bind(this, "ctrl shift V", "spinner.paste.rows", this::pasteRowsFromClipboard);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { showRowsMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { showRowsMenu(e); }
        });
        setToolTipText(SpinnerBundle.message("spinner.grid.hint"));
        getFilterComponent().setToolTipText(SpinnerBundle.message("spinner.grid.filter.hint"));
        setTransferHandler(new TransferHandler() {
            @Override public int getSourceActions(JComponent component) { return COPY; }
            @Override protected java.awt.datatransfer.Transferable createTransferable(JComponent component) {
                String text = selectedText();
                return text == null ? null : new java.awt.datatransfer.StringSelection(text);
            }
            @Override public boolean canImport(TransferSupport support) {
                return !support.isDrop() && support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);
            }
            @Override public boolean importData(TransferSupport support) {
                if (support.isDrop()) return false;
                String error;
                try { error = pasteText((String) support.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor)); }
                catch (Exception ex) { error = "spinner.paste.unsupported"; }
                if (error != null) com.intellij.openapi.ui.Messages.showWarningDialog(view.project,
                        SpinnerBundle.message(error), SpinnerBundle.message("spinner.paste.title"));
                return error == null;
            }
        });
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl V"), "paste");
        getActionMap().put("paste", TransferHandler.getPasteAction());
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl C"), "copy");
        getActionMap().put("copy", TransferHandler.getCopyAction());
    }

    String selectedRowsText() {
        int[] rows = java.util.Arrays.stream(getSelectedRows()).map(this::convertRowIndexToModel).toArray();
        if (rows.length == 0 || !view.flushDraftInputs()) return null;
        return SpinnerClipboard.format(java.util.Arrays.stream(rows)
                .mapToObj(row -> java.util.Arrays.asList(view.rawRow(row).split("\t", -1))).toList());
    }

    String pasteRowsText(String text) {
        return pasteRowsText(getSelectedModelRow(), text);
    }

    private String pasteRowsText(int row, String text) {
        if (row < 0 || row >= view.dataList.size() || !view.canEditGrid()) return "spinner.paste.target";
        java.util.List<java.util.List<String>> rows;
        try { rows = SpinnerClipboard.parseRows(text); }
        catch (IllegalArgumentException ex) { return ex.getMessage(); }
        return view.insertRowsAfter(row, view.rawRow(row), rows.stream().map(cells -> String.join("\t", cells)).toList())
                ? null : "spinner.paste.stale";
    }

    private void copyRows() {
        String text = selectedRowsText();
        if (text != null) com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(SpinnerClipboard.rowSelection(text));
    }

    private void pasteRowsFromClipboard() {
        pasteRowsFromClipboard(getSelectedModelRow());
    }

    private void pasteRowsFromClipboard(int row) {
        String error = pasteCopiedRows(row);
        if (error != null) com.intellij.openapi.ui.Messages.showWarningDialog(view.project,
                SpinnerBundle.message(error), SpinnerBundle.message("spinner.rows.paste"));
    }

    boolean canPasteRows() {
        return SpinnerClipboard.copiedRows(com.intellij.openapi.ide.CopyPasteManager.getInstance().getContents()) != null;
    }

    String pasteCopiedRows(int row) {
        String text = SpinnerClipboard.copiedRows(com.intellij.openapi.ide.CopyPasteManager.getInstance().getContents());
        return text == null ? "spinner.rows.copy.required" : pasteRowsText(row, text);
    }

    private void showRowsMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        int row = rowAtPoint(event.getPoint());
        if (row < 0) return;
        int targetRow = convertRowIndexToModel(row);
        int sourceColumn = convertColumnIndexToModel(Math.max(0, columnAtPoint(event.getPoint())));
        if (!view.flushDraftInputs() || !view.canEditGrid()) return;
        row = convertRowIndexToView(targetRow);
        if (row < 0) return;
        if (!isRowSelected(row)) changeSelection(row, convertColumnIndexToView(sourceColumn), false, false);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copy = new JMenuItem(SpinnerBundle.message("spinner.rows.copy"));
        copy.addActionListener(e -> copyRows());
        menu.add(copy);
        JMenuItem paste = new JMenuItem(SpinnerBundle.message("spinner.rows.paste"));
        paste.setEnabled(canPasteRows());
        paste.addActionListener(e -> pasteRowsFromClipboard(targetRow));
        menu.add(paste);
        if (sourceColumn > 0) {
            String[] expected = {view.rawRow(targetRow)};
            String original = String.valueOf(getModel().getValueAt(targetRow, sourceColumn));
            SpinnerUrlEditing.addAction(menu, original, value -> {
                boolean accepted = view.editGridCell(targetRow, sourceColumn,
                        SpinnerSettingsComponent.preserveSpacing(original, value.strip()), expected[0]);
                if (accepted) expected[0] = view.rawRow(targetRow);
                return accepted;
            });
        }
        int[] selected = java.util.Arrays.stream(getSelectedRows()).map(this::convertRowIndexToModel).toArray();
        JMenuItem compare = new JMenuItem(SpinnerBundle.message("spinner.compare.rows"));
        compare.setEnabled(selected.length > 1);
        compare.addActionListener(e -> view.compareRows(selected, false));
        menu.add(compare);
        JMenuItem addCompare = new JMenuItem(SpinnerBundle.message("spinner.compare.add"));
        addCompare.addActionListener(e -> view.compareRows(selected, true));
        menu.add(addCompare);
        JMenuItem deploy = new JMenuItem(SpinnerBundle.message("action.deploy.text"));
        deploy.addActionListener(e -> view.deployRows(selected));
        menu.add(deploy);
        JMenuItem deployChanges = new JMenuItem(SpinnerBundle.message("spinner.deploy.diff"));
        deployChanges.setEnabled(view.isGitFile());
        deployChanges.addActionListener(e -> SpinnerGitChanges.deployChanges(view));
        menu.add(deployChanges);
        spaceMenu(menu);
        menu.show(this, event.getX(), event.getY());
    }

    static void spaceMenu(JPopupMenu menu) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem item) {
                item.setMargin(JBUI.insets(6, 12));
                Dimension size = item.getPreferredSize();
                item.setPreferredSize(new Dimension(Math.max(JBUI.scale(240), size.width + JBUI.scale(16)),
                        Math.max(JBUI.scale(36), size.height + JBUI.scale(8))));
            }
        }
    }

    String selectedText() {
        if (getSelectedRowCount() == 0 || getSelectedColumnCount() == 0) return null;
        int[] sourceRows = java.util.Arrays.stream(getSelectedRows()).map(this::convertRowIndexToModel).toArray();
        int[] sourceColumns = java.util.Arrays.stream(getSelectedColumns()).map(this::convertColumnIndexToModel).toArray();
        if (java.util.Arrays.stream(sourceColumns).anyMatch(column -> column == 0) || !view.flushDraftInputs()) return null;
        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();
        for (int row : sourceRows) {
            java.util.List<String> cells = new java.util.ArrayList<>();
            for (int column : sourceColumns) {
                cells.add(String.valueOf(getModel().getValueAt(row, column)));
            }
            rows.add(cells);
        }
        return SpinnerClipboard.format(rows);
    }

    /** Returns a localized error key on rejection; no clipboard cells are written before validation. */
    String pasteText(String text) {
        java.util.List<java.util.List<String>> cells;
        try { cells = SpinnerClipboard.parse(text); }
        catch (IllegalArgumentException ex) { return ex.getMessage(); }
        int row = getSelectedRow(), column = getSelectedColumn();
        int height = cells.size(), width = cells.getFirst().size();
        if (row < 0 || column < 0) return "spinner.paste.target";
        java.util.List<Integer> visibleColumns = new java.util.ArrayList<>();
        for (int x = column; x < getColumnCount(); x++) visibleColumns.add(convertColumnIndexToModel(x));
        if (layoutOwner != this) {
            for (int x = 0; x < layoutOwner.getColumnCount(); x++) {
                int source = layoutOwner.convertColumnIndexToModel(x);
                if (source > 0) visibleColumns.add(source);
            }
        }
        if (height > getRowCount() - row || width > visibleColumns.size()) return "spinner.paste.bounds";
        int[] sourceRows = new int[height], sourceColumns = new int[width];
        for (int y = 0; y < height; y++) sourceRows[y] = convertRowIndexToModel(row + y);
        for (int x = 0; x < width; x++) {
            sourceColumns[x] = visibleColumns.get(x);
            if (sourceColumns[x] == 0) return "spinner.paste.target";
        }
        if (!view.flushDraftInputs() || !view.canEditGrid()) return "spinner.paste.stale";
        Map<Integer, String> expected = new LinkedHashMap<>(), replacement = new LinkedHashMap<>();
        for (int y = 0; y < height; y++) {
            String original = view.rawRow(sourceRows[y]);
            String[] raw = original.split("\t", -1);
            int length = Math.max(raw.length, java.util.Arrays.stream(sourceColumns).max().orElseThrow());
            String[] edited = java.util.Arrays.copyOf(raw, length);
            java.util.Arrays.fill(edited, raw.length, length, "");
            for (int x = 0; x < width; x++) edited[sourceColumns[x] - 1] = cells.get(y).get(x);
            expected.put(sourceRows[y], original);
            replacement.put(sourceRows[y], String.join("\t", edited));
        }
        return view.editDraftRows(expected, replacement) ? null : "spinner.paste.stale";
    }

    private static void bind(JComponent component, String key, String name, Runnable action) {
        component.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    @Override
    public boolean editCellAt(int row, int column, java.util.EventObject event) {
        if (row < 0 || column < 0 || convertColumnIndexToModel(column) == 0) return false;
        if (isEditing() && getEditingRow() == row && getEditingColumn() == column) return true;
        int targetRow = convertRowIndexToModel(row);
        int targetColumn = convertColumnIndexToModel(column);
        if (!view.flushDraftInputs()) return false;
        row = convertRowIndexToView(targetRow);
        column = convertColumnIndexToView(targetColumn);
        if (row < 0 || column < 0) return false;
        sourceRow = targetRow;
        sourceColumn = targetColumn;
        expectedRow = view.rawRow(sourceRow);
        initialValue = String.valueOf(getModel().getValueAt(sourceRow, sourceColumn));
        return super.editCellAt(row, column, event);
    }

    @Override
    public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (isEditing() && !committing && (row != getEditingRow() || column != getEditingColumn())) {
            int targetRow = convertRowIndexToModel(row);
            int targetColumn = convertColumnIndexToModel(column);
            if (!stopEditing()) return;
            row = convertRowIndexToView(targetRow);
            column = convertColumnIndexToView(targetColumn);
            if (row < 0 || column < 0) return;
        }
        super.changeSelection(row, column, toggle, extend);
    }

    // The editor already committed using its captured model coordinates. JTable's default
    // editingStopped would convert possibly resorted view coordinates a second time.
    @Override public void editingStopped(ChangeEvent event) { removeEditor(); repaint(); }
    @Override public void editingCanceled(ChangeEvent event) { super.editingCanceled(event); view.gridInputCancelled(); }

    boolean stopEditing() {
        return (!isEditing() || getCellEditor().stopCellEditing()) && (frozen == null || frozen.stopEditing());
    }

    void cancelEditing() {
        if (isEditing()) getCellEditor().cancelCellEditing();
        if (frozen != null) frozen.cancelEditing();
    }

    boolean hasPendingInput() {
        return (isEditing() && !java.util.Objects.equals(initialValue, getCellEditor().getCellEditorValue()))
                || (frozen != null && frozen.hasPendingInput());
    }

    private int getSelectedModelRow() {
        return getSelectedRow() < 0 ? -1 : convertRowIndexToModel(getSelectedRow());
    }

    private void startEditing(int row, int column) {
        if (row >= 0 && column >= 0 && editCellAt(row, column)) {
            getEditorComponent().requestFocusInWindow();
            if (getEditorComponent() instanceof JTextField field) field.selectAll();
        }
    }

    private void advance(boolean horizontal, boolean backwards) {
        int row = isEditing() ? getEditingRow() : getSelectedRow();
        int column = isEditing() ? getEditingColumn() : getSelectedColumn();
        if (row < 0 || column < 0) return;
        int modelRow = convertRowIndexToModel(row);
        if (!stopEditing()) return;
        row = convertRowIndexToView(modelRow);
        if (row < 0 || getRowCount() == 0) return;
        if (horizontal && layoutOwner != this) {
            int target = backwards ? layoutOwner.lastEditableColumn() : layoutOwner.firstEditableColumn();
            if (target >= 0) {
                int targetRow = backwards ? Math.max(0, row - 1) : row;
                layoutOwner.changeSelection(targetRow, target, false, false);
                layoutOwner.startEditing(targetRow, target);
                return;
            }
        }
        if (horizontal && pinned && (backwards ? column == firstEditableColumn() : column == lastEditableColumn())) {
            int targetRow = backwards ? row : Math.min(row + 1, getRowCount() - 1);
            int targetColumn = frozen.firstEditableColumn();
            frozen.changeSelection(targetRow, targetColumn, false, false);
            frozen.startEditing(targetRow, targetColumn);
            return;
        }
        if (horizontal) {
            int step = backwards ? -1 : 1;
            do {
                column += step;
                if (column >= getColumnCount()) { column = 0; row = Math.min(row + 1, getRowCount() - 1); }
                if (column < 0) { column = getColumnCount() - 1; row = Math.max(row - 1, 0); }
            } while (getColumnCount() > 1 && convertColumnIndexToModel(column) == 0);
        } else row = Math.min(row + 1, getRowCount() - 1);
        changeSelection(row, column, false, false);
        startEditing(row, column);
    }

    private int firstEditableColumn() {
        for (int column = 0; column < getColumnCount(); column++) if (convertColumnIndexToModel(column) > 0) return column;
        return -1;
    }

    private int lastEditableColumn() {
        for (int column = getColumnCount() - 1; column >= 0; column--) if (convertColumnIndexToModel(column) > 0) return column;
        return -1;
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        // Swing reuses a renderer across cells; never carry a draft color into the next cell.
        if (convertColumnIndexToModel(column) != 0) {
            component.setBackground(isCellSelected(row, column) ? getSelectionBackground() : getBackground());
            component.setForeground(isCellSelected(row, column) ? getSelectionForeground() : getForeground());
        }
        if (view != null && view.isDraftCell(convertRowIndexToModel(row), convertColumnIndexToModel(column))) {
            if (!isCellSelected(row, column)) component.setBackground(JBColor.namedColor("Table.modifiedCellBackground", new JBColor(0xFFF2CC, 0x51452B)));
            if (component instanceof JComponent label) label.setToolTipText(SpinnerBundle.message("spinner.grid.modified"));
        } else if (component instanceof JComponent label) label.setToolTipText(null);
        return component;
    }

    void attach(JScrollPane pane) {
        scrollPane = pane;
        configureEnclosingScrollPane();
    }

    void resetLayout() {
        if (!stopEditing()) return;
        if (frozen != null) {
            while (frozen.getColumnCount() > 0) frozen.removeColumn(frozen.getColumnModel().getColumn(0));
            scrollPane.setRowHeaderView(null);
            scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
        }
        pinned = false;
        createDefaultColumnsFromModel();
        columns.clear();
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            columns.put(column.getModelIndex(), column);
            if (column.getModelIndex() == 0) column.setMaxWidth(JBUI.scale(60));
        }
        fitColumns();
    }

    void fitColumns() {
        for (TableColumn column : columns.values()) {
            int modelColumn = column.getModelIndex();
            int width = getFontMetrics(getTableHeader().getFont()).stringWidth(getModel().getColumnName(modelColumn)) + JBUI.scale(32);
            // ponytail: sample 200 rows on EDT; use background measurement if full-file fitting is needed.
            for (int row = 0; row < Math.min(getModel().getRowCount(), 200); row++) {
                String value = String.valueOf(getModel().getValueAt(row, modelColumn));
                width = Math.max(width, getFontMetrics(getFont()).stringWidth(value.substring(0, Math.min(value.length(), 120))) + JBUI.scale(20));
            }
            column.setPreferredWidth(modelColumn == 0 ? JBUI.scale(48) : Math.max(JBUI.scale(90), Math.min(JBUI.scale(360), width)));
            column.setWidth(column.getPreferredWidth());
        }
        updateFrozenWidth();
    }

    void setColumnVisible(int modelColumn, boolean visible) {
        if (!stopEditing() || modelColumn == 0 || (pinned && modelColumn == nameColumn())) return;
        TableColumn column = columns.get(modelColumn);
        if (column == null) return;
        int position = convertColumnIndexToView(modelColumn);
        if (visible && position < 0) addColumn(column);
        else if (!visible && position >= 0) removeColumn(column);
    }

    private int nameColumn() {
        for (int column = 1; column < getModel().getColumnCount(); column++) {
            if (getModel().getColumnName(column).trim().equalsIgnoreCase("Name")) return column;
        }
        return getModel().getColumnCount() > 1 ? 1 : -1;
    }

    void setNamePinned(boolean pin) {
        if (pin == pinned || !stopEditing() || scrollPane == null || nameColumn() < 0) return;
        int name = nameColumn();
        if (pin) {
            int rowNumberIndex = convertColumnIndexToView(0);
            if (rowNumberIndex > 0) getColumnModel().moveColumn(rowNumberIndex, 0);
            nameViewIndex = Math.max(1, convertColumnIndexToView(name));
            if (frozen == null) {
                frozen = new SpinnerGrid(view, (SpinnerTableModel) getModel());
                frozen.layoutOwner = this;
                frozen.setRowSorter(getRowSorter());
                frozen.setSelectionModel(getSelectionModel());
                frozen.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
                    public void columnMarginChanged(ChangeEvent e) { updateFrozenWidth(); }
                    public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
                    public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
                    public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
                    public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
                });
            }
            while (frozen.getColumnCount() > 0) frozen.removeColumn(frozen.getColumnModel().getColumn(0));
            TableColumn rowNumber = columns.get(0);
            removeColumn(rowNumber);
            frozen.addColumn(rowNumber);
            TableColumn column = columns.get(name);
            if (convertColumnIndexToView(name) >= 0) removeColumn(column);
            frozen.addColumn(column);
            frozen.getTableHeader().setReorderingAllowed(false);
            scrollPane.setRowHeaderView(frozen);
            scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, frozen.getTableHeader());
        } else {
            TableColumn rowNumber = columns.get(0);
            frozen.removeColumn(rowNumber);
            addColumn(rowNumber);
            getColumnModel().moveColumn(getColumnCount() - 1, 0);
            TableColumn column = columns.get(name);
            frozen.removeColumn(column);
            addColumn(column);
            getColumnModel().moveColumn(getColumnCount() - 1, Math.min(nameViewIndex, getColumnCount() - 1));
            scrollPane.setRowHeaderView(null);
            scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
            scrollPane.getViewport().setViewPosition(new Point(0, scrollPane.getViewport().getViewPosition().y));
        }
        pinned = pin;
        updateFrozenWidth();
    }

    private void updateFrozenWidth() {
        if (frozen == null || !pinned || !columns.containsKey(nameColumn())) return;
        frozen.setPreferredScrollableViewportSize(new Dimension(frozen.getColumnModel().getTotalColumnWidth(), 0));
        scrollPane.revalidate();
    }

    private void showColumnMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        JPopupMenu menu = layoutOwner.columnMenu();
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    JPopupMenu columnMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem fit = new JMenuItem(SpinnerBundle.message("spinner.grid.fit"));
        fit.addActionListener(e -> fitColumns());
        menu.add(fit);
        JMenuItem reset = new JMenuItem(SpinnerBundle.message("spinner.grid.reset"));
        reset.addActionListener(e -> resetLayout());
        menu.add(reset);
        JCheckBoxMenuItem fixed = new JCheckBoxMenuItem(SpinnerBundle.message("spinner.grid.pin"), pinned);
        fixed.addActionListener(e -> setNamePinned(fixed.isSelected()));
        menu.add(fixed);
        menu.addSeparator();
        for (int modelColumn : columns.keySet()) {
            if (modelColumn == 0) continue;
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(getModel().getColumnName(modelColumn),
                    convertColumnIndexToView(modelColumn) >= 0 || pinned && modelColumn == nameColumn());
            item.setEnabled(!(pinned && modelColumn == nameColumn()));
            item.addActionListener(e -> setColumnVisible(modelColumn, item.isSelected()));
            menu.add(item);
        }
        spaceMenu(menu);
        return menu;
    }
}
