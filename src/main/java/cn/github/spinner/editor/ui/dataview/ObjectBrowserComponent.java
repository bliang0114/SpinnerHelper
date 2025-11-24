package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixObjectQuery;
import cn.github.driver.connection.MatrixQueryResult;
import cn.github.spinner.components.ComboBoxWithFilter;
import cn.github.spinner.components.FilterTable;
import cn.github.spinner.components.RowNumberTableModel;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.editor.ui.dataview.details.ObjectDetailsWindow;
import cn.github.spinner.util.MQLUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class ObjectBrowserComponent extends JBPanel<ObjectBrowserComponent> {
    private static final String[] COLUMNS = {"Type", "Name", "Revision", "ID", "Path", "PhysicalID", "Description", "Originated", "Modified", "Vault", "Policy", "Owner", "State", "Organization", "Collaborative Space"};
    private static final int[] COLUMN_WIDTHS = {180, 240, 120, 240, 60, 300, 240, 240, 240, 120, 240, 120, 120, 180, 240};
    private final Project project;
    private final VirtualFile virtualFile;
    private List<String> typeList = Collections.emptyList();
    private List<String> policyList = Collections.emptyList();
    private List<String> ownerList = Collections.emptyList();
    private List<String> organizationList = Collections.emptyList();
    private List<String> projectList = Collections.emptyList();
    private JBTextField nameTextField;
    private JBTextField revisionTextField;
    private JBTextField idTextField;
    private JBTextField physicalIdTextField;
    private ComboBoxWithFilter<String> typeComboBox;
    private ComboBoxWithFilter<String> ownerComboBox;
    private ComboBoxWithFilter<String> projectComboBox;
    private ComboBoxWithFilter<String> organizationComboBox;
    private ComboBoxWithFilter<String> policyComboBox;
    private ComboBoxWithFilter<String> stateComboBox;
    private JBTextArea whereClauseTextArea;
    private JButton queryBtn;
    private JButton resetBtn;
    protected FilterTable table;
    protected RowNumberTableModel tableModel;

    public ObjectBrowserComponent(@NotNull Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        loadMatrixData();
        initComponents();
        setupListener();
        setupLayout();
    }

    private void initComponents() {
        typeComboBox = new ComboBoxWithFilter<>(typeList, "*");
        policyComboBox = new ComboBoxWithFilter<>(policyList, "*");
        stateComboBox = new ComboBoxWithFilter<>(Collections.emptyList(), "*");
        organizationComboBox = new ComboBoxWithFilter<>(organizationList, "*");
        projectComboBox = new ComboBoxWithFilter<>(projectList, "*");
        ownerComboBox = new ComboBoxWithFilter<>(ownerList, "*");
        nameTextField = new JBTextField("*");
        revisionTextField = new JBTextField("*");
        idTextField = new JBTextField("");
        physicalIdTextField = new JBTextField("");
        whereClauseTextArea = new JBTextArea(3, 0);
        queryBtn = new JButton("Query");
        resetBtn = new JButton("Reset");
        tableModel = new RowNumberTableModel(COLUMNS, 0);
        table = new FilterTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        nameTextField.setPreferredSize(JBUI.size(300, 30));
        revisionTextField.setPreferredSize(JBUI.size(300, 30));
        idTextField.setPreferredSize(JBUI.size(300, 30));
        physicalIdTextField.setPreferredSize(JBUI.size(300, 30));
        typeComboBox.setPreferredSize(JBUI.size(300, 30));
        ownerComboBox.setPreferredSize(JBUI.size(300, 30));
        policyComboBox.setPreferredSize(JBUI.size(300, 30));
        stateComboBox.setPreferredSize(JBUI.size(300, 30));
        organizationComboBox.setPreferredSize(JBUI.size(300, 30));
        projectComboBox.setPreferredSize(JBUI.size(300, 30));
        whereClauseTextArea.setLineWrap(true); // 自动换行
        whereClauseTextArea.setBorder(JBUI.Borders.customLine(JBColor.LIGHT_GRAY));
        whereClauseTextArea.setMargin(JBUI.insets(4));
        queryBtn.setPreferredSize(new Dimension(80, 30));
        resetBtn.setPreferredSize(new Dimension(80, 30));
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        for (var i = 1; i < COLUMN_WIDTHS.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(COLUMN_WIDTHS[i - 1]);
        }
    }

    private void setupListener() {
        nameTextField.addKeyListener(new EnterPressListener());
        revisionTextField.addKeyListener(new EnterPressListener());
        idTextField.addKeyListener(new EnterPressListener());
        physicalIdTextField.addKeyListener(new EnterPressListener());
        typeComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        ownerComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        policyComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        stateComboBox.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                new Task.Backgroundable(project, "Load Policy State") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        String item = policyComboBox.getItem();
                        if (CharSequenceUtil.isNotBlank(item) && !"*".equals(item)) {
                            try {
                                String result = MQLUtil.execute(project, "print policy '{}' select state dump", item);
                                if (CharSequenceUtil.isNotBlank(result)) {
                                    SwingUtilities.invokeLater(() -> {
                                        stateComboBox.removeAllItems();
                                        stateComboBox.setItem("*");
                                        String[] array = result.split(",");
                                        for (String s : array) {
                                            stateComboBox.addItem(s);
                                        }
                                    });
                                }
                            } catch (MQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            SwingUtilities.invokeLater(() -> stateComboBox.removeAllItems());
                        }
                    }
                }.queue();
            }
        });
        stateComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        organizationComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        projectComboBox.getEditor().getEditorComponent().addKeyListener(new EnterPressListener());
        whereClauseTextArea.addKeyListener(new EnterPressListener());
        // 查询按钮点击事件
        queryBtn.addActionListener(e -> handleQuery());
        // 重置按钮点击事件
        resetBtn.addActionListener(e -> handleReset());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = table.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0) return;

                        String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 4));
                        ObjectDetailsWindow.showWindow(project, id);
                    }
                }
            }
        });
    }

    private void setupLayout() {
        var conditionPanel = new JPanel();
        conditionPanel.setLayout(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        // 第0行：Type + Name + Revision
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weighty = 0; // 垂直不拉伸
        conditionPanel.add(new JBLabel("Type"), gbc);
        gbc.gridx = 1;
        conditionPanel.add(typeComboBox, gbc);
        gbc.gridx = 2;
        conditionPanel.add(new JBLabel("Name"), gbc);
        gbc.gridx = 3;
        conditionPanel.add(nameTextField, gbc);
        gbc.gridx = 4;
        conditionPanel.add(new JBLabel("Revision"), gbc);
        gbc.gridx = 5;
        conditionPanel.add(revisionTextField, gbc);
        // 第1行：ID + Physical ID
        gbc.gridx = 0;
        gbc.gridy = 1;
        conditionPanel.add(new JBLabel("ID"), gbc);
        gbc.gridx = 1;
        conditionPanel.add(idTextField, gbc);
        gbc.gridx = 2;
        conditionPanel.add(new JBLabel("Physical ID"), gbc);
        gbc.gridx = 3;
        conditionPanel.add(physicalIdTextField, gbc);
        // 第2行：Policy + State
        gbc.gridx = 0;
        gbc.gridy = 2;
        conditionPanel.add(new JBLabel("Policy"), gbc);
        gbc.gridx = 1;
        conditionPanel.add(policyComboBox, gbc);
        gbc.gridx = 2;
        conditionPanel.add(new JBLabel("State"), gbc);
        gbc.gridx = 3;
        conditionPanel.add(stateComboBox, gbc);
        // 第3行：Organization + Collaborative Space + Owner
        gbc.gridx = 0;
        gbc.gridy = 3;
        conditionPanel.add(new JBLabel("Organization"), gbc);
        gbc.gridx = 1;
        conditionPanel.add(organizationComboBox, gbc);
        gbc.gridx = 2;
        conditionPanel.add(new JBLabel("Collaborative Space"), gbc);
        gbc.gridx = 3;
        conditionPanel.add(projectComboBox, gbc);
        gbc.gridx = 4;
        conditionPanel.add(new JBLabel("Owner"), gbc);
        gbc.gridx = 5;
        conditionPanel.add(ownerComboBox, gbc);
        // 第4行：Where Expression + 文本域
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        conditionPanel.add(new JBLabel("Where Expression"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 5;
        gbc.weighty = 1.0; // 垂直拉伸
        gbc.fill = GridBagConstraints.BOTH; // 水平+垂直填充
        var textAreaScroll = ScrollPaneFactory.createScrollPane(whereClauseTextArea);
        textAreaScroll.setPreferredSize(JBUI.size(-1, 100));
        conditionPanel.add(textAreaScroll, gbc);
        // 第5行：查询/重置按钮
        gbc.gridx = 5;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(queryBtn);
        buttonPanel.add(resetBtn);
        conditionPanel.add(buttonPanel, gbc);
        conditionPanel.setPreferredSize(JBUI.size(-1, 400));
        setLayout(new BorderLayout());
        add(conditionPanel, BorderLayout.NORTH);

        var scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadMatrixData() {
        var connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null) return;

        try {
            var statement = connection.executeStatement("list type");
            var resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                typeList = new ArrayList<>(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            }
            statement = connection.executeStatement("list person");
            resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                ownerList = new ArrayList<>(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            }
            statement = connection.executeStatement("list policy");
            resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                policyList = new ArrayList<>(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            }
            statement = connection.executeStatement("temp query bus PnOProject * * select name dump \001 recordsep \002");
            resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                List<String> recordList = CharSequenceUtil.split(resultSet.getResult(), "\002");
                List<String> allData = recordList.stream().filter(CharSequenceUtil::isNotBlank).map(str -> str.split("\001")[1]).toList();
                projectList = new ArrayList<>(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            }
            statement = connection.executeStatement("temp query bus Company * * select name dump \001 recordsep \002");
            resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                List<String> recordList = CharSequenceUtil.split(resultSet.getResult(), "\002");
                List<String> allData = recordList.stream().filter(CharSequenceUtil::isNotBlank).map(str -> str.split("\001")[1]).toList();
                organizationList = new ArrayList<>(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
            }
        } catch (MQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String buildWhereExpression() {
        var builder = new StringBuilder();
        if (CharSequenceUtil.isNotBlank(idTextField.getText())) {
            builder.append("id == '").append(idTextField.getText()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(physicalIdTextField.getText())) {
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("physicalid == '").append(physicalIdTextField.getText()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(policyComboBox.getItem()) && !"*".equalsIgnoreCase(policyComboBox.getItem())) {
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("policy == '").append(policyComboBox.getItem()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(stateComboBox.getItem()) && !"*".equalsIgnoreCase(stateComboBox.getItem())) {
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("current == '").append(stateComboBox.getItem()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(organizationComboBox.getItem()) && !"*".equalsIgnoreCase(organizationComboBox.getItem())) {
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("organization == '").append(organizationComboBox.getItem()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(projectComboBox.getItem()) && !"*".equalsIgnoreCase(projectComboBox.getItem())) {
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("project == '").append(projectComboBox.getItem()).append("'");
        }
        if (CharSequenceUtil.isNotBlank(whereClauseTextArea.getText())) {
            if (!builder.isEmpty()) {
                builder.insert(0, "(").append(")");
            }
            builder.append(builder.isEmpty() ? "" : " && ");
            builder.append("(").append(whereClauseTextArea.getText()).append(")");
        }
        if (!builder.isEmpty()) {
            builder.insert(0, "(").append(")");
        }
        return builder.toString();
    }

    private void handleQuery() {
        if (emptyExpression()) {
            UIUtil.showWarningNotification(project, "Search from Server", "Conditional is empty");
            return;
        }
        tableModel.setRowCount(0);
        table.getEmptyText().setText("Searching from server ......");
        final var typePattern = CharSequenceUtil.isBlank(typeComboBox.getItem()) ? "*" : typeComboBox.getItem();
        final var namePattern = CharSequenceUtil.isBlank(nameTextField.getText()) ? "*" : nameTextField.getText();
        final var revisionPattern = CharSequenceUtil.isBlank(revisionTextField.getText()) ? "*" : revisionTextField.getText();
        final var ownerPattern = CharSequenceUtil.isBlank(ownerComboBox.getItem()) ? "*" : ownerComboBox.getItem();
        final var whereExpression = buildWhereExpression();
        new Task.Backgroundable(project, "Search from Server") {
            private List<Object[]> dataList = Collections.emptyList();
            private Throwable error;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                var objectQuery = new MatrixObjectQuery();
                objectQuery.setName(namePattern);
                objectQuery.setType(typePattern);
                objectQuery.setRevision(revisionPattern);
                objectQuery.setOwner(ownerPattern);
                objectQuery.setWhereExpression(whereExpression);
                try {
                    var connection = SpinnerToken.getCurrentConnection(project);
                    if (connection == null) throw new MQLException("connection is closed");

                    MatrixQueryResult queryResult = connection.queryObject(objectQuery, List.of("type", "name", "revision", "id", "paths", "physicalid", "description", "originated", "modified", "lattice", "policy", "owner", "current", "organization", "project"));
                    dataList = new ArrayList<>();
                    if (!queryResult.isEmpty()) {
                        for (Map<String, String> map : queryResult.getData()) {
                            Object[] row = new Object[17];
                            row[0] = MapUtil.getStr(map, "type");
                            row[1] = MapUtil.getStr(map, "name");
                            row[2] = MapUtil.getStr(map, "revision");
                            row[3] = MapUtil.getStr(map, "id");
                            String path = MapUtil.getStr(map, "paths");
                            row[4] = MapUtil.getStr(map, CharSequenceUtil.isNotBlank(path) && !path.equalsIgnoreCase("FALSE"));
                            row[5] = MapUtil.getStr(map, "physicalid");
                            row[6] = MapUtil.getStr(map, "description");
                            row[7] = MapUtil.getStr(map, "originated");
                            row[8] = MapUtil.getStr(map, "modified");
                            row[9] = MapUtil.getStr(map, "lattice");
                            row[10] = MapUtil.getStr(map, "policy");
                            row[11] = MapUtil.getStr(map, "owner");
                            row[12] = MapUtil.getStr(map, "current");
                            row[13] = MapUtil.getStr(map, "organization");
                            row[14] = MapUtil.getStr(map, "project");
                            dataList.add(row);
                        }
                    }
                } catch (MQLException e) {
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (error != null) {
                    log.error(error.getLocalizedMessage(), error);
                    UIUtil.showErrorNotification(project, "Search from Server", error.getLocalizedMessage());
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    SwingUtilities.invokeLater(() -> {
                        for (Object[] rowValues : dataList) {
                            tableModel.addRow(rowValues);
                        }
                    });
                    table.getEmptyText().setText("Nothing to show");
                });
            }
        }.queue();
    }

    private void handleReset() {
        typeComboBox.setItem("*");
        policyComboBox.setItem("*");
        stateComboBox.setItem("*");
        organizationComboBox.setItem("*");
        projectComboBox.setItem("*");
        ownerComboBox.setItem("*");
        nameTextField.setText("*");
        revisionTextField.setText("*");
        idTextField.setText("");
        physicalIdTextField.setText("");
    }

    private boolean emptyExpression() {
        return (CharSequenceUtil.isBlank(typeComboBox.getItem()) || "*".equals(typeComboBox.getItem()))
                && (CharSequenceUtil.isBlank(policyComboBox.getItem()) || "*".equals(policyComboBox.getItem()))
                && (CharSequenceUtil.isBlank(stateComboBox.getItem()) || "*".equals(stateComboBox.getItem()))
                && (CharSequenceUtil.isBlank(organizationComboBox.getItem()) || "*".equals(organizationComboBox.getItem()))
                && (CharSequenceUtil.isBlank(projectComboBox.getItem()) || "*".equals(projectComboBox.getItem()))
                && (CharSequenceUtil.isBlank(ownerComboBox.getItem()) || "*".equals(ownerComboBox.getItem()))
                && (CharSequenceUtil.isBlank(nameTextField.getText()) || "*".equals(nameTextField.getText()))
                && (CharSequenceUtil.isBlank(revisionTextField.getText()) || "*".equals(revisionTextField.getText()))
                && CharSequenceUtil.isBlank(idTextField.getText())
                && CharSequenceUtil.isBlank(physicalIdTextField.getText());
    }

    private class EnterPressListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                handleQuery();
            }
        }
    }
}
