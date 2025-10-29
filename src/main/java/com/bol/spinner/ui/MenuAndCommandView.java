package com.bol.spinner.ui;

import cn.github.driver.MQLException;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.bol.spinner.ui.bean.MenuCommandNode;
import com.bol.spinner.util.MQLUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MenuAndCommandView extends BorderLayoutPanel {
    private final Project project;
    private Tree menuTree;
    private DefaultTreeModel treeModel;
    private JBList<MenuCommandNode> commandList;
    private DefaultListModel<MenuCommandNode> listModel;
    private JBTextField descriptionField, labelField, altField, codeField;
    private JBTextArea hrefField;
    private JBTextField rootInputField;
    private JBCheckBox commandCheckBox;
    private JBCheckBox menuCheckBox;
    private JBCheckBox channelCheckBox;
    private JBCheckBox portalCheckBox;
    private final List<MenuCommandNode> commandsList = new ArrayList<>();
    private final List<MenuCommandNode> menuList = new ArrayList<>();
    private final List<MenuCommandNode> channelList = new ArrayList<>();
    private final List<MenuCommandNode> portalList = new ArrayList<>();
    // 维护完整列表数据用于过滤
    private List<MenuCommandNode> allListData = new ArrayList<>();
    private JBTextField filterField;
    private static final String ROOT = "AEFGlobalToolbar";
    private DefaultTableModel settingsTableModel;

    private static class CustomTreeNode extends DefaultMutableTreeNode {
        public CustomTreeNode(MenuCommandNode userObject) {
            super(userObject);
        }

        @Override
        public boolean isLeaf() {
            MenuCommandNode node = (MenuCommandNode) getUserObject();
            return "Command".equals(node.getType());
        }
    }

    public MenuAndCommandView(Project project) {
        super();
        this.project = project;
        setBorder(JBUI.Borders.empty(10));
        initComponents();
    }

    private void initComponents() {
        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JComponent createTopPanel() {
        JBPanel panel = new JBPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(JBUI.Borders.customLineBottom(JBColor.LIGHT_GRAY));

        JButton setRootButton = new JButton("Set Root");
        setRootButton.setPreferredSize(new Dimension(80, 30));
        setRootButton.addActionListener(e -> {
            String searchText = rootInputField.getText().trim();
            searchAndUpdateTree(searchText);
        });
        panel.add(setRootButton);

        rootInputField = new JBTextField();
        rootInputField.setPreferredSize(new Dimension(150, 30));
        rootInputField.setText("AFGGlobalToolbar");
        panel.add(rootInputField);

        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JBLabel("Filter:"));

        filterField = new JBTextField();
        filterField.setPreferredSize(new Dimension(200, 30));
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList(filterField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList(filterField.getText().trim());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList(filterField.getText().trim());
            }
        });
        panel.add(filterField);
        panel.add(Box.createHorizontalStrut(10));

        JButton resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(80, 30));
        resetButton.addActionListener(e -> resetToDefaultRoot());
        panel.add(resetButton);

        return panel;
    }

    private JComponent createCenterPanel() {
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(0.25);
        mainSplitPane.setResizeWeight(0.25);
        mainSplitPane.setDividerSize(6);

        JSplitPane midRightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        midRightSplitPane.setDividerLocation(0.35);
        midRightSplitPane.setResizeWeight(0.35);
        midRightSplitPane.setDividerSize(6);
        midRightSplitPane.setLeftComponent(createListPanel());
        midRightSplitPane.setRightComponent(createDetailPanel());

        mainSplitPane.setRightComponent(midRightSplitPane);
        mainSplitPane.setLeftComponent(createTreePanel());
        return mainSplitPane;
    }


    private JComponent createTreePanel() {
        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
                BorderFactory.createTitledBorder("Menu Structure"),
                JBUI.Borders.empty(5)
        ));

        MenuCommandNode root = new MenuCommandNode(ROOT, "Menu");
        DefaultMutableTreeNode treeRoot = new CustomTreeNode(root);
        List<MenuCommandNode> rootChildren = getMenuChildren(root);
        for (MenuCommandNode item : rootChildren) {
            treeRoot.add(new CustomTreeNode(item));
        }
        treeModel = new DefaultTreeModel(treeRoot);
        menuTree = new Tree(treeModel);
        menuTree.setShowsRootHandles(true);
        menuTree.setRootVisible(true);
        menuTree.setCellRenderer(new ColoredTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded,
                                              boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                MenuCommandNode menuNode = (MenuCommandNode) node.getUserObject();
                String text = menuNode.toString();
                append(text);

                if ("Menu".equals(menuNode.getType())) {
                    setIcon(expanded ? AllIcons.Nodes.NativeLibrariesFolder : AllIcons.Nodes.Folder);
                } else {
                    setIcon(AllIcons.Nodes.Property);
                }
                if (selected) {
                    setBackground(JBColor.background());
                    setForeground(JBColor.foreground());
                } else {
                    setBackground(JBColor.WHITE);
                    setForeground(JBColor.BLACK);
                }
            }
        });

        menuTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreePath path = event.getPath();
                CustomTreeNode currentNode = (CustomTreeNode) path.getLastPathComponent();
                MenuCommandNode currentData = (MenuCommandNode) currentNode.getUserObject();
                if (currentData.isMenu() && currentNode.getChildCount() == 0) {
                    List<MenuCommandNode> menuChildren = getMenuChildren(currentData);
                    for (MenuCommandNode childData : menuChildren) {
                        CustomTreeNode childNode = new CustomTreeNode(childData);
                        currentNode.add(childNode);
                    }
                    treeModel.reload(currentNode);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });

        menuTree.addTreeSelectionListener(e -> {
            TreePath selectedPath = e.getNewLeadSelectionPath();
            if (selectedPath == null) return;

            CustomTreeNode currentNode = (CustomTreeNode) selectedPath.getLastPathComponent();
            MenuCommandNode nodeData = (MenuCommandNode) currentNode.getUserObject();
            setMidArea(nodeData);
            locateAndSelectInList(nodeData);
        });

        menuTree.expandRow(0);
        JBScrollPane scrollPane = new JBScrollPane(menuTree);
        scrollPane.setBorder(JBUI.Borders.empty());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 在中间列表中定位并选中与树节点匹配的元素
     */
    private void locateAndSelectInList(MenuCommandNode targetNode) {
        if (targetNode == null || listModel.isEmpty()) {
            commandList.clearSelection();
            return;
        }

        String targetName = targetNode.getName().trim();
        String targetType = targetNode.getType();

        SwingUtilities.invokeLater(() -> {
            boolean found = false;
            for (int i = 0; i < listModel.size(); i++) {
                MenuCommandNode listNode = listModel.getElementAt(i);
                if (listNode == null) continue;

                boolean nameMatch = listNode.getName().trim().equalsIgnoreCase(targetName);
                boolean typeMatch = listNode.getType().equals(targetType);

                if (nameMatch && typeMatch) {
                    commandList.setSelectedIndex(i);
                    commandList.ensureIndexIsVisible(i); // 滚动到可视区域
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.debug("未在列表中找到匹配项：{}({})", targetName, targetType);
                commandList.clearSelection();
            }
        });
    }

    public void setMidArea(MenuCommandNode menuCommandNode) {
        menuCommandNode.setInfo(project);
        descriptionField.setText(menuCommandNode.getDescription());
        labelField.setText(menuCommandNode.getLabel());
        hrefField.setText(menuCommandNode.getHref() != null ? menuCommandNode.getHref().replace("&", "\n&    ") : "");
        altField.setText(menuCommandNode.getAlt());
        codeField.setText(StrUtil.EMPTY);
        updateSettingsTable(menuCommandNode.getSetting());
    }

    public List<MenuCommandNode> getMenuChildren(MenuCommandNode menuCommandNode) {
        List<MenuCommandNode> rootChildren = new ArrayList<>();
        if (StrUtil.isNotEmpty(menuCommandNode.getName())) {
            try {
                String[] menuArray = MQLUtil.execute(project, "print menu '{}' select menu dump", menuCommandNode.getName()).split(StrPool.COMMA);
                for (String menuName : menuArray) {
                    if (StrUtil.isNotEmpty(menuName)) {
                        rootChildren.add(new MenuCommandNode(menuName.trim(), "Menu"));
                    }
                }
                String[] commandArray = MQLUtil.execute(project, "print menu '{}' select command dump", menuCommandNode.getName()).split(StrPool.COMMA);
                for (String commandName : commandArray) {
                    if (StrUtil.isNotEmpty(commandName)) {
                        rootChildren.add(new MenuCommandNode(commandName.trim(), "Command"));
                    }
                }
            } catch (MQLException e) {
                log.error("获取子节点失败", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "获取子节点失败: " + e.getMessage())
                );
            }
        }
        return rootChildren;
    }

    private JComponent createListPanel() {
        JBPanel panel = new JBPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
                BorderFactory.createTitledBorder("Commands"),
                JBUI.Borders.empty(5)
        ));
        listModel = new DefaultListModel<>();
        commandList = new JBList<>(listModel);
        commandList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        commandList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && commandList.getSelectedValue() != null) {
                setMidArea(commandList.getSelectedValue());
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(commandList);
        scrollPane.setBorder(JBUI.Borders.empty());
        JBLabel emptyLabel = new JBLabel("请从左侧选择菜单节点或勾选底部类型筛选");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setForeground(JBColor.GRAY);
        panel.add(emptyLabel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createDetailPanel() {
        JBPanel panel = new JBPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(10));

        panel.add(createLabeledField("Description", 400));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabeledField("Label", 400));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabeledField("Href", 400));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabeledField("Alt", 400));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createLabeledField("Code", 400));
        panel.add(Box.createVerticalStrut(15));

        panel.add(createSettingsSection());
        panel.add(Box.createVerticalStrut(15));

