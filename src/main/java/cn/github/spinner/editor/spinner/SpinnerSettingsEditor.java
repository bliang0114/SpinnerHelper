package cn.github.spinner.editor.spinner;

import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.components.FilterTable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** A single Settings editor component is reparented between the dock and its frame. */
final class SpinnerSettingsEditor extends JPanel implements Disposable {
    private final AbstractSpinnerViewComponent view;
    final JPanel dock = new JPanel(new BorderLayout());
    final FilterTable table;
    private final JLabel identity = new JLabel();
    private final AbstractTableModel model;
    private SpinnerSettingsPairs pairs = new SpinnerSettingsPairs("", "");
    private String expectedLine;
    private int recordRow = -1, nameColumn = -1;
    private int editingPair, editingColumn;
    private String initialValue;
    private boolean committing, disposed;
    private FrameWrapper frame;
    private boolean updatingNameChoices;
    final JButton windowButton = new JButton(SpinnerBundle.message("spinner.settings.window"));
    private JTextArea longInput;
    private String longInitial, longSourceLine;
    private int longPair, longColumn, longRecord;
    private com.intellij.openapi.ui.DialogWrapper longDialog;

    SpinnerSettingsEditor(AbstractSpinnerViewComponent view) {
        super(new BorderLayout(0, JBUI.scale(4)));
        this.view = view;
        view.bindUndo(this);
        model = new AbstractTableModel() {
            public int getRowCount() { return pairs.size(); }
            public int getColumnCount() { return 2; }
            public String getColumnName(int column) { return column == 0 ? "Name" : "Value"; }
            public Object getValueAt(int row, int column) { return pairs.get(row, column).strip(); }
            public boolean isCellEditable(int row, int column) { return recordRow >= 0 && view.canEditGrid(); }
            public void setValueAt(Object value, int row, int column) { setPairValue(row, column, String.valueOf(value)); }
        };
        table = new FilterTable(model) {
            @Override public void editingStopped(ChangeEvent e) { removeEditor(); }
            @Override public void editingCanceled(ChangeEvent e) { super.editingCanceled(e); view.gridInputCancelled(); }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { popup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { popup(e); }
            private void popup(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint()), column = table.columnAtPoint(e.getPoint());
                if (row < 0 || column < 0) return;
                int pair = table.convertRowIndexToModel(row), cell = table.convertColumnIndexToModel(column), record = recordRow;
                if (!stopEditing() || recordRow != record || pair >= pairs.size()) return;
                String[] expected = {expectedLine};
                JPopupMenu menu = new JPopupMenu();
                SpinnerUrlEditing.addAction(menu, pairs.get(pair, cell), value -> {
                    if (recordRow != record || !java.util.Objects.equals(expectedLine, expected[0])) return false;
                    boolean accepted = setPairValue(pair, cell, value);
                    if (accepted) expected[0] = expectedLine;
                    return accepted;
                });
                if (menu.getComponentCount() > 0) {
                    SpinnerGrid.spaceMenu(menu);
                    menu.show(table, e.getX(), e.getY());
                }
            }
        });
        table.setPreserveColumnFiltersOnDataChange(true);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        ExpandableTextField input = new ExpandableTextField();
        view.bindUndo(input);
        DefaultCellEditor editor = new DefaultCellEditor(input) {
            @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
                editingPair = table.convertRowIndexToModel(row);
                editingColumn = table.convertColumnIndexToModel(column);
                initialValue = String.valueOf(value);
                return super.getTableCellEditorComponent(table, value, selected, row, column);
            }
            @Override public boolean stopCellEditing() {
                if (committing) return true;
                if (!setPairValue(editingPair, editingColumn, input.getText())) {
                    input.setToolTipText(SpinnerBundle.message("spinner.settings.invalid"));
                    return false;
                }
                return super.stopCellEditing();
            }
        };
        editor.setClickCountToStart(2);
        table.setDefaultEditor(Object.class, editor);
        List<String> nameChoices = settingNames().stream().map(String::strip).distinct().toList();
        var names = new com.intellij.openapi.ui.ComboBox<String>(nameChoices.toArray(String[]::new));
        names.setEditable(true);
        DefaultCellEditor nameEditor = new DefaultCellEditor(names) {
            @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
                editingPair = table.convertRowIndexToModel(row);
                editingColumn = table.convertColumnIndexToModel(column);
                initialValue = String.valueOf(value);
                updatingNameChoices = true;
                names.setModel(new DefaultComboBoxModel<>(nameChoices.toArray(String[]::new)));
                try { return super.getTableCellEditorComponent(table, value, selected, row, column); }
                finally { updatingNameChoices = false; }
            }
            @Override public Object getCellEditorValue() { return names.getEditor().getItem(); }
            @Override public boolean stopCellEditing() {
                if (updatingNameChoices || committing) return true;
                if (!setPairValue(editingPair, editingColumn, String.valueOf(getCellEditorValue()))) return false;
                // Avoid JComboBox's synthetic action recursively committing the same edit.
                fireEditingStopped();
                return true;
            }
        };
        nameEditor.setClickCountToStart(2);
        table.getColumnModel().getColumn(0).setCellEditor(nameEditor);
        ((JTextField) names.getEditor().getEditorComponent()).getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (updatingNameChoices) return;
                view.pendingInputChanged();
                SwingUtilities.invokeLater(() -> {
                    if (!table.isEditing() || table.getEditorComponent() != names) return;
                    var field = (JTextField) names.getEditor().getEditorComponent();
                    String text = field.getText();
                    int caret = field.getCaretPosition();
                    updatingNameChoices = true;
                    try {
                        names.setModel(new DefaultComboBoxModel<>(nameChoices.stream()
                                .filter(item -> item.toLowerCase(java.util.Locale.ROOT).contains(text.strip().toLowerCase(java.util.Locale.ROOT)))
                                .toArray(String[]::new)));
                        names.setSelectedItem(text);
                        field.setCaretPosition(Math.min(caret, field.getText().length()));
                        if (names.isShowing() && names.getItemCount() > 0) names.showPopup();
                    } finally { updatingNameChoices = false; }
                });
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        });
        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
        button(buttons, "spinner.settings.add", () -> {
            if (!view.flushDraftInputs() || recordRow < 0) return;
            var next = new SpinnerSettingsPairs(pairs);
            next.add("", "");
            if (applyPairs(next)) {
                table.clearColumnFilters();
                table.getFilterComponent().reset();
                table.getFilterComponent().filter();
                int row = table.convertRowIndexToView(pairs.size() - 1);
                table.changeSelection(row, 0, false, false);
                if (table.editCellAt(row, 0)) table.getEditorComponent().requestFocusInWindow();
            }
        });
        button(buttons, "spinner.settings.remove", () -> removePairs(java.util.Arrays.stream(table.getSelectedRows())
                .map(table::convertRowIndexToModel).toArray()));
        button(buttons, "spinner.settings.copy", () -> {
            String text = selectedPairsText();
            if (text != null) com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(new java.awt.datatransfer.StringSelection(text));
        });
        button(buttons, "spinner.settings.merge", this::mergeFromClipboard);
        windowButton.addActionListener(e -> toggleWindow());
        buttons.add(windowButton);
        JPanel heading = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        heading.add(identity, BorderLayout.NORTH);
        heading.add(AbstractSpinnerViewComponent.createToolbarRow(table.getFilterComponent(), buttons, this), BorderLayout.CENTER);
        add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new com.intellij.ui.components.JBScrollPane(table);
        add(scroll, BorderLayout.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(JBUI.scale(220));
        table.getColumnModel().getColumn(1).setPreferredWidth(JBUI.scale(600));
        dock.add(this, BorderLayout.CENTER);
        dock.setMinimumSize(JBUI.size(0, 150));
        updateIdentity();
        table.setTransferHandler(new TransferHandler() {
            public int getSourceActions(JComponent component) { return COPY; }
            protected java.awt.datatransfer.Transferable createTransferable(JComponent component) {
                String text = selectedPairsText();
                return text == null ? null : new java.awt.datatransfer.StringSelection(text);
            }
            @Override public boolean canImport(TransferSupport support) { return !support.isDrop(); }
            @Override public boolean importData(TransferSupport support) {
                if (support.isDrop()) return false;
                String error;
                try { error = mergeText((String) support.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor)); }
                catch (Exception ex) { error = SpinnerBundle.message("spinner.paste.unsupported"); }
                if (error != null) showMergeError(error);
                return error == null;
            }
        });
        table.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl V"), "paste");
        table.getActionMap().put("paste", TransferHandler.getPasteAction());
        table.setToolTipText(SpinnerBundle.message("spinner.settings.merge.hint"));
    }

    private List<String> settingNames() {
        try { return SpinnerSettingNameConfig.getSettingNames(SpinnerType.fromFile(view.virtualFile)); }
        catch (IllegalArgumentException ex) { return List.of(); }
    }

    private static void button(JPanel panel, String key, Runnable action) {
        JButton button = new JButton(SpinnerBundle.message(key));
        button.setToolTipText(SpinnerBundle.message(key.equals("spinner.settings.merge") ? "spinner.settings.merge.hint" : key));
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    int recordRow() { return recordRow; }
    String recordIdentity() { return identity.getText(); }
    boolean hasPendingInput() {
        return table.isEditing() && !java.util.Objects.equals(initialValue, table.getCellEditor().getCellEditorValue())
                || longInput != null && !longInput.getText().equals(longInitial);
    }
    boolean stopEditing() {
        if (committing) return true;
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) return false;
        if (longInput != null && !longInput.getText().equals(longInitial)) {
            if (recordRow != longRecord || !java.util.Objects.equals(expectedLine, longSourceLine)
                    || !setPairValue(longPair, longColumn, longInput.getText())) return false;
            longInitial = longInput.getText();
            longSourceLine = expectedLine;
        }
        return true;
    }
    void cancelEditing() {
        longInput = null;
        if (longDialog != null) longDialog.close(com.intellij.openapi.ui.DialogWrapper.CANCEL_EXIT_CODE);
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
    }

    boolean selectRecord(int row) {
        if (!stopEditing()) return false;
        if (committing) return true;
        int previousColumn = nameColumn;
        recordRow = row >= 0 && row < view.dataList.size() ? row : -1;
        nameColumn = -1;
        for (int column = 0; column + 1 < view.headers.length; column++) {
            if (view.headers[column].contains("Setting Name") && view.headers[column + 1].contains("Setting Value")) { nameColumn = column; break; }
        }
        String line = recordRow < 0 ? null : view.rawRow(recordRow);
        if (recordRow >= 0 && java.util.Objects.equals(line, expectedLine) && nameColumn >= 0 && nameColumn == previousColumn) { updateIdentity(); return true; }
        expectedLine = line;
        if (line == null || nameColumn < 0) pairs = new SpinnerSettingsPairs("", "");
        else {
            String[] cells = line.split("\t", -1);
            pairs = new SpinnerSettingsPairs(nameColumn < cells.length ? cells[nameColumn] : "", nameColumn + 1 < cells.length ? cells[nameColumn + 1] : "");
        }
        model.fireTableDataChanged();
        updateIdentity();
        return true;
    }

    void refreshRecord() { if (!committing && !table.isEditing() && longInput == null) selectRecord(recordRow); else updateIdentity(); }
    void resetRecord(int row) {
        if (longInput != null) return;
        cancelEditing(); expectedLine = null; selectRecord(row);
    }

    boolean setPairValue(int row, int column, String value) {
        if (committing) return true;
        try {
            var next = new SpinnerSettingsPairs(pairs);
            next.set(row, column, value.strip());
            return applyPairs(next);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) { return false; }
    }

    boolean addPair(String name, String value) {
        if (!view.flushDraftInputs()) return false;
        var next = new SpinnerSettingsPairs(pairs);
        next.add(name, value);
        return applyPairs(next);
    }

    boolean removePairs(int[] rows) {
        if (!view.flushDraftInputs()) return false;
        var next = new SpinnerSettingsPairs(pairs);
        next.remove(rows);
        return applyPairs(next);
    }

    /** Returns a localized rejection reason without flushing or modifying pending input. */
    String mergeText(String text) {
        if (disposed || recordRow < 0 || nameColumn < 0 || !view.canEditGrid()) return SpinnerBundle.message("spinner.settings.merge.target");
        SpinnerSettingsPairs next;
        try { next = pairs.merged(SpinnerClipboard.parse(text)); }
        catch (SpinnerSettingsPairs.MergeConflict ex) { return SpinnerBundle.message(ex.key, ex.arguments); }
        catch (IllegalArgumentException ex) { return SpinnerBundle.message(ex.getMessage()); }
        if (view.hasUnfinishedInput() || longInput != null) return SpinnerBundle.message("spinner.settings.merge.pending");
        return applyPairs(next) ? null : SpinnerBundle.message("spinner.paste.stale");
    }

    private void mergeFromClipboard() {
        String error;
        try {
            Object content = com.intellij.openapi.ide.CopyPasteManager.getInstance().getContents(java.awt.datatransfer.DataFlavor.stringFlavor);
            error = content instanceof String text ? mergeText(text) : SpinnerBundle.message("spinner.paste.unsupported");
        } catch (Exception ex) { error = SpinnerBundle.message("spinner.paste.unsupported"); }
        if (error != null) showMergeError(error);
    }

    private void showMergeError(String error) {
        com.intellij.openapi.ui.Messages.showWarningDialog(view.project, error, SpinnerBundle.message("spinner.settings.merge"));
    }

    private boolean applyPairs(SpinnerSettingsPairs next) {
        if (disposed || recordRow < 0 || nameColumn < 0) return false;
        committing = true;
        try {
            if (!view.editSettingsRow(recordRow, nameColumn, expectedLine, next.names(), next.values())) return false;
            pairs = next;
            expectedLine = view.rawRow(recordRow);
            model.fireTableDataChanged();
            updateIdentity();
            return true;
        } finally { committing = false; }
    }

    String selectedPairsText() {
        int[] selected = java.util.Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).toArray();
        if (!stopEditing() || selected.length == 0) return null;
        List<List<String>> rows = new ArrayList<>();
        for (int row : selected) rows.add(List.of(pairs.get(row, 0), pairs.get(row, 1)));
        return SpinnerClipboard.format(rows);
    }

    private void updateIdentity() {
        String text = recordRow < 0 ? SpinnerBundle.message("spinner.settings.select") : nameColumn < 0
                ? SpinnerBundle.message("spinner.settings.absent") : SpinnerBundle.message("spinner.settings.identity",
                view.virtualFile.getName(), recordRow + 2, expectedLine == null ? "" : expectedLine.split("\t", 2)[0]);
        if (view.hasConflict()) text += " — " + SpinnerBundle.message("spinner.draft.conflict");
        identity.setText(text);
        identity.setToolTipText(text);
        if (frame != null) frame.setTitle(text);
    }

    boolean moveTo(Container target) {
        if (!stopEditing()) return false;
        Container old = getParent();
        if (old != null) { old.remove(this); old.revalidate(); old.repaint(); }
        target.add(this, BorderLayout.CENTER);
        windowButton.setVisible(target == dock);
        dock.setVisible(target == dock);
        target.revalidate();
        target.repaint();
        return true;
    }

    void toggleWindow() {
        if (!stopEditing()) return;
        if (frame != null) { frame.close(); return; }
        frame = new FrameWrapper(view.project, "Spinner.Settings.Editor", false);
        JPanel host = new JPanel(new BorderLayout());
        moveTo(host);
        frame.setComponent(host);
        frame.setTitle(identity.getText());
        frame.setSize(JBUI.size(1000, 600));
        frame.setOnCloseHandler(() -> {
            if (!moveTo(dock)) return false;
            frame = null;
            dock.setVisible(true);
            return true;
        });
        frame.show();
    }

    void toggleVisible() {
        if (!stopEditing()) return;
        if (frame != null) { frame.getFrame().setVisible(!frame.getFrame().isVisible()); return; }
        dock.setVisible(!dock.isVisible());
        dock.revalidate();
    }

    JTextArea beginLongEdit(int row, int column) {
        if (!stopEditing()) return null;
        longRecord = recordRow;
        longSourceLine = expectedLine;
        longPair = row;
        longColumn = column;
        longInitial = pairs.get(row, column).strip();
        longInput = new JTextArea(longInitial);
        longInput.setLineWrap(true);
        longInput.setWrapStyleWord(true);
        longInput.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { view.pendingInputChanged(); }
        });
        return longInput;
    }

    private void editLongValue() {
        if (table.getSelectedRow() < 0) return;
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        int column = table.convertColumnIndexToModel(Math.max(0, table.getSelectedColumn()));
        JTextArea text = beginLongEdit(row, column);
        if (text == null) return;
        try { longDialog = new com.intellij.openapi.ui.DialogWrapper(view.project) {
            { setTitle(identity.getText() + " — " + model.getColumnName(column)); init(); }
            @Override protected JComponent createCenterPanel() {
                JScrollPane pane = new com.intellij.ui.components.JBScrollPane(text);
                pane.setPreferredSize(JBUI.size(800, 400));
                return pane;
            }
            @Override protected void doOKAction() {
                if (stopEditing()) super.doOKAction();
                else setErrorText(SpinnerBundle.message("spinner.settings.invalid"));
            }
        }; longDialog.show(); } finally { longDialog = null; longInput = null; refreshRecord(); }
    }

    @Override public void dispose() {
        disposed = true;
        longInput = null;
        cancelEditing();
        if (frame != null) {
            frame.setOnCloseHandler(() -> true);
            com.intellij.openapi.util.Disposer.dispose(frame);
            frame = null;
        }
    }
}
