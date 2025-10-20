package com.bol.spinner.ui;

import com.bol.spinner.config.SpinnerSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

@Slf4j
public class MQLEditorSettingsDialog extends DialogWrapper {
    private final Project project;
    @Getter
    private final JBCheckBox keepExecHistory;
    @Getter
    private final JBTextField lineDelimiter;

    public MQLEditorSettingsDialog(Project project) {
        super(true);
        this.project = project;
        setTitle("MQL Editor Settings");
        setOKButtonText("OK");
        // 初始化字段
        keepExecHistory = new JBCheckBox("keep executing history");
        lineDelimiter = new JBTextField();
        setupValue();
        init();
    }

    private void setupValue() {
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        keepExecHistory.setSelected(spinnerSettings.isKeepMQLExecuteHistory());
        String lineDelimiterStr = spinnerSettings.getLineDelimiter();
        lineDelimiter.setText(lineDelimiterStr.isEmpty() ? "\\n" : lineDelimiterStr);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(keepExecHistory)
                .addLabeledComponent("Line Delimiter", lineDelimiter)
                .getPanel();
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
