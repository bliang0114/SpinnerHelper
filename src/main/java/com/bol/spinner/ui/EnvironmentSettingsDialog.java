package com.bol.spinner.ui;

import com.bol.spinner.auth.LogonServer;
import com.bol.spinner.auth.Util;
import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.util.UIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import matrix.db.Context;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.util.*;

public class EnvironmentSettingsDialog extends DialogWrapper {
    private final Project project;
    private final EnvironmentConfig environment;
    private final JBTextField environmentField;
    private final JBTextField hostUrlField;
    private final JBTextField usernameField;
    private final JBPasswordField passwordField;
    private final JBTextField vaultField;
    private final ComboBox<String> securityContextComboBox;

    public EnvironmentSettingsDialog(Project project, EnvironmentConfig environment) {
        super(true); // 使用当前窗口作为父窗口
        this.project = project;
        this.environment = environment;
        setTitle(environment != null ? "Edit Environment" : "New Environment");
        setOKButtonText("OK");
        // 初始化字段
        environmentField = new JBTextField();
        hostUrlField = new JBTextField();
        usernameField = new JBTextField();
        passwordField = new JBPasswordField();
        vaultField = new JBTextField("eService Production");
        ExtendableTextComponent.Extension loadExtension =
                ExtendableTextComponent.Extension.create(
                        AllIcons.General.Refresh,
                        AllIcons.General.Refresh,
                        "Load Security Context",
                        loadSecurityContext()
                );
        securityContextComboBox = new ComboBox<>();
        securityContextComboBox.setEditable(true);
        securityContextComboBox.setEditor(new BasicComboBoxEditor() {
            @Override
            protected JTextField createEditorComponent() {
                ExtendableTextField ecbEditor = new ExtendableTextField();
                ecbEditor.addExtension(loadExtension);
                ecbEditor.setBorder(null);
                return ecbEditor;
            }
        });
        init();
        if (environment != null) {
            environmentField.setEnabled(false);
            environmentField.setText(environment.getName());
            hostUrlField.setText(environment.getHostUrl());
            usernameField.setText(environment.getUser());
            passwordField.setText(environment.getPassword());
            vaultField.setText(environment.getVault());
            securityContextComboBox.setItem(environment.getSecurityContext());
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Environment:", environmentField)
                .addTooltip("Enter a unique name for this environment")
                .addSeparator()
                .addLabeledComponent("Host URL:", hostUrlField)
                .addTooltip("E.g., https://r2023x.mydomain.com/3dspace")
                .addLabeledComponent("Username:", usernameField)
                .addLabeledComponent("Password:", passwordField)
                .addSeparator()
                .addLabeledComponent("Vault:", vaultField)
                .addTooltip("E.g., eService production")
                .addLabeledComponent("Security Context:", securityContextComboBox)
                .addTooltip("E.g., VPLMAdmin.Company Name.Common Space")
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private String getName() {
        return environmentField.getText().trim();
    }

    private String getHostUrl() {
        return hostUrlField.getText().trim();
    }

    private String getUsername() {
        return usernameField.getText().trim();
    }

    private String getPassword() {
        return new String(passwordField.getPassword());
    }

    private String getVault() {
        return vaultField.getText().trim();
    }

    private String getSecurityContext() {
        return securityContextComboBox.getItem().trim();
    }

    // 验证输入
    @Override
    protected void doOKAction() {
        if (validateInput()) {
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        if (getName().isEmpty()) {
            setErrorText("Environment is required", environmentField);
            return false;
        }
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        Optional<EnvironmentConfig> optional = spinnerSettings.getEnvironment(getName());
        if (optional.isPresent() && environment == null) {
            setErrorText("Environment is exist", environmentField);
            return false;
        }
        if (getHostUrl().isEmpty()) {
            setErrorText("Host URL is required", hostUrlField);
            return false;
        }
        if (getUsername().isEmpty()) {
            setErrorText("Username is required", usernameField);
            return false;
        }
        if (getPassword().isEmpty()) {
            setErrorText("Password is required", passwordField);
            return false;
        }
        if (getVault().isEmpty()) {
            setErrorText("Vault is required", vaultField);
            return false;
        }if (getSecurityContext().isEmpty()) {
            setErrorText("Vault is required", vaultField);
            return false;
        }
        setErrorText(null); // 清除错误信息
        return true;
    }

    public EnvironmentConfig getEnvironment() {
        if (environment == null) {
            return new EnvironmentConfig(getName(), getHostUrl(), getUsername(), getPassword(), getVault(), getSecurityContext());
        } else {
            environment.setHostUrl(getHostUrl());
            environment.setUser(getUsername());
            environment.setPassword(getPassword());
            environment.setVault(getVault());
            environment.setSecurityContext(getSecurityContext());
            return environment;
        }
    }

    public Runnable loadSecurityContext() {
        return () -> {
            securityContextComboBox.removeAllItems();
            LogonServer logonServer = new LogonServer(getHostUrl(), getUsername(), getPassword(), getVault(), "", true);
            try (Context context = logonServer.connect()) {
                String res = Util.execMQL(context, "list person '" + getUsername() + "' select assignment dump");
                String[] values = res.split(",");
                List<String> valueList = new ArrayList<>(Arrays.asList(values));
                valueList = valueList.stream()
                        .filter(v -> v.startsWith("ctx::"))
                        .map(v -> v.substring(5))
                        .sorted(String::compareTo)
                        .toList();
                for (String value : valueList) {
                    securityContextComboBox.addItem(value);
                }
                securityContextComboBox.setItem(environment.getSecurityContext());
            } catch (Exception e) {
                UIUtil.showErrorNotification(project, "Spinner Config", e.getLocalizedMessage());
            }
        };
    }
}
