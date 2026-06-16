package cn.github.spinner.ui;

import cn.github.driver.MatrixDriver;
import cn.github.spinner.config.MatrixDriversConfig;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.MatrixConnectorPackageManager;
import cn.github.spinner.util.MatrixJarClassLoader;
import cn.github.spinner.util.UIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Slf4j
public class MatrixDriversDialog extends DialogWrapper {
    private final Project project;
    private DefaultListModel<String> driverListModel;
    private JBList<String> driverUIList;
    private DefaultTableModel driverTableModel;
    private JBTable driverTable;
    private ComboBox<String> driverClassComboBox;
    private JBTextField driverNameField;
    private final List<String> existedJars = new ArrayList<>();

    public MatrixDriversDialog(@Nullable Project project) {
        super(true); // 使用当前窗口作为父窗口
        this.project = project;
        setTitle(SpinnerBundle.message("dialog.matrix.drivers.title"));
        setSize(1000, 600);
        initComponent();
        init();
    }

    private void initComponent() {
        driverNameField = new JBTextField();
        driverClassComboBox = new ComboBox<>();

        driverTableModel = new DefaultTableModel(new Object[]{
                SpinnerBundle.message("table.column.driver.jar"),
                SpinnerBundle.message("table.column.path")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 禁止编辑
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
        driverTable = new JBTable(driverTableModel);
        driverTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        driverTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        driverTable.getEmptyText().setText(SpinnerBundle.message("message.driver.files.empty"));
        driverTable.getEmptyText().appendSecondaryText(" " + SpinnerBundle.message("button.matrix.connector"),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> addMatrixConnectorJar());
        // 设置列宽
        driverTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        driverTable.getColumnModel().getColumn(1).setPreferredWidth(350);

        driverListModel = new DefaultListModel<>();
        Map<String, MatrixDriversConfig.DriverInfo> driversMap = MatrixDriversConfig.getInstance().getDriversMap();
        List<String> driverNameList = driversMap.keySet().stream().sorted().toList();
        driverNameList.forEach(key -> driverListModel.addElement(key));
        driverUIList = new JBList<>(driverListModel);
        driverUIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        driverUIList.addListSelectionListener(this::driverSelectionChanged);
        driverUIList.setSelectedIndex(0);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JComponent leftComponent = createLeftComponent();
        panel.add(leftComponent, BorderLayout.WEST);

        JComponent contentComponent = createContentComponent();
        panel.add(contentComponent, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createLeftComponent() {
        JPanel driversPanel = ToolbarDecorator.createDecorator(driverUIList)
                .setAddAction(btn -> addDriver())
                .setRemoveAction(btn -> removeDriver())
                .setRemoveActionUpdater(e -> driverUIList.getSelectedIndex() >= 0)
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .disableUpDownActions()
                .createPanel();
        JBScrollPane scrollPane = new JBScrollPane(driversPanel);
        scrollPane.setPreferredSize(JBUI.size(200, 600));
        return scrollPane;
    }

    private JComponent createContentComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(SpinnerBundle.message("label.name"), driverNameField)
                .addLabeledComponent(SpinnerBundle.message("label.class"), driverClassComboBox)
                .addSeparator()
                .getPanel();
        panel.add(formPanel, BorderLayout.NORTH);

        JPanel tablePanel = ToolbarDecorator.createDecorator(driverTable)
                .setAddAction(btn -> addJarFiles())
                .setAddActionUpdater(e -> true)
                .setRemoveAction(btn -> removeSelectedJar())
                .setRemoveActionUpdater(e -> driverTable.getSelectedRowCount() > 0)
                .addExtraAction(new MatrixConnectorAction())
                .setToolbarPosition(ActionToolbarPosition.TOP)
                .disableUpDownActions() // 禁用上下移动按钮
                .createPanel();
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 添加JAR文件
     */
    private void addJarFiles() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, false, true, false, false, true)
                .withExtensionFilter(SpinnerBundle.message("filechooser.jar.filter"), "jar")
                .withTitle(SpinnerBundle.message("filechooser.select.jar.title"));
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, null, null);
        for (VirtualFile file : files) {
            String path = file.getCanonicalPath();
            if (path != null && !existedJars.contains(path)) {
                existedJars.add(path);
                driverTableModel.addRow(new Object[]{file.getName(), path});
            }
        }
        reloadDriverImplementation();
    }

    private void addMatrixConnectorJar() {
        int selectedIndex = driverUIList.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        String driverName = driverListModel.elementAt(selectedIndex);
        if (MatrixConnectorPackageManager.isDownloaded()) {
            addMatrixConnectorJarToCurrentDriver(MatrixConnectorPackageManager.getCachedJarFile(), driverName, true);
            return;
        }

        new Task.Backgroundable(project, SpinnerBundle.message("progress.download.matrix.connector"), true) {
            private File connectorFile;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(SpinnerBundle.message("progress.downloading", MatrixConnectorPackageManager.FILE_NAME));
                try {
                    connectorFile = MatrixConnectorPackageManager.getOrDownload();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSuccess() {
                addMatrixConnectorJarToCurrentDriver(connectorFile, driverName, false);
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                UIUtil.showErrorNotification(project,
                        SpinnerBundle.message("notification.title.matrix.drivers"),
                        SpinnerBundle.message("message.matrix.connector.download.failed", t.getMessage()));
            }
        }.queue();
    }

    private void addMatrixConnectorJarToCurrentDriver(File connectorFile, String driverName, boolean reused) {
        if (connectorFile == null || !connectorFile.exists()) {
            return;
        }
        int selectedIndex = driverUIList.getSelectedIndex();
        String selectedDriverName = selectedIndex >= 0 ? driverListModel.elementAt(selectedIndex) : "";
        if (driverName.equals(selectedDriverName)) {
            if (addJarFileToTable(connectorFile)) {
                reloadDriverImplementation();
                selectMatrixConnectorDriverClass();
                UIUtil.showNotification(project,
                        SpinnerBundle.message("notification.title.matrix.drivers"),
                        reused
                                ? SpinnerBundle.message("message.matrix.connector.reused", driverName)
                                : SpinnerBundle.message("message.matrix.connector.added", driverName));
            } else {
                UIUtil.showNotification(project,
                        SpinnerBundle.message("notification.title.matrix.drivers"),
                        SpinnerBundle.message("message.matrix.connector.exists", driverName));
            }
            return;
        }

        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(driverName);
        if (addMatrixConnectorToDriverInfo(driverInfo, connectorFile)) {
            UIUtil.showNotification(project,
                    SpinnerBundle.message("notification.title.matrix.drivers"),
                    reused
                            ? SpinnerBundle.message("message.matrix.connector.reused", driverName)
                            : SpinnerBundle.message("message.matrix.connector.added", driverName));
        } else {
            UIUtil.showNotification(project,
                    SpinnerBundle.message("notification.title.matrix.drivers"),
                    SpinnerBundle.message("message.matrix.connector.exists", driverName));
        }
    }

    private boolean addJarFileToTable(File file) {
        String path = getJarPath(file);
        if (existedJars.contains(path)) {
            return false;
        }
        existedJars.add(path);
        driverTableModel.addRow(new Object[]{file.getName(), path});
        return true;
    }

    private boolean addMatrixConnectorToDriverInfo(MatrixDriversConfig.DriverInfo driverInfo, File file) {
        String path = getJarPath(file);
        boolean exists = driverInfo.getDriverFiles().stream().anyMatch(driverFile -> path.equals(driverFile.getPath()));
        if (exists) {
            return false;
        }
        driverInfo.getDriverFiles().add(new MatrixDriversConfig.DriverFile(file.getName(), path));
        if (driverInfo.getDriverClass() == null || driverInfo.getDriverClass().isEmpty()) {
            driverInfo.setDriverClass(MatrixConnectorPackageManager.DRIVER_CLASS_NAME);
        }
        return true;
    }

    private void selectMatrixConnectorDriverClass() {
        String driverClassName = MatrixConnectorPackageManager.DRIVER_CLASS_NAME;
        boolean exists = false;
        ComboBoxModel<String> model = driverClassComboBox.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (driverClassName.equals(model.getElementAt(i))) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            driverClassComboBox.addItem(driverClassName);
        }
        driverClassComboBox.setSelectedItem(driverClassName);
    }

    private String getJarPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * 移除选中的JAR包
     */
    private void removeSelectedJar() {
        int[] selectedRows = driverTable.getSelectedRows();
        if (selectedRows.length > 0) {
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = driverTable.convertRowIndexToModel(selectedRows[i]);
                String path = driverTableModel.getValueAt(modelRow, 1).toString();
                existedJars.remove(path);
                driverTableModel.removeRow(modelRow);
            }
            reloadDriverImplementation();
        }
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        storeDriverConfig();
        super.doOKAction();
    }

