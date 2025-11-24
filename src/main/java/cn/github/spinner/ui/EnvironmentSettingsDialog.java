package cn.github.spinner.ui;

import cn.github.driver.MQLException;
import cn.github.driver.MatrixDriverManager;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.MatrixDriversConfig;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.util.MatrixJarClassLoader;
import cn.github.spinner.util.UIUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.io.File;
import java.util.*;

@Slf4j
public class EnvironmentSettingsDialog extends DialogWrapper {
    private final Project project;
    private final EnvironmentConfig environment;
    private final JBTextField environmentField;
    private final JBTextField hostUrlField;
    private final JBTextField usernameField;
    private final JBPasswordField passwordField;
    private final JBTextField vaultField;
    private final ComboBox<String> securityContextComboBox;
    private final ComboBox<String> driverComboBox;

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
                        AllIcons.Actions.Refresh,
                        AllIcons.Actions.Refresh,
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
        driverComboBox = new ComboBox<>();
        driverComboBox.setEditable(false);
        Map<String, MatrixDriversConfig.DriverInfo> driversMap = MatrixDriversConfig.getInstance().getDriversMap();
        driversMap.keySet().forEach(driverComboBox::addItem);
        init();
        if (environment != null) {
            environmentField.setEnabled(false);
            environmentField.setText(environment.getName());
            hostUrlField.setText(environment.getHostUrl());
            usernameField.setText(environment.getUser());
            passwordField.setText(environment.getPassword());
            vaultField.setText(environment.getVault());
            securityContextComboBox.setItem(environment.getSecurityContext());
            driverComboBox.setItem(environment.getDriver());
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Environment:", environmentField)
                .addTooltip("Enter a unique name for this environment")
                .addLabeledComponent("Driver:", driverComboBox)
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

    public void setValue(EnvironmentConfig environment) {
        hostUrlField.setText(environment.getHostUrl());
        usernameField.setText(environment.getUser());
        passwordField.setText(environment.getPassword());
        vaultField.setText(environment.getVault());
        securityContextComboBox.setItem(environment.getSecurityContext());
        driverComboBox.setItem(environment.getDriver());
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
        return Optional.ofNullable(securityContextComboBox.getItem()).map(String::trim).orElse("");
    }

    private String getDriver() {
        return Optional.ofNullable(driverComboBox.getItem()).map(String::trim).orElse("");
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
        }
        if (getSecurityContext().isEmpty()) {
            setErrorText("Security Context is required", securityContextComboBox);
            return false;
        }
        if (getDriver().isEmpty()) {
            setErrorText("Driver is required", driverComboBox);
            return false;
        }
        setErrorText(null); // 清除错误信息
        return true;
    }

    public EnvironmentConfig getEnvironment() {
        if (environment == null) {
            return new EnvironmentConfig(getName(), getHostUrl(), getUsername(), getPassword(), getVault(), getSecurityContext(), getDriver());
        } else {
            environment.setHostUrl(getHostUrl());
            environment.setUser(getUsername());
            environment.setPassword(getPassword());
            environment.setVault(getVault());
            environment.setSecurityContext(getSecurityContext());
            environment.setDriver(getDriver());
            return environment;
        }
    }

    public Runnable loadSecurityContext() {
        return () -> {
            MatrixDriversConfig.DriverInfo driverInfo = MatrixDriversConfig.getInstance().putDriver(getDriver());
            if (driverInfo == null || driverInfo.getDriverClass() == null || driverInfo.getDriverClass().isEmpty()) {
                return;
            }
            securityContextComboBox.removeAllItems();
            List<File> driverFiles = MatrixDriversConfig.getInstance().getDriverFiles(getDriver());
            try (MatrixJarClassLoader classLoader = new MatrixJarClassLoader(driverFiles, this.getClass().getClassLoader())) {
                Class.forName(driverInfo.getDriverClass(), true, classLoader);
                MatrixConnection connection = MatrixDriverManager.getConnection(getHostUrl(), getUsername(), getPassword(), getVault(), classLoader);
                MatrixStatement statement = connection.executeStatement("list person '" + getUsername() + "' select assignment dump");
                MatrixResultSet resultSet = statement.executeQuery();
                if (!resultSet.isSuccess()) {
                    throw new MQLException(resultSet.getMessage());
                }
                String[] values = resultSet.getResult().split(",");
                List<String> valueList = new ArrayList<>(Arrays.asList(values));
                valueList = valueList.stream()
                        .filter(v -> v.startsWith("ctx::"))
                        .map(v -> v.substring(5))
                        .sorted(String::compareTo)
                        .toList();
                for (String value : valueList) {
                    securityContextComboBox.addItem(value);
                }
                if (environment != null) {
                    securityContextComboBox.setItem(environment.getSecurityContext());
                }
            } catch (ClassNotFoundException e) {
                UIUtil.showErrorNotification(project, "Spinner Environment", "Load Driver Error<br/>" + getDriver());
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
                UIUtil.showErrorNotification(project, "Spinner Environment", "Get Security Context Error<br/>" + e.getLocalizedMessage());
            }
        };
    }
}
