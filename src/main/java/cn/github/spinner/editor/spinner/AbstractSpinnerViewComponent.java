package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Slf4j
public abstract class AbstractSpinnerViewComponent extends JPanel implements Disposable {
    private static final long SLOW_PHASE_MS = 100L;
    private static final long SLOW_REFRESH_MS = 200L;
    protected final Project project;
    protected final VirtualFile virtualFile;
    protected FilterTable table;
    protected DefaultTableModel tableModel;
    protected DefaultActionGroup actionGroup;
    protected RecordPaneVisibleAction recordPaneVisibleAction;
    protected JBTabbedPane recordPane;
    protected String[] headers = new String[0];
    protected final List<String[]> dataList = new ArrayList<>();
    private DocumentListener documentListener;
    private boolean pendingRefresh;
    private boolean refreshScheduled;
    private boolean updatingTable;
    private SpinnerFileDraft draft;
    private SpinnerDataRecordBuilder activeRecord;
    SpinnerSettingsEditor settingsEditor;
    private final JLabel draftStatus = new JLabel();
    private boolean collectingInput;
    private Runnable draftStateChanged = () -> {};
    private boolean recordComponentLoaded;
    private int displayedRecordModelRow = -1;
    private int[] selectionAfterInsert;
    private JButton fileDiffButton;
    private JButton deployDiffButton;
    private SpinnerRowComparison rowComparison;
    private Document document;
    private volatile boolean disposed;
    private long appliedStamp = -1;
    private final Alarm refreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final Alarm recordAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private org.jetbrains.concurrency.CancellablePromise<?> parseRequest;

    public AbstractSpinnerViewComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        bindUndo(this);
        long totalStartedNanos = System.nanoTime();
        try {
            long phaseStartedNanos = System.nanoTime();
            if (!"xls".equals(virtualFile.getExtension())) throw new IllegalArgumentException("Invalid Spinner file");
            logPhase("readFile", phaseStartedNanos, "rows=" + dataList.size() + ", columns=" + headers.length);
            phaseStartedNanos = System.nanoTime();
            initComponents();
            logPhase("initComponents", phaseStartedNanos, "columns=" + tableModel.getColumnCount());
            phaseStartedNanos = System.nanoTime();
            setupListener();
            logPhase("setupListener", phaseStartedNanos, "documentListener=" + (documentListener != null));
            phaseStartedNanos = System.nanoTime();
            setupLayout();
            logPhase("setupLayout", phaseStartedNanos, "columns=" + tableModel.getColumnCount());
            phaseStartedNanos = System.nanoTime();
            refreshFromDocument();
            logPhase("populateTable", phaseStartedNanos, "rows=" + tableModel.getRowCount());
            log.info("[SpinnerEditorPerf] component initialization completed: file={}, elapsedMs={}, rows={}, columns={}, thread={}, edt={}",
                    virtualFile.getPath(), elapsedMillis(totalStartedNanos), tableModel.getRowCount(), tableModel.getColumnCount(),
                    Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
        } catch (Exception e) {
            log.warn("[SpinnerEditorPerf] component initialization failed: file=" + virtualFile.getPath() +
                    ", elapsedMs=" + elapsedMillis(totalStartedNanos) + ", thread=" + Thread.currentThread().getName(), e);
            table = new FilterTable();
            table.getEmptyText().setText(e.getMessage());
            add(table);
        }
    }

