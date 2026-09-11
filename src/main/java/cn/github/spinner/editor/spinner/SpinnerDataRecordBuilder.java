package cn.github.spinner.editor.spinner;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.components.ComboBoxWithFilter;
import cn.github.spinner.components.EnvironmentIndicator;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

@Slf4j
public class SpinnerDataRecordBuilder {
    private static final long SLOW_BUILD_MS = 100L;
    private Project project;
    private VirtualFile virtualFile;
    private int modelRowIndex;
    private AbstractSpinnerViewComponent spinnerViewComponent;
    private JComponent[] components;
    private String originalLine;
    private String[] renderedBaseline;
    private boolean building;
    private final DefaultActionGroup actionGroup;

    private SpinnerDataRecordBuilder(VirtualFile virtualFile, int modelRowIndex, AbstractSpinnerViewComponent spinnerViewComponent) {
        this.virtualFile = virtualFile;
        this.modelRowIndex = modelRowIndex;
        this.spinnerViewComponent = spinnerViewComponent;
        actionGroup = new DefaultActionGroup();
        actionGroup.add(new DeployAction());
    }

    public static SpinnerDataRecordBuilder createBuilder(VirtualFile virtualFile, int modelRowIndex, AbstractSpinnerViewComponent spinnerViewComponent) {
        return new SpinnerDataRecordBuilder(virtualFile, modelRowIndex, spinnerViewComponent);
    }

    public SpinnerDataRecordBuilder setProject(Project project) {
        this.project = project;
        return this;
    }

