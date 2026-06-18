package cn.github.spinner.ui;

import cn.github.spinner.components.FilterTable;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.TriggerQueryUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TriggerQueryDialog extends JFrame {
    private static final int MAX_SUGGESTION_COUNT = 80;
    private static final Object[] COLUMNS = {
            "Schema Type", "Schema Name", "State", "Event", "Sequence", "Trigger", "Program", "Method"
    };

    private final Project project;
    private final ComboBox<TriggerQueryUtil.SchemaType> schemaTypeComboBox =
            new ComboBox<>(TriggerQueryUtil.SchemaType.values());
    private final JBTextField nameField = new JBTextField();
    private final JBTextField stateFilterField = new JBTextField();
    private final JButton queryButton = new JButton(SpinnerBundle.message("button.query"));
    private final DefaultListModel<TriggerQueryUtil.AdminObjectCandidate> suggestionListModel = new DefaultListModel<>();
    private final JBList<TriggerQueryUtil.AdminObjectCandidate> suggestionList = new JBList<>(suggestionListModel);
    private final JBScrollPane suggestionScrollPane = new JBScrollPane(suggestionList);
    private final JPopupMenu suggestionPopup = new JPopupMenu();
    private final Map<TriggerQueryUtil.SchemaType, List<TriggerQueryUtil.AdminObjectCandidate>> suggestionCache =
            new EnumMap<>(TriggerQueryUtil.SchemaType.class);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final FilterTable table = new FilterTable(tableModel);
    private List<TriggerQueryUtil.TriggerQueryResult> results = new ArrayList<>();
    private boolean loadingSuggestions;
    private boolean applyingSuggestion;

    public TriggerQueryDialog(@NotNull Project project) {
        this.project = project;
        setTitle(SpinnerBundle.message("dialog.trigger.query.title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(true);
        setSize(JBUI.size(1100, 680));
        setMinimumSize(JBUI.size(900, 520));
        setContentPane(createContentPanel());
        setLocationRelativeTo(null);
    }

    public static void showWindow(@NotNull Project project) {
        TriggerQueryDialog window = new TriggerQueryDialog(project);
        window.setVisible(true);
    }

    private @NotNull JComponent createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(createQueryPanel(), BorderLayout.NORTH);
        panel.add(createTablePanel(), BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JComponent createQueryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(0, 0, 8, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = JBUI.insets(0, 0, 0, 8);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.weightx = 0;
        panel.add(new JLabel(SpinnerBundle.message("label.trigger.schema.type")), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        schemaTypeComboBox.setPreferredSize(JBUI.size(130, 30));
        schemaTypeComboBox.setToolTipText(SpinnerBundle.message("tooltip.trigger.schema.type"));
        schemaTypeComboBox.setRenderer(new TriggerSchemaTypeRenderer());
        panel.add(schemaTypeComboBox, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(SpinnerBundle.message("label.trigger.name")), constraints);

        constraints.gridx = 3;
        constraints.weightx = 0.45;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        nameField.setToolTipText(SpinnerBundle.message("tooltip.trigger.name"));
        panel.add(nameField, constraints);

        constraints.gridx = 4;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(SpinnerBundle.message("label.policy.states")), constraints);

        constraints.gridx = 5;
        constraints.weightx = 0.25;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        stateFilterField.setToolTipText(SpinnerBundle.message("tooltip.policy.states"));
        panel.add(stateFilterField, constraints);

        constraints.gridx = 6;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        queryButton.addActionListener(e -> queryTriggers());
        panel.add(queryButton, constraints);

        setupNameAutoComplete();
        stateFilterField.addActionListener(e -> queryTriggers());
        return panel;
    }

    private @NotNull JComponent createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getEmptyText().setText(SpinnerBundle.message("message.trigger.query.empty"));
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(220);
        table.getColumnModel().getColumn(6).setPreferredWidth(220);
        table.getColumnModel().getColumn(7).setPreferredWidth(180);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() >= 2) {
                    navigateSelectedResult();
                }
            }
        });
        panel.add(table.getFilterComponent(), BorderLayout.NORTH);
        panel.add(new JBScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void queryTriggers() {
        String name = nameField.getText().trim();
        if (CharSequenceUtil.isBlank(name)) {
            UIUtil.showWarningNotification(project,
                    SpinnerBundle.message("notification.title.trigger.query"),
                    SpinnerBundle.message("message.trigger.name.required"));
            return;
        }

        TriggerQueryUtil.SchemaType schemaType = selectedSchemaType();
        setQuerying(true);
        hideSuggestions();
        tableModel.setRowCount(0);
        table.getEmptyText().setText(SpinnerBundle.message("message.loading.data"));

        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.query.triggers"), true) {
            private List<TriggerQueryUtil.TriggerQueryResult> queryResults = Collections.emptyList();
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    queryResults = TriggerQueryUtil.query(project, schemaType, name, stateFilterField.getText().trim());
                } catch (Throwable throwable) {
                    error = throwable;
                }
            }

            @Override
            public void onSuccess() {
                setQuerying(false);
                if (error != null) {
                    UIUtil.showErrorNotification(project,
                            SpinnerBundle.message("notification.title.trigger.query"),
                            SpinnerBundle.message("message.trigger.query.failed", error.getMessage()));
                    table.getEmptyText().setText(SpinnerBundle.message("message.trigger.query.empty"));
                    return;
                }
                results = new ArrayList<>(queryResults);
                for (TriggerQueryUtil.TriggerQueryResult result : results) {
                    tableModel.addRow(new Object[]{
                            result.schemaType(),
                            result.schemaName(),
                            result.state(),
                            result.eventType(),
                            result.sequence(),
                            result.triggerName(),
                            result.program(),
                            result.method()
                    });
                }
                table.getEmptyText().setText(SpinnerBundle.message("message.trigger.query.no.results"));
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                setQuerying(false);
                UIUtil.showErrorNotification(project,
                        SpinnerBundle.message("notification.title.trigger.query"),
                        SpinnerBundle.message("message.trigger.query.failed", t.getMessage()));
            }
        }.queue();
    }

    private void setQuerying(boolean querying) {
        queryButton.setEnabled(!querying);
        schemaTypeComboBox.setEnabled(!querying);
        nameField.setEnabled(!querying);
        stateFilterField.setEnabled(!querying);
    }

    private void setupNameAutoComplete() {
        suggestionPopup.setFocusable(false);
        suggestionPopup.setBorder(JBUI.Borders.customLine(JBColor.border()));
        suggestionPopup.add(suggestionScrollPane);

        suggestionList.setFixedCellHeight(JBUI.scale(34));
        suggestionList.setCellRenderer(new AdminObjectCandidateRenderer());
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    applySelectedSuggestion();
                }
            }
        });

        schemaTypeComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                hideSuggestions();
                loadSuggestionsIfNeeded();
            }
        });

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                loadSuggestionsIfNeeded();
            }
        });
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshSuggestions();
            }
        });
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_DOWN) {
                    moveSuggestionSelection(1);
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_UP) {
                    moveSuggestionSelection(-1);
                    event.consume();
                } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideSuggestions();
                    event.consume();
                }
            }
        });
        nameField.addActionListener(event -> {
            if (suggestionPopup.isVisible()) {
                applySelectedSuggestion();
            } else {
                queryTriggers();
            }
        });
    }

    private void loadSuggestionsIfNeeded() {
        if (!nameField.hasFocus() || loadingSuggestions) {
            return;
        }
        TriggerQueryUtil.SchemaType schemaType = selectedSchemaType();
        if (getCachedSuggestions(schemaType) != null) {
            refreshSuggestions();
            return;
        }

        loadingSuggestions = true;
        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.load.trigger.suggestions"), true) {
            private final TriggerQueryUtil.SchemaType requestType = schemaType;
            private List<TriggerQueryUtil.AdminObjectCandidate> loadedSuggestions = Collections.emptyList();
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    loadedSuggestions = TriggerQueryUtil.listAdminObjects(project, requestType);
                } catch (Throwable throwable) {
                    error = throwable;
                }
            }

            @Override
            public void onSuccess() {
                loadingSuggestions = false;
                if (error != null) {
                    UIUtil.showWarningNotification(project,
                            SpinnerBundle.message("notification.title.trigger.query"),
                            SpinnerBundle.message("message.trigger.suggestions.load.failed", error.getMessage()));
                    return;
                }
                suggestionCache.put(requestType, loadedSuggestions);
                refreshSuggestions();
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                loadingSuggestions = false;
                UIUtil.showWarningNotification(project,
                        SpinnerBundle.message("notification.title.trigger.query"),
                        SpinnerBundle.message("message.trigger.suggestions.load.failed", t.getMessage()));
            }
        }.queue();
    }

    private void refreshSuggestions() {
        if (applyingSuggestion || !nameField.hasFocus()) {
            return;
        }

        TriggerQueryUtil.SchemaType schemaType = selectedSchemaType();
        List<TriggerQueryUtil.AdminObjectCandidate> cachedSuggestions = getCachedSuggestions(schemaType);
        if (cachedSuggestions == null) {
            hideSuggestions();
            loadSuggestionsIfNeeded();
            return;
        }

        String filterText = nameField.getText().trim().toLowerCase(Locale.ROOT);
        suggestionListModel.removeAllElements();
        cachedSuggestions.stream()
                .filter(candidate -> filterText.isEmpty() ||
                        candidate.name().toLowerCase(Locale.ROOT).contains(filterText))
                .limit(MAX_SUGGESTION_COUNT)
                .forEach(suggestionListModel::addElement);
        if (suggestionListModel.isEmpty()) {
            hideSuggestions();
            return;
        }

        suggestionList.setSelectedIndex(0);
        showSuggestions();
    }

    private @Nullable List<TriggerQueryUtil.AdminObjectCandidate> getCachedSuggestions(@NotNull TriggerQueryUtil.SchemaType schemaType) {
        List<TriggerQueryUtil.AdminObjectCandidate> suggestions = suggestionCache.get(schemaType);
        if (suggestions != null) {
            return suggestions;
        }
        List<TriggerQueryUtil.AdminObjectCandidate> allSuggestions = suggestionCache.get(TriggerQueryUtil.SchemaType.ALL);
        if (schemaType == TriggerQueryUtil.SchemaType.ALL || allSuggestions == null) {
            return null;
        }
        return allSuggestions.stream()
                .filter(candidate -> candidate.schemaType() == schemaType)
                .toList();
    }

    private void showSuggestions() {
        if (!nameField.isShowing()) {
            return;
        }
        int rowCount = Math.min(suggestionListModel.getSize(), 8);
        int width = Math.max(nameField.getWidth(), JBUI.scale(360));
        int height = Math.max(rowCount, 1) * suggestionList.getFixedCellHeight() + JBUI.scale(4);
        suggestionScrollPane.setPreferredSize(new Dimension(width, height));
        suggestionPopup.pack();
        if (!suggestionPopup.isVisible()) {
            suggestionPopup.show(nameField, 0, nameField.getHeight());
        }
    }

    private void hideSuggestions() {
        suggestionPopup.setVisible(false);
    }

    private void moveSuggestionSelection(int offset) {
        if (!suggestionPopup.isVisible()) {
            refreshSuggestions();
            return;
        }
        int size = suggestionListModel.getSize();
        if (size <= 0) {
            return;
        }
        int selectedIndex = suggestionList.getSelectedIndex();
        int nextIndex = Math.max(0, Math.min(size - 1, selectedIndex + offset));
        suggestionList.setSelectedIndex(nextIndex);
        suggestionList.ensureIndexIsVisible(nextIndex);
    }

    private void applySelectedSuggestion() {
        TriggerQueryUtil.AdminObjectCandidate candidate = suggestionList.getSelectedValue();
        if (candidate == null) {
            queryTriggers();
            return;
        }
        applyingSuggestion = true;
        nameField.setText(candidate.name());
        applyingSuggestion = false;
        hideSuggestions();
        nameField.requestFocusInWindow();
    }

    private TriggerQueryUtil.SchemaType selectedSchemaType() {
        Object selectedItem = schemaTypeComboBox.getSelectedItem();
        return selectedItem instanceof TriggerQueryUtil.SchemaType schemaType
                ? schemaType
                : TriggerQueryUtil.SchemaType.ALL;
    }

    private @NotNull String schemaTypeDisplayName(@NotNull TriggerQueryUtil.SchemaType schemaType) {
        return schemaType == TriggerQueryUtil.SchemaType.ALL
                ? SpinnerBundle.message("option.trigger.schema.all")
                : schemaType.mqlName();
    }

    private @NotNull Color schemaTypeColor(@NotNull TriggerQueryUtil.SchemaType schemaType) {
        return switch (schemaType) {
            case TYPE -> new JBColor(new Color(0x1D4ED8), new Color(0x93C5FD));
            case POLICY -> new JBColor(new Color(0x7C3AED), new Color(0xC4B5FD));
            case RELATIONSHIP -> new JBColor(new Color(0x047857), new Color(0x6EE7B7));
            case ATTRIBUTE -> new JBColor(new Color(0xC2410C), new Color(0xFDBA74));
            case ALL -> JBColor.GRAY;
        };
    }

    private void navigateSelectedResult() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= results.size()) {
            return;
        }

        TriggerQueryUtil.TriggerQueryResult result = results.get(modelRow);
        if (result.sourcePath().isBlank()) {
            openRemoteProgramSource(result);
            return;
        }

        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path.of(result.sourcePath()));
        if (virtualFile == null) {
            openRemoteProgramSource(result);
            return;
        }
        int line = Math.max(result.sourceLine() - 1, 0);
        new OpenFileDescriptor(project, virtualFile, line, 0).navigate(true);
    }

    private void openRemoteProgramSource(@NotNull TriggerQueryUtil.TriggerQueryResult result) {
        new TrackedBackgroundTask(project, SpinnerBundle.message("progress.load.trigger.source"), true) {
            private String sourceCode = "";
            private int methodLine = -1;
            private TriggerQueryUtil.ClassLookupResult classLookupResult;
            private boolean remoteSourceLoaded;
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    sourceCode = TriggerQueryUtil.queryProgramCode(project, result.program());
                    if (CharSequenceUtil.isNotBlank(sourceCode)) {
                        methodLine = TriggerQueryUtil.findMethodLine(sourceCode, result.method());
                        remoteSourceLoaded = true;
                        return;
                    }
                    error = new IllegalStateException(
                            SpinnerBundle.message("message.trigger.source.not.found", result.program()));
                } catch (Throwable throwable) {
                    error = throwable;
                }
                try {
                    classLookupResult = TriggerQueryUtil.findClassTarget(project, result.program(), result.method());
                } catch (Throwable throwable) {
                    if (error == null) {
                        error = throwable;
                    }
                }
            }

            @Override
            public void onSuccess() {
                if (remoteSourceLoaded) {
                    LightVirtualFile virtualFile = new LightVirtualFile(result.program() + ".java");
                    virtualFile.setFileType(JavaFileType.INSTANCE);
                    virtualFile.setContent(this, sourceCode, false);
                    virtualFile.setWritable(false);
                    new OpenFileDescriptor(project, virtualFile, Math.max(methodLine - 1, 0), 0).navigate(true);
                    return;
                }
                if (navigateClassTarget(classLookupResult)) {
                    return;
                }
                if (error != null) {
                    UIUtil.showWarningNotification(project,
                            SpinnerBundle.message("notification.title.trigger.query"),
                            SpinnerBundle.message("message.trigger.source.load.failed", result.program(), error.getMessage()));
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable t) {
                UIUtil.showWarningNotification(project,
                        SpinnerBundle.message("notification.title.trigger.query"),
                        SpinnerBundle.message("message.trigger.source.load.failed", result.program(), t.getMessage()));
            }
        }.queue();
    }

    private boolean navigateClassTarget(@Nullable TriggerQueryUtil.ClassLookupResult classLookupResult) {
        if (classLookupResult == null || classLookupResult.isEmpty()) {
            return false;
        }

        SmartPsiElementPointer<PsiElement> elementPointer = classLookupResult.elementPointer();
        if (elementPointer != null) {
            PsiElement element = elementPointer.getElement();
            if (element instanceof Navigatable navigatable && element.isValid()) {
                navigatable.navigate(true);
                return true;
            }
        }

        if (classLookupResult.classPath().isBlank()) {
            return false;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance()
                .refreshAndFindFileByNioFile(Path.of(classLookupResult.classPath()));
        if (virtualFile == null) {
            return false;
        }
        new OpenFileDescriptor(project, virtualFile).navigate(true);
        return true;
    }

    private final class TriggerSchemaTypeRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Object displayValue = value instanceof TriggerQueryUtil.SchemaType schemaType
                    ? schemaTypeDisplayName(schemaType)
                    : value;
            return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
        }
    }

    private final class AdminObjectCandidateRenderer extends JPanel
            implements ListCellRenderer<TriggerQueryUtil.AdminObjectCandidate> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel typeLabel = new JLabel();

        private AdminObjectCandidateRenderer() {
            super(new BorderLayout(JBUI.scale(16), 0));
            setOpaque(true);
            setBorder(JBUI.Borders.empty(0, 12));
            nameLabel.setOpaque(false);
            typeLabel.setOpaque(false);
            typeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(nameLabel, BorderLayout.CENTER);
            add(typeLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends TriggerQueryUtil.AdminObjectCandidate> list,
                                                      TriggerQueryUtil.AdminObjectCandidate value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value == null) {
                nameLabel.setText("");
                typeLabel.setText("");
            } else {
                nameLabel.setText(value.name());
                typeLabel.setText(value.schemaType().mqlName());
            }

            Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
            setBackground(background);
            Color schemaColor = value == null ? list.getForeground() : schemaTypeColor(value.schemaType());
            nameLabel.setForeground(schemaColor);
            typeLabel.setForeground(schemaColor);
            return this;
        }
    }
}
