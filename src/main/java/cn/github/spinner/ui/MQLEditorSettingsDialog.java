package cn.github.spinner.ui;

import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
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
    @Getter
    private final JBTextField resultMaxSizeMb;
    @Getter
    private final TextFieldWithBrowseButton cachePath;

    public MQLEditorSettingsDialog(Project project) {
        super(true);
        this.project = project;
        setTitle(SpinnerBundle.message("dialog.mql.editor.settings.title"));
        setOKButtonText(SpinnerBundle.message("button.ok"));
        // 初始化字段
        keepExecHistory = new JBCheckBox(SpinnerBundle.message("checkbox.keep.executing.history"));
        lineDelimiter = new JBTextField();
        timeoutMinutes = new JBTextField();
        resultMaxSizeMb = new JBTextField();
        cachePath = new TextFieldWithBrowseButton();
        cachePath.addBrowseFolderListener(new TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()));
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
        int resultMaxSizeMbInt = spinnerSettings.getMqlResultMaxSizeMb();
        resultMaxSizeMb.setText(resultMaxSizeMbInt == 0 ? "5" : String.valueOf(resultMaxSizeMbInt));
        cachePath.setText(spinnerSettings.getAdminDefinitionsCachePath());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(keepExecHistory)
                .addLabeledComponent(SpinnerBundle.message("label.line.delimiter"), lineDelimiter)
                .addLabeledComponent(SpinnerBundle.message("label.timeout.minutes"), timeoutMinutes)
                .addLabeledComponent(SpinnerBundle.message("label.result.max.size.mb"), resultMaxSizeMb)
                .addLabeledComponent(SpinnerBundle.message("label.admin.definitions.cache.path"), cachePath)
                .getPanel();
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