    private void driverSelectionChanged(ListSelectionEvent event) {
        int selectedIndex = driverUIList.getSelectedIndex();
        if (selectedIndex < 0) return;

        driverClassComboBox.removeAllItems();
        driverClassComboBox.addItem(SpinnerBundle.message("message.driver.not.specified"));
        driverTableModel.setRowCount(0);
        existedJars.clear();

        String driverName = driverListModel.elementAt(selectedIndex);
        driverNameField.setText(driverName);
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(driverName);
        if (driverInfo != null) {
            List<MatrixDriversConfig.DriverFile> driverFiles = driverInfo.getDriverFiles();
            for (MatrixDriversConfig.DriverFile driverFile : driverFiles) {
                existedJars.add(driverFile.getPath());
                driverTableModel.addRow(new Object[]{driverFile.getName(), driverFile.getPath()});
            }

            reloadDriverImplementation();

            String driverClass = driverInfo.getDriverClass();
            if (driverClass != null && !driverClass.isEmpty()) {
                driverClassComboBox.setSelectedItem(driverClass);
            }
        }
    }

    private void addDriver() {
        String driverName = SpinnerBundle.message("message.user.driver");
        int count = 0;
        for (int i = 0; i < driverListModel.getSize(); i++) {
            if (driverListModel.get(i).startsWith(driverName)) {
                count++;
            }
        }
        driverName = count > 0 ? driverName + " [" + (count + 1) + "]" : driverName;
        driverListModel.addElement(driverName);
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(driverName);
        if (MatrixConnectorPackageManager.isDownloaded()) {
            addMatrixConnectorToDriverInfo(driverInfo, MatrixConnectorPackageManager.getCachedJarFile());
        }
    }

