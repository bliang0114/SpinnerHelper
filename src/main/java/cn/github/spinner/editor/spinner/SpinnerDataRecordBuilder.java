package cn.github.spinner.editor.spinner;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.components.ComboBoxWithFilter;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
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
    private Project project;
    private VirtualFile virtualFile;
    private int modelRowIndex;
    private AbstractSpinnerViewComponent spinnerViewComponent;
    private JComponent[] components;
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
        this.components = new JComponent[headers.length];
        SpinnerType spinnerType = SpinnerType.fromFile(this.virtualFile);
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (int i = 0; i < headers.length; i++) {
            String value = i >= values.size() ? "" : values.elementAt(i);
            String label = headers[i];
            if (CharSequenceUtil.containsAny(headers[i], "Setting Name")) {
                label = label.replace(" Names ", " Names \\& Values ");
                label = label.replace(" Name ", " Name \\& Value ");
                this.components[i] = new SpinnerSettingsComponent(spinnerType, value, values.elementAt(i + 1), this::applyIfChanged);
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
        return panel;
    }

    public String getValue() {
        StringBuilder value = new StringBuilder();
        if (this.components != null) {
            for (JComponent component : this.components) {
                if (component instanceof SpinnerTextFieldComponent textFieldComponent) {
                    value.append(textFieldComponent.getValue()).append("\t");
                } else if (component instanceof ComboBox<?> comboBox) {
                    value.append(comboBox.getItem()).append("\t");
                } else if (component instanceof SpinnerSettingsComponent settingsComponent) {
                    value.append(settingsComponent.getValue()).append("\t");
                } else if (component instanceof SpinnerMultiTextFieldComponent multiTextFieldComponent) {
                    value.append(multiTextFieldComponent.getValue()).append("\t");
                }
            }
        }
        return value.toString();
    }

    public class DeployAction extends AnAction {
        public DeployAction() {
            super("Deploy", "Deploy", AllIcons.Nodes.Deploy);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
           String finalValue = apply();
            if (project == null) {
                UIUtil.showWarningNotification(null, "Spinner Data View", "project is null, deploy failure");
                return;
            }
            MatrixConnection connection = UserInput.getInstance().connection.get(project);
            if (connection == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, "Please connect to a matrix server first.");
                return;
            }
            List<String> lines = FileUtil.readLines(virtualFile.getPath(), virtualFile.getCharset());
            WorkspaceUtil.importSpinnerFile(connection, project, virtualFile.getPath(), lines.getFirst() + "\n" + finalValue);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
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
        apply();
    }

    public String apply(){
        String value = getValue();
        log.info("Value: {}", value);
        int lineNumber = modelRowIndex + 1;
        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(virtualFile));
        if (document == null) return null;
        LineSnapshot snapshot = ReadAction.compute(() -> {
            if (lineNumber >= document.getLineCount()) {
                return null;
            }
            int startOffset = document.getLineStartOffset(lineNumber);
            int endOffset = document.getLineEndOffset(lineNumber);
            String lineValue = document.getText(new TextRange(startOffset, endOffset));
            return new LineSnapshot(startOffset, endOffset, lineValue);
        });
        if (snapshot == null) return null;
        String[] valueArr = value.split("\t", -1);
        String[] lineValueArr = snapshot.lineValue.split("\t", -1);
        boolean notNeedRefactor = valueArr.length <= lineValueArr.length || Arrays.stream(valueArr, lineValueArr.length, valueArr.length).anyMatch(StrUtil::isNotEmpty);
        if (!notNeedRefactor) {
            String[] newValueArr = Arrays.copyOf(valueArr, lineValueArr.length);
            value = String.join("\t", newValueArr);
        }
        String finalValue = value;
        if (finalValue.equals(snapshot.lineValue)) {
            return finalValue;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(snapshot.startOffset, snapshot.endOffset, finalValue);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
        spinnerViewComponent.reloadValue(modelRowIndex, finalValue);
        return finalValue;
    }

    private record LineSnapshot(int startOffset, int endOffset, String lineValue) {
    }


}