    public JComponent build(String[] headers, Vector<String> values) {
        building = true;
        originalLine = String.join("\t", values.stream().map(value -> value == null ? "" : value).toList());
        long startedNanos = System.nanoTime();
        this.components = new JComponent[headers.length];
        SpinnerType spinnerType = SpinnerType.fromFile(this.virtualFile);
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (int i = 0; i < headers.length; i++) {
            String value = i >= values.size() ? "" : values.elementAt(i);
            String label = headers[i];
            if (CharSequenceUtil.containsAny(headers[i], "Setting Name")) {
                label = label.replace(" Names ", " Names \\& Values ");
                label = label.replace(" Name ", " Name \\& Value ");
                this.components[i] = new SpinnerSettingsComponent(spinnerType, value,
                        i + 1 < values.size() ? values.elementAt(i + 1) : "", this::applyIfChanged);
            } else if (CharSequenceUtil.containsAny(label, "Setting Value")) {
                continue;
            } else if (spinnerType == SpinnerType.ATTRIBUTE && "Type".equals(label)) {
                List<String> items = List.of("binary", "boolean", "enum", "integer", "real", "string", "timestamp");
                ComboBoxWithFilter<String> comboBox = new ComboBoxWithFilter<>(items, value);
                registerComboBoxAutoSave(comboBox);
                this.components[i] = comboBox;
            } else if (spinnerType == SpinnerType.ATTRIBUTE && CharSequenceUtil.containsAny(label, "Ranges")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.CHANNEL && CharSequenceUtil.containsAny(label, "Commands")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.COMMAND && CharSequenceUtil.containsAny(label, "Users", "Unblock")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.INTERFACE && CharSequenceUtil.containsAny(label, "Parents", "Attributes", "Types", "Rels")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.MENU && CharSequenceUtil.containsAny(label, "Command/Menu Names")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.POLICY && CharSequenceUtil.containsAny(label, "Types", "Formats")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.PORTAL && CharSequenceUtil.containsAny(label, "Channels")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.RELATIONSHIP && CharSequenceUtil.containsAny(label, "Attributes", "From Types", "From Rels", "To Types", "To Rels")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.ROLE && CharSequenceUtil.containsAny(label, "Parent Roles", "Child Roles", "Assignments")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.RULE && CharSequenceUtil.containsAny(label, "Programs", "Attributes", "Forms", "Interfaces")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.TABLE_COLUMN && CharSequenceUtil.containsAny(label, "Users")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.TRIGGER && CharSequenceUtil.containsAny(label, "Input")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, " ", this::applyIfChanged);
            } else if (spinnerType == SpinnerType.TYPE && CharSequenceUtil.containsAny(label, "Attributes", "Methods")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.FORM && CharSequenceUtil.containsAny(label, "Types")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else if (spinnerType == SpinnerType.FORM_FIELD && CharSequenceUtil.containsAny(label, "Users")) {
                this.components[i] = new SpinnerMultiTextFieldComponent(label, value, this::applyIfChanged);
            } else {
                this.components[i] = new SpinnerTextFieldComponent(label, value, this::applyIfChanged);
            }
            formBuilder.addLabeledComponent(label, this.components[i]).addSeparator();
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner Row Record.Toolbar", actionGroup, true);
        toolbar.setTargetComponent(panel);
        toolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);
        panel.add(toolbarPanel, BorderLayout.NORTH);

        // 创建包装面板，使用BorderLayout确保顶部对齐
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(formBuilder.getPanel(), BorderLayout.NORTH); // 关键：使用NORTH而不是CENTER
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(wrapperPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
        logPerformance("record form build", startedNanos, SLOW_BUILD_MS,
                "row=" + modelRowIndex + ", columns=" + headers.length + ", spinnerType=" + spinnerType);
        renderedBaseline = getRenderedValues();
        building = false;
        watchInput(panel);
        return panel;
    }

    boolean hasChanges() { return !building && !java.util.Objects.equals(originalLine, getValue()); }
    void deactivate() { building = true; }

    private void watchInput(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.text.JTextComponent text) {
                text.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { spinnerViewComponent.pendingInputChanged(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { spinnerViewComponent.pendingInputChanged(); }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { spinnerViewComponent.pendingInputChanged(); }
                });
            }
            if (child instanceof Container container) watchInput(container);
        }
    }

    public String getValue() {
        if (renderedBaseline == null) return originalLine;
        String[] edited = getRenderedValues();
        String[] result = originalLine.split("\t", -1);
        for (int i = 0; i < edited.length; i++) {
            if (java.util.Objects.equals(edited[i], renderedBaseline[i])) continue;
            if (i >= result.length) {
                int previousLength = result.length;
                result = Arrays.copyOf(result, i + 1);
                Arrays.fill(result, previousLength, result.length, "");
            }
            result[i] = edited[i];
        }
        return String.join("\t", result);
    }

    private String[] getRenderedValues() {
        String[] values = new String[components.length];
        Arrays.fill(values, "");
        if (this.components != null) {
            for (int i = 0; i < components.length; i++) {
                JComponent component = components[i];
                if (component instanceof SpinnerTextFieldComponent textFieldComponent) {
                    values[i] = textFieldComponent.getValue();
                } else if (component instanceof ComboBox<?> comboBox) {
                    values[i] = String.valueOf(comboBox.getEditor().getItem());
                } else if (component instanceof SpinnerSettingsComponent settingsComponent) {
                    String[] pair = settingsComponent.getValue().split("\t", -1);
                    values[i] = pair[0];
                    if (i + 1 < values.length) values[++i] = pair[1];
                } else if (component instanceof SpinnerMultiTextFieldComponent multiTextFieldComponent) {
                    values[i] = multiTextFieldComponent.getValue();
                }
            }
        }
        return values;
    }

    public class DeployAction extends AnAction {
        public DeployAction() {
            super(SpinnerBundle.message("action.deploy.text"), SpinnerBundle.message("action.deploy.description"), AllIcons.Nodes.Deploy);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!spinnerViewComponent.isDocumentCurrent()) return;
            String finalValue = spinnerViewComponent.appliedRecord(modelRowIndex);
            if (finalValue == null) return;
            if (project == null) {
                UIUtil.showWarningNotification(null, SpinnerBundle.message("notification.title.spinner.data.view"), SpinnerBundle.message("message.batch.processing.failed", "project is null"));
                return;
            }
            MatrixConnection connection = UserInput.getInstance().connection.get(project);
            if (connection == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, SpinnerBundle.message("message.connect.required"));
                return;
            }
            WorkspaceUtil.importSpinnerFile(connection, project, virtualFile.getPath(), finalValue);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setText(EnvironmentIndicator.actionText(
                    project,
                    SpinnerBundle.message("action.deploy.text")
            ));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private void registerComboBoxAutoSave(ComboBoxWithFilter<String> comboBox) {
        comboBox.addActionListener(e -> applyIfChanged());
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField textField) {
            textField.addActionListener(e -> applyIfChanged());
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    applyIfChanged();
                }
            });
        }
    }

    private void applyIfChanged() {
        if (building) return;
        apply();
    }

    public String apply(){
        String value = getValue();
        if (building || value == null || value.equals(originalLine)) return value;
        if (!spinnerViewComponent.editDraft(modelRowIndex, originalLine, value)) return null;
        originalLine = value;
        renderedBaseline = getRenderedValues();
        return value;
    }

    private void logPerformance(@NotNull String operation, long startedNanos, long slowThresholdMs, @NotNull String details) {
        long elapsedMs = elapsedMillis(startedNanos);
        String message = "[SpinnerEditorPerf] " + operation + ": file=" + virtualFile.getPath() +
                ", elapsedMs=" + elapsedMs + ", " + details + ", thread=" + Thread.currentThread().getName() +
                ", edt=" + SwingUtilities.isEventDispatchThread();
        if (elapsedMs >= slowThresholdMs) {
            log.warn(message);
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }



}
