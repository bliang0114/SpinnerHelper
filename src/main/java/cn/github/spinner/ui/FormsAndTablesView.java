package cn.github.spinner.ui;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormsAndTablesView extends JBPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(FormsAndTablesView.class);
    private static final int SPLIT_PANE_DIVIDER_LOCATION = 300;
    private static final int LIST_PREFERRED_WIDTH = 300;
    private static final int LIST_PREFERRED_HEIGHT = 400;
    private static final String TYPE_PATTERN_REGEX = "\\((.*?)\\)";
    private static final Pattern TYPE_PATTERN = Pattern.compile(TYPE_PATTERN_REGEX);
    private static final Pattern KEYWORDS_PATTERN;
    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b|\\b(true|false)\\b");

    static {
        String[] keywords = {
                "form", "web", "field", "expressiontype", "multiline", "edit", "label",
                "range", "setting", "value", "nothidden", "property", "created", "modified",
                "businessobject", "autoheight", "autowidth", "editable", "hidden", "href",
                "sorttype", "true", "false", "table", "column", "set", "name", "user",
                "description", "inactive"
        };
        String keywordsRegex = "\\b(" + String.join("|", Arrays.stream(keywords).map(Pattern::quote).toArray(String[]::new)) + ")\\b";
        KEYWORDS_PATTERN = Pattern.compile(keywordsRegex, Pattern.CASE_INSENSITIVE);
    }

    private final Project myProject;
    private final VirtualFile myFile;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JBList<String> itemList = new JBList<>(listModel);
    private final JTextPane contentPane = new JTextPane();
    private final JTextField filterField = new JTextField(20);
    private final JCheckBox formsCb = new JCheckBox("Forms", true);
    private final JCheckBox tablesCb = new JCheckBox("Tables", true);
    private final List<Item> allItems = new ArrayList<>();
    private final List<Disposable> disposables = new ArrayList<>();
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
        normalAttr = new SimpleAttributeSet();
        keywordAttr = new SimpleAttributeSet();
        stringAttr = new SimpleAttributeSet();
        numberAttr = new SimpleAttributeSet();

        StyleConstants.setForeground(normalAttr, JBColor.foreground());
        StyleConstants.setForeground(keywordAttr, new JBColor(new Color(86, 156, 214), new Color(78, 148, 206)));
        StyleConstants.setForeground(stringAttr, new JBColor(new Color(79, 163, 49), new Color(126, 198, 153)));
        StyleConstants.setForeground(numberAttr, new JBColor(new Color(61, 113, 26), new Color(184, 215, 163)));
        StyleConstants.setBold(keywordAttr, true);

        setLayout(new BorderLayout(10, 10));
        setBorder(JBUI.Borders.empty(8));
        initComponents();
        loadItemsAsync();
        filterItems();
        setupListeners();
    }

    private void initComponents() {
        JBPanel topPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.add(new JBLabel("Filter:"));
        topPanel.add(filterField);
        topPanel.add(Box.createHorizontalStrut(10));
        add(topPanel, BorderLayout.NORTH);

        JBPanel centerPanel = new JBPanel<>(new BorderLayout());
        JBScrollPane listScrollPane = new JBScrollPane(itemList);
        listScrollPane.setPreferredSize(new Dimension(LIST_PREFERRED_WIDTH, LIST_PREFERRED_HEIGHT));

        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    setBackground(UIUtil.getListSelectionBackground());
                    setForeground(UIUtil.getListSelectionForeground());
                }
                return this;
            }
        });

        contentPane.setEditable(false);
        contentPane.setContentType("text/plain");
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        contentPane.setFont(scheme.getFont(EditorFontType.PLAIN));
        JBScrollPane contentScrollPane = new JBScrollPane(contentPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, contentScrollPane);
        splitPane.setDividerLocation(SPLIT_PANE_DIVIDER_LOCATION);
        splitPane.setEnabled(true);
        splitPane.setDividerSize(3);

        centerPanel.add(splitPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JBPanel bottomPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bottomPanel.add(formsCb);
        bottomPanel.add(tablesCb);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadItemsAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String[] tableArray = MQLUtil.execute(myProject, "list table system").split("\n");
                for (String tableName : tableArray) {
                    allItems.add(new Item(tableName, "Table"));
                }
                String[] formArray = MQLUtil.execute(myProject, "list form").split("\n");
                for (String formName : formArray) {
                    allItems.add(new Item(formName, "Form"));
                }
                ApplicationManager.getApplication().invokeLater(this::filterItems);
            } catch (MQLException e) {
                LOG.error("Failed to load items", e);
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
        DocumentListener filterListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterItems(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterItems(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterItems(); }
        };
        filterField.getDocument().addDocumentListener(filterListener);
        disposables.add(() -> filterField.getDocument().removeDocumentListener(filterListener));

        formsCb.addActionListener(e -> filterItems());
        tablesCb.addActionListener(e -> filterItems());
        itemList.addListSelectionListener(this::onListSelectionChanged);
        disposables.add(() -> itemList.removeListSelectionListener(this::onListSelectionChanged));
    }

    private void onListSelectionChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int selectedIndex = itemList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedText = itemList.getSelectedValue();
                int bracketIndex = selectedText.indexOf(" (");
                if (bracketIndex == -1) return;
                String formOrTableName = selectedText.substring(0, bracketIndex);
                Matcher typeMatch = TYPE_PATTERN.matcher(selectedText);
                if (typeMatch.find()) {
                    String type = typeMatch.group(1);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            String content = "";
                            if (type.equals("Form")) {
                                content = MQLUtil.execute(myProject, "print {} {}", type, formOrTableName);
                            } else if (type.equals("Table")) {
                                content = MQLUtil.execute(myProject, "print {} {} system", type, formOrTableName);
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
                            LOG.error("Failed to load content", ex);
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
            highlightPattern(KEYWORDS_PATTERN, keywordAttr, content);
            highlightPattern(STRING_PATTERN, stringAttr, content);
            highlightPattern(NUMBER_PATTERN, numberAttr, content);
        } catch (BadLocationException ex) {
            LOG.warn("Failed to apply syntax highlighting", ex);
        }
    }

    private void highlightPattern(Pattern pattern, AttributeSet attr, String content) {
        try {
            Matcher matcher = pattern.matcher(content);
            StyledDocument doc = contentPane.getStyledDocument();
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                doc.setCharacterAttributes(start, end - start, attr, false);
            }
        } catch (Exception ex) {
            LOG.warn("Highlight pattern failed", ex);
        }
    }

    @Override
    public void dispose() {
        disposables.forEach(Disposer::dispose);
        disposables.clear();
    }
}