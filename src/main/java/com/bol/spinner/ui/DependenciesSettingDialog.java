package com.bol.spinner.ui;

import com.bol.spinner.config.SpinnerSettings;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileUtil;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class DependenciesSettingDialog extends DialogWrapper {
    private final Project project;
    private JBTable jarTable;
    private DefaultTableModel tableModel;
    private List<VirtualFile> existedJars = new ArrayList<>();

    private static final Set<String> TARGET_JAR_NAMES = new HashSet<>(Arrays.asList(
            "eMatrixServletRMI.jar",
            "enoviaKernel.jar",
            "FcsBackEnd.jar",
            "FcsClient.jar",
            "FcsServer.jar",
            "m1jsystem.jar",
            "mx_jdom_1.0.jar",
            "slf4j-api.jar"
    ));

    public DependenciesSettingDialog(Project project) {
        super(true); // 使用当前窗口作为父窗口
        this.project = project;
        setTitle("Dependencies Setting");
        setSize(600, 400);
        setOKButtonText("OK");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        initJarTable();
        // 使用ToolbarDecorator创建带工具栏的表格
        JPanel tablePanel = ToolbarDecorator.createDecorator(jarTable)
                .setAddAction(btn -> addJarFiles())
                .setAddActionUpdater(e -> true)
                .setRemoveAction(btn -> removeSelectedJar())
                .setRemoveActionUpdater(e -> jarTable.getSelectedRowCount() > 0)
                .setToolbarPosition(ActionToolbarPosition.RIGHT)
                .disableUpDownActions() // 禁用上下移动按钮
                .createPanel();
        panel.add(new JBScrollPane(tablePanel), BorderLayout.CENTER);
        // 添加信息提示面板
        JPanel infoPanel = createInfoPanel();
        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
    }

    private void initJarTable() {
        // 创建表格
        tableModel = new DefaultTableModel(new Object[]{"Jar Name", "Absolute Path"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止编辑
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
        jarTable = new JBTable(tableModel);
        jarTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jarTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        // 设置列宽
        jarTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        jarTable.getColumnModel().getColumn(1).setPreferredWidth(350);

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<SpinnerSettings.Dependency> dependencies = spinnerSettings.getDependencies();
        for (SpinnerSettings.Dependency dependency : dependencies) {
            existedJars.add(VirtualFileManager.getInstance().findFileByNioPath(Path.of(dependency.getPath())));
            tableModel.addRow(new Object[]{dependency.getName(), dependency.getPath()});
        }
    }

    /**
     * 创建信息提示面板
     */
    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                JBUI.Borders.empty(5)
        ));
        JLabel infoLabel = new JLabel("<html>选择以下特定的JAR包:<br>" +
                "eMatrixServletRMI.jar, enoviaKernel.jar, FcsBackEnd.jar, FcsClient.jar, " +
                "FcsServer.jar, m1jsystem.jar, mx_jdom_1.0.jar, slf4j-api.jar</html>");
        infoLabel.setBorder(JBUI.Borders.empty(5));
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        return infoPanel;
    }

    /**
     * 添加JAR文件
     */
    private void addJarFiles() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, false, true, false, false, true)
                .withExtensionFilter("Jar File (*.jar)", "jar")
                .withTitle("选择JAR包");
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        for (VirtualFile file : files) {
            if (!existedJars.contains(file) && TARGET_JAR_NAMES.contains(file.getName())) {
                existedJars.add(file);
                tableModel.addRow(new Object[]{file.getName(), file.getPath()});
            }
        }
    }

    /**
     * 移除选中的JAR包
     */
    private void removeSelectedJar() {
        int[] selectedRows = jarTable.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = jarTable.convertRowIndexToModel(selectedRows[i]);
                existedJars.remove(modelRow);
                tableModel.removeRow(modelRow);
            }
        }
    }

    /**
     * 获取所有选中的JAR包文件
     */
    public List<VirtualFile> getJars() {
        return new ArrayList<>(existedJars);
    }

    /**
     * 检查是否包含所有必需的JAR包
     */
    public boolean validateJarFiles() {
        List<String> existedJarNames = existedJars.stream().map(VirtualFile::getName).toList();
        // 获取缺失的JAR包列表
        List<String> missingJars = new ArrayList<>();
        for (String requiredJar : TARGET_JAR_NAMES) {
            if (!existedJarNames.contains(requiredJar)) {
                missingJars.add(requiredJar);
            }
        }
        if (!missingJars.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                    this.getContentPane(),
                    "缺少以下必需的JAR包：\n" + String.join("\n", missingJars) + "\n\n是否继续？",
                    "缺少必需JAR包",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            return result == JOptionPane.YES_OPTION;
        }
        return true;
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        if (validateJarFiles()) {
            super.doOKAction();
        }
    }
}
