package cn.github.spinner.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MQLPlaceholderInputDialog extends DialogWrapper {
    private final Map<String, JBTextField> fieldMap = new LinkedHashMap<>();
    private final Consumer<Map<String, String>> submitHandler;

    public MQLPlaceholderInputDialog(@Nullable Project project,
                                     @NotNull List<String> placeholders,
                                     @NotNull Consumer<Map<String, String>> submitHandler) {
        super(project);
        this.submitHandler = submitHandler;
        setTitle("MQL Placeholder Input");
        setOKButtonText("OK");
        setCancelButtonText("Cancel");
        setModal(false);
        placeholders.forEach(placeholder -> fieldMap.put(placeholder, new JBTextField()));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        fieldMap.forEach((placeholder, field) ->
                builder.addLabeledComponent(placeholder + ":", field)
        );
        return builder.getPanel();
    }

    public @NotNull Map<String, String> getValues() {
        Map<String, String> values = new LinkedHashMap<>();
        fieldMap.forEach((placeholder, field) -> values.put(placeholder, field.getText()));
        return values;
    }

    @Override
    protected void doOKAction() {
        submitHandler.accept(getValues());
        super.doOKAction();
    }
}
