package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
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
    private String pendingDocumentText;

    public AbstractSpinnerViewComponent(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        try {
            readFile();
            initComponents();
            setupListener();
            setupLayout();
            setValue();
        } catch (Exception e) {
            table = new FilterTable();
            table.getEmptyText().setText(e.getMessage());
            add(table);
        }
    }

    protected void initComponents() {
        tableModel = new RowNumberTableModel(headers, 0);
        table = new FilterTable(tableModel);
        recordPane = new JBTabbedPane();
        recordPane.add("Record", new JPanel());
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
                    }
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingTable) return;

            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                recordPane.setComponentAt(0, new JPanel());
                return;
            }

            int modelRowIndex = table.convertRowIndexToModel(selectedRow);
            showRecordForRow(modelRowIndex);
        });
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            log.warn("Document not found for file: {}", virtualFile.getPath());
            return;
        }
        documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                SwingUtilities.invokeLater(() -> {
                    pendingDocumentText = event.getDocument().getText();
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

    public void refreshFromDocument() {
        String text = pendingDocumentText;
        if (text == null) {
            text = ReadAction.compute(() -> {
                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                return document == null ? null : document.getText();
            });
        }
        if (text != null) {
            refreshFromText(text);
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
        int selectedModelRow = getSelectedModelRow();
        boolean recordVisible = recordPane.isVisible();
        updatingTable = true;
        tableModel.setRowCount(0);
        dataList.clear();
        List<String> lines = parseLines(text);
        if (!lines.isEmpty()) {
            lines.remove(0);
            dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
        }
        setValue();
        updatingTable = false;
        restoreSelection(selectedModelRow);
        refreshRecordPane(selectedModelRow, recordVisible);
        pendingRefresh = false;
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
            recordPane.setComponentAt(0, new JPanel());
            return;
        }
        int viewRow = table.convertRowIndexToView(selectedModelRow);
        if (viewRow < 0) {
            recordPane.setComponentAt(0, new JPanel());
            return;
        }
        showRecordForRow(selectedModelRow);
    }

    private void showRecordForRow(int modelRowIndex) {
        if (modelRowIndex < 0 || modelRowIndex >= tableModel.getRowCount()) {
            recordPane.setComponentAt(0, new JPanel());
            return;
        }
        @SuppressWarnings("unchecked")
        Vector<String> vector = tableModel.getDataVector().get(modelRowIndex);
        JComponent component = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this)
                .setProject(project).build(headers, vector);
        recordPane.setComponentAt(0, component);
    }

    public class RecordPaneVisibleAction extends ToggleAction {
        public RecordPaneVisibleAction() {
            super("Show / Hide Record View", "Show / hide record view", AllIcons.Nodes.Record);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            recordPane.setVisible(!recordPane.isVisible());
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return recordPane.isVisible();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean b) {
            recordPane.setVisible(b);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }
}
