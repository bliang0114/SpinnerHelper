package com.bol.spinner.editor.spinner;

import cn.github.driver.connection.MatrixConnection;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.ui.ComboBoxWithFilter;
import com.bol.spinner.util.UIUtil;
import com.bol.spinner.util.WorkspaceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextField;
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
    private static SpinnerDataRecordBuilder INSTANCE = null;
    private Project project;
    private VirtualFile virtualFile;
    private int modelRowIndex;
    private AbstractSpinnerViewComponent spinnerViewComponent;
    private JComponent[] components;
    private final DefaultActionGroup actionGroup;

    private SpinnerDataRecordBuilder(VirtualFile virtualFile, int modelRowIndex, AbstractSpinnerViewComponent spinnerViewComponent) {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        this.virtualFile = virtualFile;
        this.modelRowIndex = modelRowIndex;
        this.spinnerViewComponent = spinnerViewComponent;
        actionGroup = new DefaultActionGroup();
        actionGroup.add(new ApplyAction());
        actionGroup.add(new DeployAction());
    }

    public static SpinnerDataRecordBuilder createBuilder(VirtualFile virtualFile, int modelRowIndex, AbstractSpinnerViewComponent spinnerViewComponent) {
        if (INSTANCE == null) {
            INSTANCE = new SpinnerDataRecordBuilder(virtualFile, modelRowIndex, spinnerViewComponent);
        }
        INSTANCE.virtualFile = virtualFile;
        INSTANCE.modelRowIndex = modelRowIndex;
        INSTANCE.spinnerViewComponent = spinnerViewComponent;
        return INSTANCE;
    }

    public SpinnerDataRecordBuilder setProject(Project project) {
        INSTANCE.project = project;
        return INSTANCE;
    }

    public JComponent build(String[] headers, Vector<String> values) {
        INSTANCE.components = new JComponent[headers.length];
        SpinnerType spinnerType = SpinnerType.fromFile(INSTANCE.virtualFile.getName());
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (int i = 0; i < headers.length; i++) {
            String value = i >= values.size() ? "" : values.elementAt(i);
            String label = headers[i];
            if (CharSequenceUtil.containsAny(headers[i], "Setting Name")) {
                label = label.replace(" Names ", " Names \\& Values ");
                label = label.replace(" Name ", " Name \\& Value ");
                INSTANCE.components[i] = new SpinnerSettingsComponent(spinnerType, value, values.elementAt(i + 1));
            } else if (CharSequenceUtil.containsAny(label, "Setting Value")) {
                continue;
            } else if (spinnerType == SpinnerType.ATTRIBUTE && "Type".equals(label)) {
                List<String> items = List.of("binary", "boolean", "enum", "integer", "real", "string", "timestamp");
                ComboBoxWithFilter<String> comboBox = new ComboBoxWithFilter<>(items, value);
                INSTANCE.components[i] = comboBox;
            } else if (spinnerType == SpinnerType.ATTRIBUTE && CharSequenceUtil.containsAny(label, "Ranges")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.CHANNEL && CharSequenceUtil.containsAny(label, "Commands")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.COMMAND && CharSequenceUtil.containsAny(label, "Users", "Unblock")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.INTERFACE && CharSequenceUtil.containsAny(label, "Parents", "Attributes", "Types", "Rels")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.MENU && CharSequenceUtil.containsAny(label, "Command/Menu Names")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.POLICY && CharSequenceUtil.containsAny(label, "Types", "Formats")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.PORTAL && CharSequenceUtil.containsAny(label, "Channels")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.RELATIONSHIP && CharSequenceUtil.containsAny(label, "Attributes", "From Types", "From Rels", "To Types", "To Rels")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.ROLE && CharSequenceUtil.containsAny(label, "Parent Roles", "Child Roles", "Assignments")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.RULE && CharSequenceUtil.containsAny(label, "Programs", "Attributes", "Forms", "Interfaces")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.TABLE_COLUMN && CharSequenceUtil.containsAny(label, "Users")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.TRIGGER && CharSequenceUtil.containsAny(label, "Input")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value, " ");
            } else if (spinnerType == SpinnerType.TYPE && CharSequenceUtil.containsAny(label, "Attributes", "Methods")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.FORM && CharSequenceUtil.containsAny(label, "Types")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else if (spinnerType == SpinnerType.FORM_FIELD && CharSequenceUtil.containsAny(label, "Users")) {
                INSTANCE.components[i] = new SpinnerMultiTextFieldComponent(label, value);
            } else {
                INSTANCE.components[i] = new SpinnerTextFieldComponent(label, value);
            }
            formBuilder.addLabeledComponent(label, INSTANCE.components[i]).addSeparator();
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
        if (INSTANCE.components != null) {
            for (JComponent component : INSTANCE.components) {
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

    public class ApplyAction extends AnAction {
        public ApplyAction() {
            super("Apply", "Apply", AllIcons.General.GreenCheckmark);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            apply();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class DeployAction extends AnAction {
        public DeployAction() {
            super("Apply && Deploy", "Apply && Deploy", AllIcons.Nodes.Deploy);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
           String finalValue = apply();
            if (INSTANCE.project == null) {
                UIUtil.showWarningNotification(null, "Spinner Data View", "project is null, deploy failure");
                return;
            }
            MatrixConnection connection = SpinnerToken.getCurrentConnection(INSTANCE.project);
            if (connection == null) {
                UIUtil.showWarningNotification(null, "Spinner Data View", "connection is null, deploy failure");
                return;
            }
            List<String> lines = FileUtil.readLines(INSTANCE.virtualFile.getPath(), INSTANCE.virtualFile.getCharset());
            WorkspaceUtil.importSpinnerFile(connection, INSTANCE.project, INSTANCE.virtualFile.getPath(), lines.getFirst() + "\n" + finalValue);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public String apply(){
        String value = getValue();
        log.info("Value: {}", value);
        int lineNumber = modelRowIndex + 1;
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) return null;
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) return null;
        if (lineNumber >= document.getLineCount()) return null;
        int startOffset = document.getLineStartOffset(lineNumber);
        int endOffset = document.getLineEndOffset(lineNumber);
        String lineValue = document.getText(new TextRange(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)));
        String[] valueArr = value.split("\t", -1);
        String[] lineValueArr = lineValue.split("\t", -1);
        boolean notNeedRefactor = valueArr.length <= lineValueArr.length || Arrays.stream(valueArr, lineValueArr.length, valueArr.length).anyMatch(StrUtil::isNotEmpty);
        if (!notNeedRefactor) {
            String[] newValueArr = Arrays.copyOf(valueArr, lineValueArr.length);
            value = String.join("\t", newValueArr);
        }
        String finalValue = value;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(startOffset, endOffset, finalValue);
            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
        INSTANCE.spinnerViewComponent.reloadValue(INSTANCE.modelRowIndex, finalValue);
        return finalValue;
    }


}