//        panel.add(createAccessForSection());
//        panel.add(Box.createVerticalStrut(15));
//        panel.add(createRoleSection());
//        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JComponent createLabeledField(String labelText, int width) {
        JBPanel panel = new JBPanel(new BorderLayout(10, 0));

        JBLabel label = new JBLabel(labelText + ":");
        label.setPreferredSize(new Dimension(80, 30));
        panel.add(label, BorderLayout.WEST);

        if ("Href".equals(labelText)) {
            hrefField = new JBTextArea();
            hrefField.setFont(UIManager.getFont("TextField.font"));
            hrefField.setLineWrap(true);
            hrefField.setWrapStyleWord(true);
            hrefField.setPreferredSize(new Dimension(width, 200));
            JBScrollPane scrollPane = new JBScrollPane(hrefField);
            scrollPane.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            panel.add(scrollPane, BorderLayout.CENTER);
        } else {
            JBTextField textField = new JBTextField();
            textField.setPreferredSize(new Dimension(width, 30));
            panel.add(textField, BorderLayout.CENTER);

            switch (labelText) {
                case "Description":
                    descriptionField = textField;
                    break;
                case "Label":
                    labelField = textField;
                    break;
                case "Alt":
                    altField = textField;
                    break;
                case "Code":
                    codeField = textField;
                    break;
            }
        }

        return panel;
    }

    private JComponent createSettingsSection() {
        JBPanel panel = new JBPanel(new BorderLayout());
        JBLabel label = new JBLabel("Settings");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);
        settingsTableModel = new DefaultTableModel(new Object[][]{}, new Object[]{"Key", "Value"});
        JBTable table = new JBTable(settingsTableModel);
        table.setPreferredScrollableViewportSize(new Dimension(400, 100));
        table.setShowGrid(true);
        table.setGridColor(JBColor.LIGHT_GRAY);
        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void updateSettingsTable(Map<String, String> settings) {
        settingsTableModel.setRowCount(0);
        if (settings != null) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                settingsTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }

    private JComponent createAccessForSection() {
        JBPanel panel = new JBPanel(new BorderLayout(10, 0));
        JBLabel label = new JBLabel("Access for:");
        label.setPreferredSize(new Dimension(80, 30));
        panel.add(label, BorderLayout.WEST);
        ComboBox<String> accessCombo = new ComboBox<>(new String[]{"admin_platform", "user_platform", "guest"});
        accessCombo.setPreferredSize(new Dimension(200, 30));
        panel.add(accessCombo, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createRoleSection() {
        JBPanel panel = new JBPanel(new BorderLayout());
        JBLabel label = new JBLabel("Role");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{{"Admin", "Full Access"}, {"Editor", "Modify Access"}, {"Viewer", "Read Only"}},
                new Object[]{"Role", "Access"}
        );
        JBTable table = new JBTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(400, 80));
        table.setShowGrid(true);
        table.setGridColor(JBColor.LIGHT_GRAY);

        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createBottomPanel() {
        JBPanel panel = new JBPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        panel.setBorder(JBUI.Borders.customLineTop(JBColor.LIGHT_GRAY));

        commandCheckBox = new JBCheckBox("Commands", false);
        menuCheckBox = new JBCheckBox("Menus", false);
        channelCheckBox = new JBCheckBox("Channels", false);
        portalCheckBox = new JBCheckBox("Portals", false);

        commandCheckBox.addActionListener(this::createActionListener);
        menuCheckBox.addActionListener(this::createActionListener);
        channelCheckBox.addActionListener(this::createActionListener);
        portalCheckBox.addActionListener(this::createActionListener);

        panel.add(commandCheckBox);
        panel.add(menuCheckBox);
        panel.add(channelCheckBox);
        panel.add(portalCheckBox);
        return panel;
    }

    public void createActionListener(ActionEvent actionEvent) {
        runAsync(() -> {
            try {
                String actionCommand = actionEvent.getActionCommand();
                switch (actionCommand) {
                    case "Commands" -> {
                        if (commandCheckBox.isSelected() && commandsList.isEmpty()) {
                            for (String command : MQLUtil.execute(project, "list command").split("\n")) {
                                if (StrUtil.isNotEmpty(command.trim())) {
                                    commandsList.add(new MenuCommandNode(command.trim(), "Command"));
                                }
                            }
                        } else if (!commandCheckBox.isSelected()) {
                            commandsList.clear();
                        }
                    }
                    case "Menus" -> {
                        if (menuCheckBox.isSelected() && menuList.isEmpty()) {
                            for (String menu : MQLUtil.execute(project, "list menu").split("\n")) {
                                if (StrUtil.isNotEmpty(menu.trim())) {
                                    menuList.add(new MenuCommandNode(menu.trim(), "Menu"));
                                }
                            }
                        } else if (!menuCheckBox.isSelected()) {
                            menuList.clear();
                        }
                    }
                    case "Channels" -> {
                        if (channelCheckBox.isSelected() && channelList.isEmpty()) {
                            for (String channel : MQLUtil.execute(project, "list channel").split("\n")) {
                                if (StrUtil.isNotEmpty(channel.trim())) {
                                    channelList.add(new MenuCommandNode(channel.trim(), "Channel"));
                                }
                            }
                        } else if (!channelCheckBox.isSelected()) {
                            channelList.clear();
                        }
                    }
                    case "Portals" -> {
                        if (portalCheckBox.isSelected() && portalList.isEmpty()) {
                            for (String portal : MQLUtil.execute(project, "list portal").split("\n")) {
                                if (StrUtil.isNotEmpty(portal.trim())) {
                                    portalList.add(new MenuCommandNode(portal.trim(), "Portal"));
                                }
                            }
                        } else if (!portalCheckBox.isSelected()) {
                            portalList.clear();
                        }
                    }
                }
            } catch (MQLException e) {
                log.error("MQL execute error", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, "获取数据失败: " + e.getMessage())
                );
                return;
            }

            List<MenuCommandNode> resultList = new ArrayList<>();
            resultList.addAll(commandsList);
            resultList.addAll(menuList);
            resultList.addAll(channelList);
            resultList.addAll(portalList);
            allListData = resultList.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.getName().trim().isEmpty())
                    .distinct()
                    .sorted(Comparator.comparing(MenuCommandNode::getName))
                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> {
                filterField.setText("");
                filterList(""); // 刷新列表
            });
        });
    }


    private void runAsync(Runnable task) {
        ApplicationManager.getApplication().executeOnPooledThread(task);
    }

    private void filterList(String filterText) {
        listModel.clear();
        if (StrUtil.isEmpty(filterText)) {
            listModel.addAll(allListData);
        } else {
            String lowerFilter = filterText.toLowerCase();
            allListData.stream()
                    .filter(item -> item.getName().toLowerCase().contains(lowerFilter))
                    .forEach(listModel::addElement);
        }
    }

    private void searchAndUpdateTree(String searchText) {
        if (StrUtil.isEmpty(searchText)) {
            JOptionPane.showMessageDialog(this, "请输入搜索内容");
            return;
        }

        String target = "";
        String type = "";
        try {
            target = MQLUtil.execute(project, "print menu '{}' select name dump", searchText).trim();
            type = "Menu";
            if (StrUtil.isEmpty(target)) {
                throw new MQLException("Menu not found");
            }
        } catch (MQLException e) {
            try {
                target = MQLUtil.execute(project, "print command '{}' select name dump", searchText).trim();
                type = "Command";
                if (StrUtil.isEmpty(target)) {
                    throw new MQLException("Command not found");
                }
            } catch (MQLException ex) {
                JOptionPane.showMessageDialog(this, "未找到匹配的节点: " + searchText);
                return;
            }
        }

        if (StrUtil.isAllNotEmpty(target, type)) {
            MenuCommandNode menuCommandNode = new MenuCommandNode(target, type);
            menuCommandNode.setInfo(project);

            DefaultMutableTreeNode newRoot = new CustomTreeNode(menuCommandNode);
            if ("Menu".equals(type)) {
                List<MenuCommandNode> children = getMenuChildren(menuCommandNode);
                for (MenuCommandNode child : children) {
                    newRoot.add(new CustomTreeNode(child));
                }
            }

            treeModel.setRoot(newRoot);
            menuTree.expandRow(0);
            TreePath newPath = new TreePath(newRoot.getPath());
            menuTree.setSelectionPath(newPath);
        }
    }

    /**
     * 重置树到默认根节点
     */
    private void resetToDefaultRoot() {
        MenuCommandNode defaultRoot = new MenuCommandNode(ROOT, "Menu");
        DefaultMutableTreeNode treeRoot = new CustomTreeNode(defaultRoot);
        List<MenuCommandNode> rootChildren = getMenuChildren(defaultRoot);
        for (MenuCommandNode item : rootChildren) {
            treeRoot.add(new CustomTreeNode(item));
        }
        treeModel.setRoot(treeRoot);
        menuTree.expandRow(0);
        rootInputField.setText(ROOT);
        commandList.clearSelection();
    }

}