    protected void initComponents() {
        tableModel = new SpinnerTableModel(this);
        table = new SpinnerGrid(this, (SpinnerTableModel) tableModel);
        table.setPreserveColumnFiltersOnDataChange(true);
        recordPane = new JBTabbedPane();
        recordPane.add(SpinnerBundle.message("tab.record"), new JPanel());
        settingsEditor = new SpinnerSettingsEditor(this);
        com.intellij.openapi.util.Disposer.register(this, settingsEditor);
        recordPaneVisibleAction = new RecordPaneVisibleAction();
        actionGroup = new DefaultActionGroup();
        project.getMessageBus().connect(this).subscribe(com.intellij.openapi.vcs.ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED,
                new com.intellij.openapi.vcs.VcsMappingListener() {
                    @Override public void directoryMappingChanged() {
                        ApplicationManager.getApplication().invokeLater(() -> { if (!disposed) updateGitActions(); });
                    }
                });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && pendingRefresh) {
                scheduleRefreshFromDocument();
            }
        });
    }

    protected void setupListener() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingTable) return;
            int target = getSelectedModelRow();
            int previous = settingsEditor.recordRow();
            if (!settingsEditor.selectRecord(target)) {
                int oldViewRow = previous < 0 ? -1 : table.convertRowIndexToView(previous);
                if (oldViewRow >= 0) {
                    updatingTable = true;
                    try { table.setRowSelectionInterval(oldViewRow, oldViewRow); }
                    finally { updatingTable = false; }
                }
                return;
            }
            if (!recordPane.isVisible()) return;

            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                clearRecordPane();
                return;
            }

            int modelRowIndex = table.convertRowIndexToModel(selectedRow);
            recordAlarm.cancelAllRequests();
            recordAlarm.addRequest(() -> {
                if (!disposed && getSelectedModelRow() == modelRowIndex) showRecordForRow(modelRowIndex, false);
            }, 75);
        });
        document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            log.warn("Document not found for file: {}", virtualFile.getPath());
            return;
        }
        draft = SpinnerFileDraft.acquire(document, this);
        documentListener = new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                if (!draft.isApplying() && !flushDraftInputs()) draft.retainUnresolvedInput();
            }

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (draft.isApplying()) return;
                draft.documentChanged();
                updateDraftStatus();
                if (draft.isDirty()) return;
                if (activeRecord != null) {
                    activeRecord.deactivate();
                    activeRecord = null;
                    recordPane.setComponentAt(0, new JLabel(SpinnerBundle.message("spinner.draft.loading")));
                    recordComponentLoaded = false;
                }
                pendingRefresh = true;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (disposed) return;
                    if (!isShowing()) {
                        return;
                    }
                    scheduleRefreshFromDocument();
                });
            }
        };
        document.addDocumentListener(documentListener, this);
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
                com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES,
                new com.intellij.openapi.vfs.newvfs.BulkFileListener() {
                    @Override
                    public void before(@NotNull List<? extends com.intellij.openapi.vfs.newvfs.events.VFileEvent> events) {
                        for (var event : events) {
                            if (virtualFile.equals(event.getFile()) && !event.isFromSave()
                                    && (event instanceof com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
                                    || event instanceof com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent)) {
                                if (!flushDraftInputs()) draft.retainUnresolvedInput();
                                draft.externalChange();
                                updateDraftStatus();
                            }
                        }
                    }
                });
    }

    protected AnAction[] createToolbarAction() {
        return new AnAction[0];
    }

    private JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        AnAction[] actions = createToolbarAction();
        actionGroup.addAll(actions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner Data View.Toolbar", actionGroup, true);
        toolbar.setTargetComponent(table);
        toolbarPanel.add(toolbar.getComponent());
        JButton columns = new JButton(SpinnerBundle.message("spinner.grid.columns"));
        columns.addActionListener(e -> ((SpinnerGrid) table).columnMenu().show(columns, 0, columns.getHeight()));
        toolbarPanel.add(columns);
        JButton settings = new JButton(SpinnerBundle.message("spinner.settings.toggle"));
        settings.addActionListener(e -> settingsEditor.toggleVisible());
        toolbarPanel.add(settings);
        JButton fileDiff = new JButton(SpinnerBundle.message("spinner.file.diff"));
        fileDiffButton = fileDiff;
        fileDiff.setToolTipText(SpinnerBundle.message("spinner.file.diff.hint"));
        fileDiff.addActionListener(e -> showFileDiff());
        toolbarPanel.add(fileDiff);
        deployDiffButton = new JButton(SpinnerBundle.message("spinner.deploy.diff"));
        deployDiffButton.setToolTipText(SpinnerBundle.message("spinner.deploy.diff.hint"));
        deployDiffButton.addActionListener(e -> SpinnerGitChanges.deployChanges(this));
        toolbarPanel.add(deployDiffButton);
        updateGitActions();
        JButton apply = new JButton(SpinnerBundle.message("spinner.draft.apply"));
        apply.addActionListener(e -> { if (!applyDraft()) showApplyFailure(); });
        JButton discard = new JButton(SpinnerBundle.message("spinner.draft.discard"));
        discard.addActionListener(e -> discardDraft());
        JButton diff = new JButton(SpinnerBundle.message("spinner.draft.diff"));
        diff.setToolTipText(SpinnerBundle.message("spinner.draft.diff.hint"));
        diff.addActionListener(e -> showDraftDiff());
        toolbarPanel.add(apply);
        toolbarPanel.add(discard);
        toolbarPanel.add(diff);
        JPanel controls = new JPanel(new BorderLayout(0, 4));
        controls.add(createToolbarRow(table.getFilterComponent(), toolbarPanel, this), BorderLayout.CENTER);
        controls.add(draftStatus, BorderLayout.SOUTH);
        return controls;
    }

    static JComponent createToolbarRow(JComponent search, JPanel buttons, JComponent host) {
        search.setPreferredSize(JBUI.size(240, 30));
        JPanel tableControls = new JPanel(new BorderLayout(JBUI.scale(4), 0));
        tableControls.add(search, BorderLayout.WEST);
        tableControls.add(buttons, BorderLayout.CENTER);
        JScrollPane toolbarScroll = new com.intellij.ui.components.JBScrollPane(tableControls,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                if (tableControls.getPreferredSize().width > host.getWidth()) {
                    size.height += getHorizontalScrollBar().getPreferredSize().height;
                }
                return size;
            }
        };
        toolbarScroll.setBorder(JBUI.Borders.empty());
        return toolbarScroll;
    }

    protected void setupLayout() {
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (int i = 1; i < tableModel.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(240);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        recordPane.setPreferredSize(JBUI.size(300, -1));
        recordPane.setVisible(false);
        setLayout(new BorderLayout());
        JComponent toolbarPanel = getToolbarComponent();
        add(toolbarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        ((SpinnerGrid) table).attach(scrollPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JBSplitter settingsSplit = new JBSplitter(true, 0.65f);
        settingsSplit.setHonorComponentsMinimumSize(true);
        settingsSplit.setFirstComponent(scrollPane);
        settingsSplit.setSecondComponent(settingsEditor.dock);
        add(settingsSplit, BorderLayout.CENTER);
    }

    public void reloadValue(int rowIndex, String line) {
        if (rowIndex >= dataList.size()) return; // A split view may still be loading its first snapshot.
        String[] values = line.split("\t", -1);
        dataList.set(rowIndex, values);
        updatingTable = true;
        try {
            ((SpinnerTableModel) tableModel).replaceRow(rowIndex, values);
        } finally {
            updatingTable = false;
        }
    }

    String rawRow(int row) { return String.join("\t", dataList.get(row)); }

    boolean canEditGrid() { return draft != null && appliedStamp != -1 && (draft.isDirty() || isDocumentCurrent()); }

    boolean editGridCell(int row, int column, String value, String expected) {
        if (!canEditGrid() || disposed || row < 0 || row >= dataList.size() || column < 1 || column > headers.length
                || value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return false;
        String current = rawRow(row);
        String[] cells = dataList.get(row);
        int index = column - 1;
        String oldValue = index < cells.length ? cells[index] : "";
        if (oldValue.equals(value)) return true;
        if (!current.equals(expected)) return false;
        String[] edited = java.util.Arrays.copyOf(cells, Math.max(cells.length, column));
        for (int i = cells.length; i < edited.length; i++) edited[i] = "";
        edited[index] = value;
        if (!editDraft(row, current, String.join("\t", edited))) return false;
        if (activeRecord != null && displayedRecordModelRow == row) {
            activeRecord.deactivate();
            activeRecord = null;
            showRecordForRow(row, true);
        }
        return true;
    }

    boolean isDraftCell(int row, int column) { return draft != null && draft.isCellModified(row, column); }

    boolean editSettingsRow(int row, int nameColumn, String expected, String names, String values) {
        if (!canEditGrid() || row < 0 || row >= dataList.size() || !rawRow(row).equals(expected)) return false;
        String[] raw = dataList.get(row);
        String[] cells = java.util.Arrays.copyOf(raw, Math.max(raw.length, nameColumn + 2));
        java.util.Arrays.fill(cells, raw.length, cells.length, "");
        cells[nameColumn] = names;
        cells[nameColumn + 1] = values;
        return editDraftRows(java.util.Map.of(row, expected), java.util.Map.of(row, String.join("\t", cells)));
    }
    void gridInputCancelled() { updateDraftStatus(); table.repaint(); }

    void openRecordDetails(int row) {
        if (row < 0 || !flushDraftInputs()) return;
        recordPane.setVisible(true);
        showRecordForRow(row, false);
    }

    boolean editDraft(int row, String expected, String value) {
        if (disposed || draft == null || !draft.edit(row, expected, value)) return false;
        for (AbstractSpinnerViewComponent view : List.copyOf(draft.views)) {
            view.reloadValue(row, value);
            if (view != this && view.displayedRecordModelRow == row && view.activeRecord != null && !view.activeRecord.hasChanges()) {
                view.activeRecord.deactivate();
                view.activeRecord = null;
                view.showRecordForRow(row, true);
            }
            view.updateDraftStatus();
        }
        if (!draft.isDirty() && !draft.isBaselineCurrent()) {
            draft.reset();
            reloadDraftViews();
        }
        return true;
    }

    boolean editDraftRows(java.util.Map<Integer, String> expected, java.util.Map<Integer, String> replacement) {
        if (!canEditGrid() || !draft.editRows(expected, replacement)) return false;
        for (var view : List.copyOf(draft.views)) {
            for (var entry : replacement.entrySet()) view.reloadValue(entry.getKey(), entry.getValue());
            if (view.activeRecord != null && replacement.containsKey(view.displayedRecordModelRow)) {
                view.activeRecord.deactivate();
                view.activeRecord = null;
                view.showRecordForRow(view.displayedRecordModelRow, true);
            }
            view.updateDraftStatus();
        }
        if (!draft.isDirty() && !draft.isBaselineCurrent()) {
            draft.reset();
            reloadDraftViews();
        }
        return true;
    }

    private boolean flushInput() {
        if (collectingInput) return true;
        collectingInput = true;
        try {
            if (table instanceof SpinnerGrid grid && !grid.stopEditing()) return false;
            if (settingsEditor != null && !settingsEditor.stopEditing()) return false;
            if (activeRecord == null) return true;
            boolean accepted = activeRecord.apply() != null;
            if (!accepted) draftStatus.setText(SpinnerBundle.message("spinner.draft.invalid.input"));
            return accepted;
        } finally {
            collectingInput = false;
        }
    }

    boolean flushDraftInputs() {
        if (draft == null) return true;
        boolean accepted = true;
        for (AbstractSpinnerViewComponent view : List.copyOf(draft.views)) accepted &= view.flushInput();
        return accepted;
    }

    boolean hasDraft() {
        return draft != null && (draft.isDirty() || hasUnfinishedInput());
    }

    boolean hasUnfinishedInput() {
        return draft != null && draft.views.stream().anyMatch(
                view -> (view.activeRecord != null && view.activeRecord.hasChanges())
                        || (view.table instanceof SpinnerGrid grid && grid.hasPendingInput())
                        || (view.settingsEditor != null && view.settingsEditor.hasPendingInput()));
    }

    boolean hasConflict() { return draft != null && draft.isConflict(); }

    String draftText() { flushDraftInputs(); return draft.text(); }

    public boolean applyDraft() {
        try {
            if (!flushDraftInputs()) return false;
            if (draft == null || !virtualFile.isValid() || !virtualFile.isWritable() || !draft.apply(project)) return false;
            reloadDraftViews();
            return true;
        } catch (RuntimeException ex) {
            log.warn("Spinner draft apply failed; draft retained", ex);
            return false;
        }
    }

    void discardDraft() {
        for (var view : List.copyOf(draft.views)) {
            if (view.table instanceof SpinnerGrid grid) grid.cancelEditing();
            view.settingsEditor.cancelEditing();
        }
        draft.reset();
        reloadDraftViews();
    }

    boolean insertRowsAfter(int row, String expectedRow, List<String> records) {
        if (!flushDraftInputs() || !canEditGrid() || row < 0 || row >= dataList.size()
                || !rawRow(row).equals(expectedRow)) return false;
        if (!draft.insertAfter(row, draft.text(), records)) return false;
        for (var view : List.copyOf(draft.views)) {
            view.selectionAfterInsert = java.util.Arrays.stream(view.table.getSelectedRows())
                    .map(view.table::convertRowIndexToModel).map(selected -> selected > row ? selected + records.size() : selected).toArray();
        }
        reloadDraftViews();
        return true;
    }

    private void reloadDraftViews() {
        for (AbstractSpinnerViewComponent view : List.copyOf(draft.views)) {
            if (view.activeRecord != null) view.activeRecord.deactivate();
            view.activeRecord = null;
            view.settingsEditor.resetRecord(-1);
            view.appliedStamp = -1;
            view.updateDraftStatus();
            view.refreshFromDocument();
        }
    }

    private void updateDraftStatus() {
        if (draft == null) return;
        if (settingsEditor != null) settingsEditor.refreshRecord();
        draftStatus.setText(draft.isConflict() ? SpinnerBundle.message("spinner.draft.conflict")
                : draft.isDirty() ? SpinnerBundle.message("spinner.draft.rows", draft.changedRows()) : SpinnerBundle.message("spinner.draft.clean"));
        draftStateChanged.run();
    }

    void onDraftStateChanged(Runnable listener) { draftStateChanged = listener; }

    void pendingInputChanged() {
        draftStatus.setText(SpinnerBundle.message(hasConflict() ? "spinner.draft.conflict" : "spinner.draft.pending.input"));
        draftStateChanged.run();
    }

    private void showApplyFailure() {
        com.intellij.openapi.ui.Messages.showWarningDialog(project,
                SpinnerBundle.message("spinner.draft.apply.failed"), SpinnerBundle.message("spinner.draft.title"));
    }

    boolean isGitFile() { return SpinnerGitChanges.isGitFile(project, virtualFile); }

    void deployRows(int[] rows) {
        if (!flushDraftInputs() || rows.length == 0 || !canEditGrid()) return;
        List<String> payload = new ArrayList<>();
        payload.add(String.join("\t", headers));
        for (int row : rows) payload.add(rawRow(row));
        cn.github.spinner.action.editor.SpinnerDeployAction.deploySpinnerContent(project, virtualFile.getPath(), String.join("\n", payload));
    }

    void compareRows(int[] rows, boolean append) {
        if (!flushDraftInputs() || !canEditGrid() || rows.length == 0) return;
        if (rowComparison == null) {
            rowComparison = new SpinnerRowComparison();
            com.intellij.openapi.util.Disposer.register(this, rowComparison);
        }
        List<SpinnerRowComparison.Entry> entries = new ArrayList<>();
        for (int row : rows) {
            var cells = List.of(rawRow(row).split("\t", -1));
            entries.add(new SpinnerRowComparison.Entry(virtualFile.getName() + ":" + (row + 2) + " — " + cells.getFirst(), cells));
        }
        rowComparison.add(headers, entries, append);
        rowComparison.show(project);
    }

    private void updateGitActions() {
        boolean enabled = isGitFile();
        if (fileDiffButton != null) fileDiffButton.setEnabled(enabled);
        if (deployDiffButton != null) deployDiffButton.setEnabled(enabled);
    }

    void bindUndo(JComponent component) {
        new com.intellij.openapi.project.DumbAwareAction() {
            @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.EDT; }
            @Override public void actionPerformed(@NotNull AnActionEvent e) { undoDraft(); }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("ctrl Z")), component, this);
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ctrl Z"), "spinner.undo");
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl Z"), "spinner.undo");
        component.getActionMap().put("spinner.undo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undoDraft(); }
        });
    }

    void undoDraft() {
        if (draft == null) return;
        if (!flushDraftInputs()) {
            ((SpinnerGrid) table).cancelEditing();
            settingsEditor.cancelEditing();
            gridInputCancelled();
            return;
        }
        if (draft.undo()) { reloadDraftViews(); return; }
        if (draft.isDirty()) return;
        var editor = com.intellij.openapi.editor.EditorFactory.getInstance().createEditor(document, project);
        try {
            var textEditor = com.intellij.openapi.fileEditor.impl.text.TextEditorProvider.getInstance().getTextEditor(editor);
            var manager = com.intellij.openapi.command.undo.UndoManager.getInstance(project);
            if (manager.isUndoAvailable(textEditor)) { manager.undo(textEditor); reloadDraftViews(); }
        } finally { com.intellij.openapi.editor.EditorFactory.getInstance().releaseEditor(editor); }
    }

    void showFileDiff() {
        if (!isGitFile()) return;
        // Use the IDE's VCS baseline and diff settings, including whitespace differences.
        AnAction action = ActionManager.getInstance().getAction("Compare.SameVersion");
        if (action == null) {
            com.intellij.openapi.ui.Messages.showInfoMessage(project,
                    SpinnerBundle.message("spinner.file.diff.unavailable"), SpinnerBundle.message("spinner.file.diff"));
            return;
        }
        var context = com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.VIRTUAL_FILE, virtualFile)
                .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, new VirtualFile[]{virtualFile})
                .build();
        com.intellij.openapi.actionSystem.ex.ActionUtil.invokeAction(action, context, "Spinner File Diff", null, null);
    }

    void showDraftDiff() {
        if (!flushDraftInputs()) { showApplyFailure(); return; }
        var factory = com.intellij.diff.DiffContentFactory.getInstance();
        com.intellij.diff.DiffManager.getInstance().showDiff(project,
                new com.intellij.diff.requests.SimpleDiffRequest(virtualFile.getName(),
                        factory.create(project, document.getText()), factory.create(project, draft.text()),
                        SpinnerBundle.message("spinner.draft.current.document"), SpinnerBundle.message("spinner.draft.unapplied")));
    }

    boolean prepareToLeave(java.util.function.IntSupplier choose) {
        try {
            flushDraftInputs();
            if (!hasDraft()) return true;
            int choice = choose.getAsInt();
            if (choice == 0) return applyDraft();
            if (choice == 1) { discardDraft(); return true; }
            return false;
        } catch (RuntimeException ex) {
            log.warn("Cannot leave Spinner with pending input", ex);
            return false;
        }
    }

    boolean prepareToLeave() {
        return prepareToLeave(() -> com.intellij.openapi.ui.Messages.showDialog(project,
                SpinnerBundle.message(hasConflict() ? "spinner.draft.leave.conflict" : "spinner.draft.leave.pending", virtualFile.getName()),
                SpinnerBundle.message("spinner.draft.leave.title"), new String[]{SpinnerBundle.message("spinner.draft.apply"),
                SpinnerBundle.message("spinner.draft.discard"), SpinnerBundle.message("spinner.draft.cancel")}, 2,
                com.intellij.openapi.ui.Messages.getQuestionIcon()));
    }

    String appliedRecord(int row) {
        if (!isDocumentCurrent() || hasConflict()) return null;
        int line = row + 1;
        if (line >= document.getLineCount()) return null;
        return document.getText(new com.intellij.openapi.util.TextRange(0, document.getLineEndOffset(0))) + "\n"
                + document.getText(new com.intellij.openapi.util.TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)));
    }

    public void refreshFromDocument() {
        if (disposed || document == null) return;
        flushInput();
        if (draft.isDirty() && appliedStamp != -1) { pendingRefresh = false; updateDraftStatus(); return; }
        if (document.getModificationStamp() == appliedStamp) {
            pendingRefresh = false;
            return;
        }
        pendingRefresh = true;
        if (refreshScheduled) return;
        refreshScheduled = true;
        refreshAlarm.addRequest(() -> {
            if (disposed) return;
            if (parseRequest != null) parseRequest.cancel();
            String draftSource = draft.isDirty() ? draft.text() : null;
            long revision = draft.revision();
            parseRequest = ReadAction.nonBlocking(() -> {
                long stamp = document.getModificationStamp();
                return new ParsedDocument(stamp, SpinnerTableSnapshot.parse(draftSource == null ? document.getText() : draftSource));
            }).expireWith(this).coalesceBy(this)
                    .finishOnUiThread(ModalityState.any(), parsed -> {
                        if (disposed) return;
                        refreshScheduled = false;
                        flushInput();
                        if (document.getModificationStamp() != parsed.stamp() || revision != draft.revision()) {
                            refreshFromDocument();
                            return;
                        }
                        applySnapshot(parsed.snapshot());
                        appliedStamp = parsed.stamp();
                        pendingRefresh = false;
                        updateDraftStatus();
                    }).submit(AppExecutorUtil.getAppExecutorService());
        }, 75);
    }

    private record ParsedDocument(long stamp, SpinnerTableSnapshot snapshot) {}

    private void scheduleRefreshFromDocument() {
        if (!disposed && isShowing() && pendingRefresh) refreshFromDocument();
    }

    private void applySnapshot(SpinnerTableSnapshot snapshot) {
        long totalStartedNanos = System.nanoTime();
        int[] selectedRows = selectionAfterInsert != null ? selectionAfterInsert
                : java.util.Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).toArray();
        selectionAfterInsert = null;
        int selectedModelRow = selectedRows.length == 0 ? -1 : selectedRows[0];
        int[] selectedColumns = java.util.Arrays.stream(table.getSelectedColumns()).map(table::convertColumnIndexToModel).toArray();
        boolean recordVisible = recordPane.isVisible();
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, table);
        Point position = viewport == null ? null : viewport.getViewPosition();
        boolean structureChanged = !java.util.Arrays.equals(headers, snapshot.headers());
        updatingTable = true;
        try {
            if (structureChanged) {
                table.clearColumnFilters();
                // The old filter may refer to columns removed by the new header.
                ((DefaultRowSorter<?, ?>) table.getRowSorter()).setRowFilter(null);
            }
            dataList.clear();
            dataList.addAll(snapshot.rows());
            headers = snapshot.headers();
            ((SpinnerTableModel) tableModel).replace(snapshot);
            if (structureChanged) {
                table.getFilterComponent().filter();
                ((SpinnerGrid) table).resetLayout();
            }
            table.clearSelection();
            for (int row : selectedRows) {
                if (row < tableModel.getRowCount()) {
                    int viewRow = table.convertRowIndexToView(row);
                    if (viewRow >= 0) table.addRowSelectionInterval(viewRow, viewRow);
                }
            }
            for (int column : selectedColumns) {
                if (column < tableModel.getColumnCount()) {
                    int viewColumn = table.convertColumnIndexToView(column);
                    if (viewColumn >= 0) table.addColumnSelectionInterval(viewColumn, viewColumn);
                }
            }
            refreshRecordPane(selectedModelRow, recordVisible);
            settingsEditor.resetRecord(getSelectedModelRow());
            if (viewport != null && position != null) {
                viewport.setViewPosition(new Point(Math.min(position.x, Math.max(0, table.getPreferredSize().width - viewport.getWidth())),
                        Math.min(position.y, Math.max(0, table.getPreferredSize().height - viewport.getHeight()))));
            }
        } finally {
            updatingTable = false;
        }
        long totalMs = elapsedMillis(totalStartedNanos);
        if (totalMs >= SLOW_REFRESH_MS) {
            log.warn("[SpinnerEditorPerf] table snapshot apply: file={}, elapsedMs={}, rows={}, columns={}",
                    virtualFile.getPath(), totalMs, tableModel.getRowCount(), tableModel.getColumnCount());
        }
    }

    public boolean hasPendingRefresh() {
        return pendingRefresh;
    }

    boolean isDocumentCurrent() {
        return !disposed && document != null && document.getModificationStamp() == appliedStamp;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (pendingRefresh && isShowing()) {
            scheduleRefreshFromDocument();
        }
        super.paintComponent(g);
    }

    @Override
    public void dispose() {
        if (table instanceof SpinnerGrid grid) grid.cancelEditing();
        disposed = true;
        refreshAlarm.cancelAllRequests();
        recordAlarm.cancelAllRequests();
        if (parseRequest != null) parseRequest.cancel();
        documentListener = null;
        pendingRefresh = false;
        refreshScheduled = false;
        if (draft != null) draft.release(this);
        if (activeRecord != null) activeRecord.deactivate();
        activeRecord = null;
        draftStateChanged = () -> {};
        recordComponentLoaded = false;
        displayedRecordModelRow = -1;
        if (recordPane != null) {
            recordPane.removeAll();
        }
        removeAll();
        dataList.clear();
    }

    private int getSelectedModelRow() {
        int selectedRow = table.getSelectedRow();
        return selectedRow >= 0 ? table.convertRowIndexToModel(selectedRow) : -1;
    }

    private void refreshRecordPane(int selectedModelRow, boolean recordVisible) {
        if (!recordVisible) {
            return;
        }
        if (selectedModelRow < 0 || selectedModelRow >= tableModel.getRowCount()) {
            clearRecordPane();
            return;
        }
        int viewRow = table.convertRowIndexToView(selectedModelRow);
        if (viewRow < 0) {
            clearRecordPane();
            return;
        }
        showRecordForRow(selectedModelRow, true);
    }

    private void showRecordForRow(int modelRowIndex, boolean force) {
        if (!recordPane.isVisible()) {
            return;
        }
        if (modelRowIndex < 0 || modelRowIndex >= tableModel.getRowCount()) {
            clearRecordPane();
            return;
        }
        if (!force && recordComponentLoaded && displayedRecordModelRow == modelRowIndex) {
            return;
        }
        flushInput();
        if (activeRecord != null) activeRecord.deactivate();
        Vector<String> vector = new Vector<>(java.util.Arrays.asList(dataList.get(modelRowIndex)));
        long startedNanos = System.nanoTime();
        activeRecord = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this).setProject(project);
        JComponent component = activeRecord.build(headers, vector);
        recordPane.setComponentAt(0, component);
        displayedRecordModelRow = modelRowIndex;
        recordComponentLoaded = true;
        long elapsedMs = elapsedMillis(startedNanos);
        if (elapsedMs >= SLOW_PHASE_MS) {
            log.warn("[SpinnerEditorPerf] record pane build slow: file={}, row={}, elapsedMs={}, columns={}, thread={}, edt={}",
                    virtualFile.getPath(), modelRowIndex, elapsedMs, headers.length,
                    Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
        }
    }

    private void clearRecordPane() {
        flushInput();
        if (activeRecord != null) activeRecord.deactivate();
        activeRecord = null;
        recordPane.setComponentAt(0, new JPanel());
        displayedRecordModelRow = -1;
        recordComponentLoaded = false;
    }

    private void setRecordPaneVisible(boolean visible) {
        flushInput();
        recordPane.setVisible(visible);
        if (!visible) {
            return;
        }
        int selectedModelRow = getSelectedModelRow();
        if (selectedModelRow >= 0) {
            showRecordForRow(selectedModelRow, false);
        }
    }

    private void logPhase(@NotNull String phase, long startedNanos, @NotNull String details) {
        long elapsedMs = elapsedMillis(startedNanos);
        if (elapsedMs >= SLOW_PHASE_MS) {
            log.warn("[SpinnerEditorPerf] initialization phase slow: file={}, phase={}, elapsedMs={}, {}, thread={}, edt={}",
                    virtualFile.getPath(), phase, elapsedMs, details,
                    Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    public class RecordPaneVisibleAction extends ToggleAction {
        public RecordPaneVisibleAction() {
            super(SpinnerBundle.message("action.record.view.toggle.text"), SpinnerBundle.message("action.record.view.toggle.description"), AllIcons.Nodes.Record);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            setRecordPaneVisible(!recordPane.isVisible());
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return recordPane.isVisible();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean b) {
            setRecordPaneVisible(b);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }
}
