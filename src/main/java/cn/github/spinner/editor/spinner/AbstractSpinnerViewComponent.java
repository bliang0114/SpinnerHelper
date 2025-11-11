package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
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
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractSpinnerViewComponent extends JPanel {
    protected final Project project;
    protected final VirtualFile virtualFile;
    protected FilterTable table;
    protected DefaultTableModel tableModel;
    protected DefaultActionGroup actionGroup;
    protected RecordPaneVisibleAction recordPaneVisibleAction;
    protected JBTabbedPane recordPane;
    protected String[] headers;
    protected final List<String[]> dataList = new ArrayList<>();

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
            if (e.getValueIsAdjusting()) return;

            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) return;

            int modelRowIndex = table.convertRowIndexToModel(selectedRow);
            if (modelRowIndex < 0) return;
            JComponent component = SpinnerDataRecordBuilder.createBuilder(this.virtualFile, modelRowIndex, AbstractSpinnerViewComponent.this)
                    .setProject(project).build(headers, tableModel.getDataVector().get(modelRowIndex));
            recordPane.setComponentAt(0, component);
        });
        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        assert document != null;
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    dataList.clear();
                    String text = event.getDocument().getText();
                    if (text.contains("\n")) {
                        List<String> lines = CharSequenceUtil.split(text, "\n");
                        if (!lines.isEmpty()) {
                            lines.remove(0);
                        }
                        dataList.addAll(lines.stream().map(line -> line.split("\t")).toList());
                        setValue();
                    }
                });
            }
        });
    }

    protected AnAction[] createToolbarAction() {
        return new AnAction[] {new RecordPaneVisibleAction()};
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
        for (int i = 0; i < columnCount; i++) {
            String value = i >= values.length ? "" : values[i];
            tableModel.setValueAt(value, rowIndex, i);
        }
    }

    protected void readFile() throws Exception {
        String extension = this.virtualFile.getExtension();
        if (!"xls".equals(extension)) {
            throw new Exception("Error: invalid spinner file");
        }
        List<String> lines;
        try {
            lines = FileUtil.readLines(virtualFile.getPath(), virtualFile.getCharset());
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

    public class RecordPaneVisibleAction extends ToggleAction {
        public RecordPaneVisibleAction() {
            super("Show / Hide Record View", "Show / Hide Record View", AllIcons.Nodes.Record);
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
