package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
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
    protected String[] headers;
    protected final List<String[]> dataList = new ArrayList<>();
    private DocumentListener documentListener;
    private boolean pendingRefresh;
    private boolean refreshScheduled;
    private boolean updatingTable;
    private boolean internalDocumentUpdate;
    private boolean recordComponentLoaded;
    private int displayedRecordModelRow = -1;
    private String pendingDocumentText;

    public AbstractSpinnerViewComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        long totalStartedNanos = System.nanoTime();
        try {
            long phaseStartedNanos = System.nanoTime();
            readFile();
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
            setValue();
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
        tableModel = new RowNumberTableModel(headers, 0);
        table = new FilterTable(tableModel);
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
            showRecordForRow(modelRowIndex, false);
        });
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
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
                long eventStartedNanos = System.nanoTime();
                SwingUtilities.invokeLater(() -> {
                    long queueDelayMs = elapsedMillis(eventStartedNanos);
                    long captureStartedNanos = System.nanoTime();
                    pendingDocumentText = event.getDocument().getText();
                    long captureMs = elapsedMillis(captureStartedNanos);
                    if (queueDelayMs >= SLOW_PHASE_MS || captureMs >= SLOW_PHASE_MS) {
                        log.warn("[SpinnerEditorPerf] documentChanged handoff slow: file={}, queueDelayMs={}, captureTextMs={}, documentChars={}, showing={}, thread={}, edt={}",
                                virtualFile.getPath(), queueDelayMs, captureMs, event.getDocument().getTextLength(), isShowing(),
                                Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
                    }
                    if (!isShowing()) {
                        pendingRefresh = true;
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

    protected void setValue() {
        for (String[] row : dataList) {
            tableModel.addRow(row);
        }
    }

    public void reloadValue(int rowIndex, String line) {
        int columnCount = tableModel.getColumnCount();
        String[] values = line.split("\t");
        dataList.set(rowIndex, values);
        pendingDocumentText = null;
        for (int i = 0; i < columnCount; i++) {
            String value = i >= values.length ? "" : values[i];
            tableModel.setValueAt(value, rowIndex, i);
        }
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
        long startedNanos = System.nanoTime();
        String text = pendingDocumentText;
        boolean usedPendingText = text != null;
        if (text == null) {
            text = ReadAction.compute(() -> {
                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                return document == null ? null : document.getText();
            });
        }
        if (text != null) {
            refreshFromText(text);
        }
        long elapsedMs = elapsedMillis(startedNanos);
        if (elapsedMs >= SLOW_REFRESH_MS) {
            log.warn("[SpinnerEditorPerf] refreshFromDocument slow: file={}, elapsedMs={}, source={}, textChars={}, rows={}, thread={}, edt={}",
                    virtualFile.getPath(), elapsedMs, usedPendingText ? "pending-event" : "document", text == null ? 0 : text.length(),
                    tableModel.getRowCount(), Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
        }
    }

    private void scheduleRefreshFromDocument() {
        if (refreshScheduled) {
            pendingRefresh = true;
            return;
        }
        refreshScheduled = true;
        pendingRefresh = true;
        SwingUtilities.invokeLater(() -> {
            refreshScheduled = false;
            if (!isDisplayable() || !isShowing() || !pendingRefresh) {
                return;
            }
            refreshFromDocument();
        });
    }

    private void refreshFromText(@NotNull String text) {
        long totalStartedNanos = System.nanoTime();
        int selectedModelRow = getSelectedModelRow();
        boolean recordVisible = recordPane.isVisible();
        long parseMs;
        long populateMs;
        long restoreMs;
        updatingTable = true;
        try {
            tableModel.setRowCount(0);
            dataList.clear();
            long parseStartedNanos = System.nanoTime();
            List<String> lines = parseLines(text);
            if (!lines.isEmpty()) {
                lines.remove(0);
                dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
            }
            parseMs = elapsedMillis(parseStartedNanos);
            long populateStartedNanos = System.nanoTime();
            setValue();
            populateMs = elapsedMillis(populateStartedNanos);
            long restoreStartedNanos = System.nanoTime();
            restoreSelection(selectedModelRow);
            refreshRecordPane(selectedModelRow, recordVisible);
            restoreMs = elapsedMillis(restoreStartedNanos);
        } finally {
            updatingTable = false;
        }
        pendingRefresh = false;
        long totalMs = elapsedMillis(totalStartedNanos);
        if (totalMs >= SLOW_REFRESH_MS) {
            log.warn("[SpinnerEditorPerf] table refresh slow: file={}, elapsedMs={}, parseMs={}, populateMs={}, restoreAndRecordMs={}, textChars={}, rows={}, columns={}, recordVisible={}, thread={}, edt={}",
                    virtualFile.getPath(), totalMs, parseMs, populateMs, restoreMs, text.length(), tableModel.getRowCount(),
                    tableModel.getColumnCount(), recordVisible, Thread.currentThread().getName(), SwingUtilities.isEventDispatchThread());
        }
    }

    public boolean hasPendingRefresh() {
        return pendingRefresh;
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
        documentListener = null;
        pendingRefresh = false;
        refreshScheduled = false;
        internalDocumentUpdate = false;
        recordComponentLoaded = false;
        displayedRecordModelRow = -1;
        pendingDocumentText = null;
        if (recordPane != null) {
            recordPane.removeAll();
        }
        removeAll();
        dataList.clear();
    }

    protected void readFile() throws Exception {
        String extension = this.virtualFile.getExtension();
        if (!"xls".equals(extension)) {
            throw new Exception("Error: invalid spinner file");
        }
        List<String> lines = ReadAction.compute(() -> {
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            return document == null ? null : parseLines(document.getText());
        });
        try {
            if (lines == null || lines.isEmpty()) {
                lines = FileUtil.readLines(virtualFile.getPath(), virtualFile.getCharset());
            }
            if (lines == null || lines.isEmpty()) {
                throw new Exception("Error: file is empty");
            }
        } catch (Exception e) {
            throw new Exception("Error: error while reading file");
        }
        String header = lines.get(0);
        headers = header.split("\t");
        lines.remove(0);
        dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
    }

    private static List<String> parseLines(@NotNull String text) {
        return CharSequenceUtil.split(text.replace("\r\n", "\n").replace('\r', '\n'), "\n");
    }

    private int getSelectedModelRow() {
        int selectedRow = table.getSelectedRow();
        return selectedRow >= 0 ? table.convertRowIndexToModel(selectedRow) : -1;
    }

    private void restoreSelection(int selectedModelRow) {
        if (selectedModelRow < 0 || selectedModelRow >= tableModel.getRowCount()) {
            table.clearSelection();
            return;
        }
        int viewRow = table.convertRowIndexToView(selectedModelRow);
        if (viewRow >= 0) {
            table.setRowSelectionInterval(viewRow, viewRow);
        } else {
            table.clearSelection();
        }
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
        @SuppressWarnings("unchecked")
        Vector<String> vector = tableModel.getDataVector().get(modelRowIndex);
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
