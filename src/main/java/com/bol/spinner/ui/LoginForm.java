package com.bol.spinner.ui;

import com.bol.spinner.auth.LogonServer;
import com.bol.spinner.auth.SpinnerToken;
import com.bol.spinner.config.LoginConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.util.SpinnerNotifier;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import matrix.db.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class LoginForm {
    private JButton loginBtn;
    private JTextField urlField;
    private JTextField usernameField;
    private JTextField passwordField;
    private JPanel mainPanel;
    private JButton logoutBth;
    private JLabel urlLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JTextField roleField;
    private JLabel roleLabel;
    private JTextField envField;
    private JLabel envLabel;
    private JComboBox envComboBox;
    private JButton refreshBtn;
    SpinnerSettings spinnerSettings = ApplicationManager.getApplication().getService(SpinnerSettings.class);

    public LoginForm(Project project, ToolWindow toolWindow) {
        Map<String, LoginConfig> loginConfigs = spinnerSettings.getLoginConfigs();
        if(loginConfigs != null){
            for (String env : loginConfigs.keySet()) {
                envComboBox.addItem(env);
            }
            if(spinnerSettings.getLastLogin() != null && loginConfigs.containsKey(spinnerSettings.getLastLogin())){
                envComboBox.setSelectedItem(spinnerSettings.getLastLogin());
            }
        }
        loginBtn.addActionListener(e -> {
            if(SpinnerToken.context != null){
                SpinnerNotifier.showNotification(project, "已有账号登录","");
                return;
            }
            if(!checkLogin()){
                return;
            }
            String env = envField.getText();
            String url = urlField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = roleField.getText();
            Context context = null;
            try {
                LogonServer logonServer = new LogonServer(url, username, password, "eService Production", "", true);
                logonServer.setRole(role);
                context = logonServer.connect();
            } catch (Exception ex) {
                ex.printStackTrace();
                SpinnerNotifier.showNotification(project, "登录失败", ex.getLocalizedMessage());
            }
            SpinnerToken.setContext(context);
            if (context != null) {
                LoginConfig loginConfig = new LoginConfig(url, username, password, role);
                spinnerSettings.addLoginConfig(env, loginConfig);
                spinnerSettings.setLastLogin(env);
                disable();
                SpinnerNotifier.showNotification(project, "登录成功", "");
            }
        });
        logoutBth.addActionListener(e -> {
            SpinnerToken.closeContext();
            SpinnerToken.setContext(null);
            enable();
            SpinnerNotifier.showNotification(project, "登出成功", "");
        });
        envComboBox.addActionListener(e -> {
            if(envComboBox.getSelectedItem() == null || envComboBox.getSelectedItem().toString().isEmpty()){
                return;
            }
            LoginConfig loginConfig = spinnerSettings.getLoginConfig(envComboBox.getSelectedItem().toString());
            if(loginConfig != null){
                envField.setText(envComboBox.getSelectedItem().toString());
                urlField.setText(loginConfig.getUrl());
                usernameField.setText(loginConfig.getUsername());
                passwordField.setText(loginConfig.getPassword());
                roleField.setText(loginConfig.getRole());
            }
        });
        refreshBtn.setPreferredSize(new Dimension(30, 30));
        refreshBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                load();
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void load(){
        Map<String, LoginConfig> loginConfigs = spinnerSettings.getLoginConfigs();
        if(loginConfigs != null){
            envComboBox.removeAllItems();
            for (String env : loginConfigs.keySet()) {
                envComboBox.addItem(env);
            }
            if(spinnerSettings.getLastLogin() != null && loginConfigs.containsKey(spinnerSettings.getLastLogin())){
                envComboBox.setSelectedItem(spinnerSettings.getLastLogin());
            }
        }
    }

    public void disable(){
        envField.setEnabled(false);
        urlField.setEnabled(false);
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        roleField.setEnabled(false);
        loginBtn.setEnabled(false);
        envComboBox.setEnabled(false);
        logoutBth.setEnabled(true);
    }

    public void enable(){
        envField.setEnabled(true);
        urlField.setEnabled(true);
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        roleField.setEnabled(true);
        loginBtn.setEnabled(true);
        envComboBox.setEnabled(true);
        logoutBth.setEnabled(false);
    }

    public boolean checkLogin(){
        String env = envField.getText();
        if(env == null || env.isEmpty()){
            SpinnerNotifier.showNotification(null, "ENV is required", "");
            return false;
        }
        String url = urlField.getText();
        if(url == null || url.isEmpty()){
            SpinnerNotifier.showNotification(null, "URL is required", "");
            return false;
        }
        String username = usernameField.getText();
        if(username == null || username.isEmpty()){
            SpinnerNotifier.showNotification(null, "username is required", "");
            return false;
        }
        String password = passwordField.getText();
        if(password == null || password.isEmpty()){
            SpinnerNotifier.showNotification(null, "password is required", "");
            return false;
        }
        String role = roleField.getText();
        if(role == null || role.isEmpty()){
            SpinnerNotifier.showNotification(null, "role is required", "");
            return false;
        }
        return true;
    }

}