    private void removeDriver() {
        int selectedIndex = driverUIList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String driverName = driverListModel.get(selectedIndex);
            driverListModel.remove(selectedIndex);
            MatrixDriversConfig.getInstance().removeDriver(driverName);
        }
    }

    private void reloadDriverImplementation() {
        driverClassComboBox.removeAllItems();
        driverClassComboBox.addItem(SpinnerBundle.message("message.driver.not.specified"));
        int rowCount = driverTableModel.getRowCount();
        List<File> files = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            String path = driverTableModel.getValueAt(i, 1).toString();
            files.add(new File(path));
        }
        if (files.isEmpty()) {
            return;
        }
        List<String> implementations = new ArrayList<>();
        try (MatrixJarClassLoader classLoader = new MatrixJarClassLoader(files, this.getClass().getClassLoader())) {
            ServiceLoader<MatrixDriver> serviceLoader = ServiceLoader.load(MatrixDriver.class, classLoader);
            for (MatrixDriver implementation : serviceLoader) {
                implementations.add(implementation.getClass().getName());
            }
            implementations.stream().distinct().forEach(driverClassComboBox::addItem);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ServiceConfigurationError | LinkageError e) {
            log.warn("Load matrix driver implementation failed.", e);
        }
    }

    private void storeDriverConfig() {
        int selectedIndex = driverUIList.getSelectedIndex();
        if (selectedIndex < 0) return;

        String newDriverName = driverNameField.getText().trim();
        if (newDriverName.isEmpty()) {
            setErrorText(SpinnerBundle.message("message.driver.name.required"), driverNameField);
            return;
        }

        setErrorText(null);
        String driverName = driverListModel.elementAt(selectedIndex);
        MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(driverName);
        if (driverInfo != null) {
            MatrixDriversConfig.getInstance().removeDriver(driverName);
        }
        driverInfo = new MatrixDriversConfig.DriverInfo();
        String driverClass = driverClassComboBox.getItem().trim();
        driverClass = SpinnerBundle.message("message.driver.not.specified").equals(driverClass) ? "" : driverClass;
        driverInfo.setDriverClass(driverClass);

        List<MatrixDriversConfig.DriverFile> driverFiles = new ArrayList<>();
        int rowCount = driverTableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = driverTableModel.getValueAt(i, 0).toString();
            String path = driverTableModel.getValueAt(i, 1).toString();
            driverFiles.add(new MatrixDriversConfig.DriverFile(name, path));
        }
        driverInfo.setDriverFiles(driverFiles);
        MatrixDriversConfig.getInstance().putDriver(newDriverName, driverInfo);
        driverListModel.set(selectedIndex, newDriverName);
        UIUtil.showNotification(this.project, SpinnerBundle.message("notification.title.matrix.drivers"), SpinnerBundle.message("message.driver.saved", newDriverName));
    }

    @Override
    protected Action @NotNull [] createActions() {
        ApplyAction applyAction = new ApplyAction();
        return new Action[]{this.getOKAction(), this.getCancelAction(), applyAction};
    }

    private final class MatrixConnectorAction extends AnActionButton {
        private MatrixConnectorAction() {
            super(SpinnerBundle.message("button.matrix.connector"),
                    SpinnerBundle.message("tooltip.matrix.connector"),
                    AllIcons.Actions.Download);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            addMatrixConnectorJar();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    protected final class ApplyAction extends DialogWrapperAction {
        private ApplyAction() {
            super(CommonBundle.getApplyButtonText());
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            storeDriverConfig();
        }
    }
}
