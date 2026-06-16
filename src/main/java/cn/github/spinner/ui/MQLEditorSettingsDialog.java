package cn.github.spinner.ui;

import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.i18n.SpinnerBundle;
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
    @Getter
    private final JBTextField timeoutMinutes;

    public MQLEditorSettingsDialog(Project project) {
        super(true);
        this.project = project;
        setTitle(SpinnerBundle.message("dialog.mql.editor.settings.title"));
        setOKButtonText(SpinnerBundle.message("button.ok"));
        // 初始化字段
        keepExecHistory = new JBCheckBox(SpinnerBundle.message("checkbox.keep.executing.history"));
        lineDelimiter = new JBTextField();
        timeoutMinutes = new JBTextField();
        setupValue();
        init();
    }

    private void setupValue() {
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        keepExecHistory.setSelected(spinnerSettings.isKeepMQLExecuteHistory());
        String lineDelimiterStr = spinnerSettings.getLineDelimiter();
        lineDelimiter.setText(lineDelimiterStr.isEmpty() ? "\\n" : lineDelimiterStr);
        int timeoutMinutesInt = spinnerSettings.getTimeoutMinutes();
        timeoutMinutes.setText(timeoutMinutesInt == 0 ? "10" : String.valueOf(timeoutMinutesInt));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(keepExecHistory)
                .addLabeledComponent(SpinnerBundle.message("label.line.delimiter"), lineDelimiter)
                .addLabeledComponent(SpinnerBundle.message("label.timeout.minutes"), timeoutMinutes)
                .getPanel();
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
