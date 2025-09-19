package com.bol.spinner.ui;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class DependenciesSettingDialog extends DialogWrapper {
    private final Project project;
    private LoadAction loadAction;
    private JBTable jarTable;
    private DefaultTableModel tableModel;
    private final List<VirtualFile> selectedJars = new ArrayList<>();

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

        // 使用ToolbarDecorator创建带工具栏的表格
        JPanel tablePanel = ToolbarDecorator.createDecorator(jarTable)
                .setAddAction(btn -> addJarFiles())
                .setAddActionUpdater(e -> true)
                .setRemoveAction(btn -> removeSelectedJar())
                .setRemoveActionUpdater(e -> jarTable.getSelectedRowCount() > 0)
                .setToolbarPosition(ActionToolbarPosition.RIGHT)
                .disableUpDownActions() // 禁用上下移动按钮
                .createPanel();
        panel.add(tablePanel, BorderLayout.CENTER);
        // 添加信息提示面板
        JPanel infoPanel = createInfoPanel();
        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
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
            if (!selectedJars.contains(file)) {
                if (TARGET_JAR_NAMES.contains(file.getName())) {
                    selectedJars.add(file);
                    tableModel.addRow(new Object[]{file.getName(), file.getPath()});
                }
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
                selectedJars.remove(modelRow);
                tableModel.removeRow(modelRow);
            }
        }
    }

    /**
     * 获取所有选中的JAR包文件
     */
    public List<VirtualFile> getSelectedJars() {
        return new ArrayList<>(selectedJars);
    }

    /**
     * 检查是否包含所有必需的JAR包
     */
    public boolean validateJarFiles() {
        boolean containsAllRequiredJars = TARGET_JAR_NAMES.stream().allMatch(jarName -> selectedJars.stream()
                        .anyMatch(file -> file.getName().equals(jarName)));
        if (!containsAllRequiredJars) {
            List<String> missingJars = getMissingJars();
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

    /**
     * 获取缺失的JAR包列表
     */
    public List<String> getMissingJars() {
        List<String> missing = new ArrayList<>();
        for (String requiredJar : TARGET_JAR_NAMES) {
            if (selectedJars.stream().noneMatch(file -> file.getName().equals(requiredJar))) {
                missing.add(requiredJar);
            }
        }
        return missing;
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        if (validateJarFiles()) {
            super.doOKAction();
        }
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        this.loadAction = new LoadAction();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[] {loadAction, getOKAction(), getCancelAction()};
    }

    protected class LoadAction extends DialogWrapperAction {
        protected LoadAction() {
            super("Load");
            this.addPropertyChangeListener((evt) -> {
                if ("Name".equals(evt.getPropertyName())) {
                    DependenciesSettingDialog.this.repaint();
                }
            });
            this.putValue("MacActionOrder", 100);
        }

        protected void doAction(ActionEvent e) {
            if (validateJarFiles()) {
                DependenciesSettingDialog.super.doOKAction();
            }
        }
    }
}
