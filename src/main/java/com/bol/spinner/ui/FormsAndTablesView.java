package com.bol.spinner.ui;

import cn.github.driver.MQLException;
import com.bol.spinner.util.MQLUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormsAndTablesView extends JBPanel {
    private final Project myProject;
    private final VirtualFile myFile;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JBList<String> itemList = new JBList<>(listModel);
    private final JTextPane contentPane = new JTextPane();
    private final JTextField filterField = new JTextField(20);
    private final JCheckBox formsCb = new JCheckBox("Forms", true);
    private final JCheckBox tablesCb = new JCheckBox("Tables", true);
    private final List<Item> allItems = new ArrayList<>();
    private final static Pattern pattern = Pattern.compile("\\((.*?)\\)");
    // 高亮样式
    private final MutableAttributeSet normalAttr;
    private final MutableAttributeSet keywordAttr;
    private final MutableAttributeSet stringAttr;
    private final MutableAttributeSet numberAttr;

    static class Item {
        String name;
        String type;

        Item(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    public FormsAndTablesView(Project project, VirtualFile file) {
        myProject = project;
        myFile = file;
        // 初始化高亮样式
        normalAttr = new SimpleAttributeSet();
        keywordAttr = new SimpleAttributeSet();
        stringAttr = new SimpleAttributeSet();
        numberAttr = new SimpleAttributeSet();

        // 设置样式属性
        StyleConstants.setForeground(normalAttr, JBColor.foreground());
        StyleConstants.setForeground(keywordAttr, new JBColor(new Color(86, 156, 214), new Color(78, 148, 206)));
        StyleConstants.setForeground(stringAttr, new JBColor(new Color(79, 163, 49), new Color(126, 198, 153)));
        StyleConstants.setForeground(numberAttr, new JBColor(new Color(61, 113, 26), new Color(184, 215, 163)));

        // 设置关键字粗体
        StyleConstants.setBold(keywordAttr, true);
        setLayout(new BorderLayout(10, 10));
        setBorder(JBUI.Borders.empty(8));
        initComponents();
        loadItemsAsync();
        filterItems();
        setupListeners();
    }

    private void initComponents() {
        // 顶部：过滤 + 查找区域
        JBPanel topPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.add(new JBLabel("Filter:"));
        topPanel.add(filterField);
        topPanel.add(Box.createHorizontalStrut(10));
        add(topPanel, BorderLayout.NORTH);
        // 中间：左侧列表 + 右侧内容区
        JBPanel centerPanel = new JBPanel<>(new BorderLayout());

        // 左侧列表
        JBScrollPane listScrollPane = new JBScrollPane(itemList);
        listScrollPane.setPreferredSize(new Dimension(300, 400));

        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    setBackground(new JBColor(new Color(63, 127, 191), new Color(63, 127, 191)));
                    setForeground(JBColor.WHITE);
                }
                return this;
            }
        });

        // 右侧内容区域
        contentPane.setEditable(false);
        contentPane.setContentType("text/plain");

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        contentPane.setFont(scheme.getFont(EditorFontType.PLAIN));
        JBScrollPane contentScrollPane = new JBScrollPane(contentPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, contentScrollPane);
        splitPane.setDividerLocation(300);
        splitPane.setEnabled(true);
        splitPane.setDividerSize(3);

        centerPanel.add(splitPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 底部：Forms/Tables 类型筛选
        JBPanel bottomPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomPanel.add(formsCb);
        bottomPanel.add(tablesCb);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadItemsAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String[] tableArray = MQLUtil.execute("list table system").split("\n");
                for (String tableName : tableArray) {
                    allItems.add(new Item(tableName, "Table"));
                }
                String[] formArray = MQLUtil.execute("list form").split("\n");
                for (String formName : formArray) {
                    allItems.add(new Item(formName, "Form"));
                }
                ApplicationManager.getApplication().invokeLater(this::filterItems);
            } catch (MQLException e) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog(myProject, "Failed to load items: " + e.getMessage(), "Data Load Error"));
            }
        });
    }

    private void filterItems() {
        listModel.clear();
        String filterText = filterField.getText().toLowerCase().trim();
        boolean showForms = formsCb.isSelected();
        boolean showTables = tablesCb.isSelected();

        for (Item item : allItems) {
            boolean typeMatch = (showForms && "Form".equals(item.type)) || (showTables && "Table".equals(item.type));
            boolean nameMatch = item.name.toLowerCase().contains(filterText);
            if (typeMatch && nameMatch) {
                listModel.addElement(item.name + " (" + item.type + ")");
            }
        }
    }

    private void setupListeners() {
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterItems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterItems();
            }
        });

        formsCb.addActionListener(e -> filterItems());
        tablesCb.addActionListener(e -> filterItems());
        itemList.addListSelectionListener(this::onListSelectionChanged);
    }

    private void onListSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int selectedIndex = itemList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedText = itemList.getSelectedValue();
                int bracketIndex = selectedText.indexOf(" (");
                if (bracketIndex == -1) return;
                String formOrTableName = selectedText.substring(0, bracketIndex);
                Matcher typeMatch = pattern.matcher(selectedText);
                if (typeMatch.find()) {
                    String type = typeMatch.group(1);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            String content = "";
                            if (type.equals("Form")) {
                                content = MQLUtil.execute("print {} {}", type, formOrTableName);
                            } else if (type.equals("Table")) {
                                content = MQLUtil.execute("print {} {} system", type, formOrTableName);
                            }
                            if (content.startsWith("\n")) {
                                content = content.substring(1);
                                content = content.replace("\n\n\n", "\n\n").replace("\n    ", "\n\t");
                            }
                            String finalContent = content;
                            ApplicationManager.getApplication().invokeLater(() -> {
                                applySyntaxHighlighting(finalContent);
                                contentPane.setCaretPosition(0);
                            });
                        } catch (MQLException ex) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showErrorDialog(myProject, "Failed to load content: " + ex.getMessage(), "Content Error"));
                        }
                    });
                }
            }
        }
    }


    private void applySyntaxHighlighting(String content) {
        StyledDocument doc = contentPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, content, normalAttr);
            // 定义高亮关键字
            String[] keywords = {
                    "form", "web", "field", "expressiontype",
                    "multiline", "edit", "label", "range",
                    "setting", "value", "nothidden", "property", "created", "modified",
                    "range", "businessobject", "autoheight", "autowidth", "editable", "hidden",
                    "href", "sorttype", "true", "false", "table", "column", "set", "name","user","description","inactive"
            };

            for (String keyword : keywords) {
                highlightPattern("\\b" + Pattern.quote(keyword) + "\\b", keywordAttr, content);
            }
            // 高亮字符串
            highlightPattern("\"[^\"]*\"", stringAttr, content);
            highlightPattern("'[^']*'", stringAttr, content);
            // 高亮数字
            highlightPattern("\\b\\d+\\b", numberAttr, content);
            highlightPattern("\\b(true|false)\\b", numberAttr, content);
        } catch (BadLocationException ex) {
            // 忽略错误
        }
    }

    private void highlightPattern(String patternStr, AttributeSet attr, String content) {
        try {
            Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(content);
            StyledDocument doc = contentPane.getStyledDocument();
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        } catch (Exception ex) {
            // 忽略错误
        }
    }

}