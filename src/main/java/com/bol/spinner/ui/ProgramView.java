package com.bol.spinner.ui;

import com.bol.spinner.editor.MQLLanguage;
import com.bol.spinner.editor.ui.MQLConsoleManager;
import com.bol.spinner.util.MQLUtil;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.TextEditor;
import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ProgramView implements FileEditor {

    private final JBPanel<?> myPanel;
    private final Project myProject;
    private final VirtualFile myFile;
    private JBTextField filterTextField;
    private JTable programTable;
    private DefaultTableModel programTableModel;
    private final List<String[]> programTableData;
    private final List<File> tempJavaFiles = new ArrayList<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public ProgramView(Project project, VirtualFile file) {
        myProject = project;
        myFile = file;
        programTableData = new ArrayList<>();
        initProgList();
        myPanel = new JBPanel<>(new BorderLayout(10, 10));
        myPanel.setBorder(JBUI.Borders.empty(8));
        initializeComponents();
        setupLayout();
        setupListeners();
    }

    private void initializeComponents() {
        // 过滤输入框
        filterTextField = new JBTextField(20);
        filterTextField.setToolTipText("Input program name to filter...");
        filterTextField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.LIGHT_GRAY),
                JBUI.Borders.empty(4, 6)
        ));

        // 表格模型（列：名称、创建时间、修改时间、用户）
        String[] tableColumnNames = {"名称", "创建时间", "修改时间"};
        programTableModel = new DefaultTableModel(tableColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        loadProgramsToTableModel();

        // 表格组件
        programTable = new JBTable(programTableModel);
        programTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        programTable.setRowHeight(26);
        programTable.setShowGrid(true);          // 显示网格线
        programTable.setGridColor(new JBColor(Gray._235, Gray._180)); // 浅灰色网格
        programTable.setIntercellSpacing(new Dimension(1, 1));
        programTable.setBackground(JBColor.WHITE); // 白色背景
        JTableHeader header = programTable.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12)); // 加粗字体增强辨识度
        // 表头背景色 - 使用浅灰色背景增强层次感
        header.setBackground(new JBColor(Gray._245, Gray._70));
        header.setForeground(new JBColor(Color.BLACK, Color.WHITE)); // 深色文字提高可读性
        // 改进边框 - 底部边框更明显，与内容区分
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new JBColor(Gray._200, Gray._100)),
                JBUI.Borders.empty(5)
        ));
        // 设置表头单元格渲染器
        header.setDefaultRenderer(new HeaderRenderer());
        // 表头高度调整
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
        // 启用排序视觉反馈
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);
        // 列宽调整（适配长文本）
        TableColumn nameCol = programTable.getColumnModel().getColumn(0);
        nameCol.setPreferredWidth(300); // 名称列加宽
        programTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        programTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        // 自定义单元格渲染器（自然选中色）
        programTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());
    }

    /**
     * 表头单元格渲染器 - 优化每个表头单元格的样式
     */
    private class HeaderRenderer implements TableCellRenderer {
        private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

        public HeaderRenderer() {
            renderer.setHorizontalAlignment(SwingConstants.LEFT);
            renderer.setBorder(JBUI.Borders.empty(0, 6)); // 增加左侧内边距
            renderer.setFont(new Font("SansSerif", Font.BOLD, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // 表头背景色与表头一致
            renderer.setBackground(programTable.getTableHeader().getBackground());
            renderer.setForeground(programTable.getTableHeader().getForeground());
            return renderer;
        }
    }

    private static class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(JBUI.Borders.empty(2, 6)); // 增加内边距，让内容不拥挤

            if (isSelected) {
                setBackground(new JBColor(new Color(53, 93, 225, 220), new Color(53, 93, 225, 220))); // 选中行背景色
                setForeground(JBColor.WHITE); // 选中行文字设为白色，提高对比度
            } else {
                setBackground(table.getBackground());
                setForeground(JBColor.BLACK);
            }

            String cellText = value != null ? value.toString() : "";
            if (cellText.startsWith("Failed") || cellText.startsWith("No ")) {
                setForeground(JBColor.GRAY);
            }

            return this;
        }
    }

    private void loadProgramsToTableModel() {
        programTableModel.setRowCount(0);
        if (programTableData.isEmpty()) {
            programTableModel.addRow(new String[]{"No programs found", "", "", ""});
        } else {
            for (String[] rowData : programTableData) {
                programTableModel.addRow(rowData);
            }
        }
    }

    private void initProgList() {
        programTableData.clear();
        try {
            String allProgram = MQLUtil.execute("list prog select name Originated Modified dump");
            if (CharSequenceUtil.isNotBlank(allProgram)) {
                String[] rawRows = allProgram.split("\n");
                for (String rawRow : rawRows) {
                    String trimmedRow = rawRow.trim();
                    if (CharSequenceUtil.isBlank(trimmedRow)) continue;
                    String[] rowColumns = trimmedRow.split(","); // 需匹配MQL返回的分隔符
                    String[] formattedRow = new String[4];
                    formattedRow[0] = rowColumns.length > 0 ? rowColumns[0] : "";
                    formattedRow[1] = rowColumns.length > 1 ? rowColumns[1] : "";
                    formattedRow[2] = rowColumns.length > 2 ? rowColumns[2] : "";
                    programTableData.add(formattedRow);
                }
                programTableData.sort((row1, row2) -> row1[0].compareToIgnoreCase(row2[0]));
            }
        } catch (MQLException e) {
            String errorMsg = "Failed to load programs: " + e.getMessage();
            programTableData.add(new String[]{errorMsg, "", "", ""});
            Messages.showWarningDialog(myProject, errorMsg, "Program List Load Error");
        }
    }

    private void setupLayout() {
        // 顶部：过滤+刷新
        myPanel.add(createTopPanel(), BorderLayout.NORTH);

        // 中间：表格滚动面板（完全铺满）
        JScrollPane tableScrollPane = ScrollPaneFactory.createScrollPane(programTable);
        tableScrollPane.setMinimumSize(new Dimension(600, 400));
        // 优化滚动面板边框
        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.LIGHT_GRAY),
                JBUI.Borders.empty(2)
        ));
        myPanel.add(tableScrollPane, BorderLayout.CENTER);
    }

    private JComponent createTopPanel() {
        JBPanel<?> topPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JBLabel filterLabel = new JBLabel("Filter:");
        topPanel.add(filterLabel);
        topPanel.add(filterTextField);

        JButton refreshBtn = new JButton("Refresh List");
        refreshBtn.setMargin(JBUI.insets(2, 8));
        refreshBtn.addActionListener(e -> refreshProgramList());
        topPanel.add(refreshBtn);

        return topPanel;
    }

    private void setupListeners() {
        // 表格选择监听
        programTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = programTable.getSelectedRow();
                if (selectedRow != -1) {
                    String programName = programTableModel.getValueAt(selectedRow, 0).toString();
                    if (!programName.startsWith("Failed") && !programName.startsWith("No ")) {
                        openProgramInNativeEditor(programName);
                    }
                }
            }
        });

        // 过滤延迟监听
        AtomicReference<ScheduledFuture<?>> filterFuture = new AtomicReference<>();
        filterTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleFilter();
            }

            private void scheduleFilter() {
                ScheduledFuture<?> future = filterFuture.getAndSet(null);
                if (future != null) future.cancel(false);
                filterFuture.set(executor.schedule(() -> {
                    SwingUtilities.invokeLater(() -> doFilter());
                }, 300, TimeUnit.MILLISECONDS));
            }
        });
    }

    private void doFilter() {
        String filterText = filterTextField.getText().toLowerCase().trim();
        programTableModel.setRowCount(0);

        List<String[]> matchedRows = new ArrayList<>();
        for (String[] rowData : programTableData) {
            String programName = rowData[0].toLowerCase();
            if (programName.contains(filterText)) {
                matchedRows.add(rowData);
            }
        }

        if (matchedRows.isEmpty()) {
            programTableModel.addRow(new String[]{"No programs match: " + filterText, "", "", ""});
        } else {
            for (String[] matchedRow : matchedRows) {
                programTableModel.addRow(matchedRow);
            }
        }
        programTable.clearSelection();
    }

    private void refreshProgramList() {
        Messages.showInfoMessage(myProject, "Refreshing program list...", "Refreshing");
        initProgList();
        loadProgramsToTableModel();
        programTable.clearSelection();
        Messages.showInfoMessage(myProject, "Refresh completed", "Refresh Success");
    }

    private void openProgramInNativeEditor(String programName) {
        try {
            String programCode = generateJavaCode(programName);
            String programType = getProgType(programName);

            if (programCode.contains("the program is empty")) {
                Messages.showInfoMessage(myProject, programCode, "Empty Program");
                return;
            }

            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);

            // ---------- 分支1：MQL类型 → 调用MQL控制台/编辑器 ----------
            if ("MQL".equals(programType)) {
                LightVirtualFile mqlVirtualFile = new LightVirtualFile(programName);
                mqlVirtualFile.setLanguage(MQLLanguage.INSTANCE); // 关联MQL语言（语法高亮等）
                mqlVirtualFile.setContent(programCode, programCode, true);
                mqlVirtualFile.setWritable(true);
                try {
                    MQLConsoleManager consoleManager = MQLConsoleManager.getInstance(myProject);
                    consoleManager.openOrFocusMQLConsole(mqlVirtualFile);
                } catch (Exception ex) {
                    UIUtil.showErrorNotification(myProject,
                            "Failed to open MQL Editor: " + ex.getMessage(),
                            "MQL Editor Error");
                }
            }
            // ---------- 分支2：JAVA/Other类型 → 生成临时文件，用文本编辑器打开 ----------
            else {
                String fileExt = "JAVA".equals(programType) ? ".java" : ".txt";
                String safeFileName = programName.replaceAll("[^a-zA-Z0-9_.-]", "_") + fileExt;
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "SpinnerPrograms");
                if (!tempDir.exists()) tempDir.mkdirs();
                File tempFile = new File(tempDir, safeFileName);
                tempJavaFiles.add(tempFile); // 统一管理所有临时文件

                try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
                    writer.write(programCode);
                }

                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(tempFile);
                if (virtualFile == null) {
                    throw new IOException("Temporary file not found");
                }

                fileEditorManager.openFile(virtualFile, true);

                // 设置文件为只读
                for (FileEditor editor : fileEditorManager.getAllEditors(virtualFile)) {
                    if (editor instanceof TextEditor) {
                        Editor textEditor = ((TextEditor) editor).getEditor();
                        textEditor.getDocument().setReadOnly(true);
                        break;
                    }
                }
            }
        } catch (IOException | MQLException e) {
            Messages.showErrorDialog(myProject, "Open failed: " + e.getMessage(), "Error");
        }
    }

    private static String generateJavaCode(String programName) throws MQLException {
        String content = MQLUtil.execute("list prog {} select code dump", programName);
        return StringUtils.isEmpty(content.trim()) ? "the program is empty!" : content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
    }

    private static String getProgType(String programName) throws MQLException {
        String[] typeArray = MQLUtil.execute("list prog {} select ismqlprogram isjavaprogram dump", programName).split(",");
        if (typeArray.length == 2) {
            if (typeArray[0].trim().equalsIgnoreCase("TRUE")) {
                return "MQL";
            } else if (typeArray[1].trim().equalsIgnoreCase("TRUE")) {
                return "JAVA";
            }
        }
        return "Other";

    }

    // ====================== FileEditor接口实现 ======================
    @NotNull
    @Override
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return filterTextField;
    }

    @NotNull
    @Override
    public String getName() {
        return "Program List";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return myFile.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
        tempJavaFiles.forEach(file -> {
            if (file.exists()) file.delete();
        });
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "SpinnerPrograms");
        if (tempDir.exists() && Objects.requireNonNull(tempDir.listFiles()).length == 0)
            tempDir.delete();
        tempJavaFiles.clear();
    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    }
}
