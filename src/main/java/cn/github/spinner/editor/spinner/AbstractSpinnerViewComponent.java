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
    private boolean internalDocumentUpdate;
    private boolean recordComponentLoaded;
    private int displayedRecordModelRow = -1;
    private Document document;
    private volatile boolean disposed;
    private long appliedStamp = -1;
    private final Alarm refreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final Alarm recordAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private org.jetbrains.concurrency.CancellablePromise<?> parseRequest;

    public AbstractSpinnerViewComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
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
        tableModel = new SpinnerTableModel();
        table = new FilterTable(tableModel);
        table.setPreserveColumnFiltersOnDataChange(true);
        recordPane = new JBTabbedPane();
        recordPane.add(SpinnerBundle.message("tab.record"), new JPanel());
        recordPaneVisibleAction = new RecordPaneVisibleAction();
        actionGroup = new DefaultActionGroup();
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && pendingRefresh) {
                scheduleRefreshFromDocument();
            }
        });
    }

    protected void setupListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        table.setRowSelectionInterval(rowIndex, rowIndex);
                        recordPane.setVisible(true);
                        showRecordForRow(table.convertRowIndexToModel(rowIndex), false);
                    }
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingTable) return;
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
        documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (internalDocumentUpdate) {
                    return;
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
    }

    protected AnAction[] createToolbarAction() {
        return new AnAction[]{new RecordPaneVisibleAction()};
    }

    private JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        toolbarPanel.add(table.getFilterComponent());
        AnAction[] actions = createToolbarAction();
        actionGroup.addAll(actions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner Data View.Toolbar", actionGroup, true);
        toolbar.setTargetComponent(table);
        toolbarPanel.add(toolbar.getComponent());
        return toolbarPanel;
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
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JBSplitter splitter = new JBSplitter();
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(recordPane);
        add(splitter, BorderLayout.CENTER);
    }

    public void reloadValue(int rowIndex, String line) {
        String[] values = line.split("\t", -1);
        dataList.set(rowIndex, values);
        updatingTable = true;
        try {
            ((SpinnerTableModel) tableModel).replaceRow(rowIndex, values);
        } finally {
            updatingTable = false;
        }
        appliedStamp = document.getModificationStamp();
    }

    void runInternalDocumentUpdate(@NotNull Runnable update) {
        internalDocumentUpdate = true;
        try {
            update.run();
        } finally {
            internalDocumentUpdate = false;
        }
    }

    public void refreshFromDocument() {
        if (disposed || document == null) return;
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
            parseRequest = ReadAction.nonBlocking(() -> {
                long stamp = document.getModificationStamp();
                return new ParsedDocument(stamp, SpinnerTableSnapshot.parse(document.getText()));
            }).expireWith(this).coalesceBy(this)
                    .finishOnUiThread(ModalityState.any(), parsed -> {
                        if (disposed) return;
                        refreshScheduled = false;
                        if (document.getModificationStamp() != parsed.stamp()) {
                            refreshFromDocument();
                            return;
                        }
                        applySnapshot(parsed.snapshot());
                        appliedStamp = parsed.stamp();
                        pendingRefresh = false;
                    }).submit(AppExecutorUtil.getAppExecutorService());
        }, 75);
    }

    private record ParsedDocument(long stamp, SpinnerTableSnapshot snapshot) {}

    private void scheduleRefreshFromDocument() {
        if (!disposed && isShowing() && pendingRefresh) refreshFromDocument();
    }

    private void applySnapshot(SpinnerTableSnapshot snapshot) {
        long totalStartedNanos = System.nanoTime();
        int selectedModelRow = getSelectedModelRow();
        int[] selectedRows = java.util.Arrays.stream(table.getSelectedRows()).map(table::convertRowIndexToModel).toArray();
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
                table.getColumnModel().getColumn(0).setMaxWidth(60);
                for (int i = 1; i < tableModel.getColumnCount(); i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(240);
                }
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
        disposed = true;
        refreshAlarm.cancelAllRequests();
        recordAlarm.cancelAllRequests();
        if (parseRequest != null) parseRequest.cancel();
        documentListener = null;
        pendingRefresh = false;
        refreshScheduled = false;
        internalDocumentUpdate = false;
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
        Vector<String> vector = new Vector<>(java.util.Arrays.asList(dataList.get(modelRowIndex)));
        long startedNanos = System.nanoTime();
        JComponent component = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this)
                .setProject(project).build(headers, vector);
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
        recordPane.setComponentAt(0, new JPanel());
        displayedRecordModelRow = -1;
        recordComponentLoaded = false;
    }

    private void setRecordPaneVisible(boolean visible) {
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
