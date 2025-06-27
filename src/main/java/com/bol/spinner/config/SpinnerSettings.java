package com.bol.spinner.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
@State(name="SpinnerSettings", storages = @Storage("spinnerSettingsConfig.xml"))
public final class SpinnerSettings implements PersistentStateComponent<SpinnerSettings> {

    private Map<String, LoginConfig> loginConfigs;

    private String lastLogin;

    public Map<String, LoginConfig> getLoginConfigs() {
        return loginConfigs;
    }

    public void setLoginConfigs(Map<String, LoginConfig> loginConfigs) {
        this.loginConfigs = loginConfigs;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public static SpinnerSettings getInstance(Project project){
        return project.getService(SpinnerSettings.class);
    }
    @Override
    public @Nullable SpinnerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SpinnerSettings spinnerSettings) {
        XmlSerializerUtil.copyBean(spinnerSettings, this);
    }

    public void addLoginConfig(String name, LoginConfig loginConfig){
        if(loginConfigs == null){
            loginConfigs = new LinkedHashMap<>();
        }
        loginConfigs.put(name, loginConfig);
    }

    public LoginConfig getLoginConfig(String name){
        if(loginConfigs == null){
            return null;
        }
        return loginConfigs.get(name);
    }
}
