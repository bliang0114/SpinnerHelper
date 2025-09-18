package com.bol.spinner.config;

import com.intellij.util.xmlb.annotations.Transient;

public class EnvironmentConfig {
    private String name;
    private String hostUrl;
    private String user;
    private String password;
    private String vault;
    private String securityContext;
    @Transient
    private transient boolean connected = false;

    public EnvironmentConfig() {
    }

    public EnvironmentConfig(String name, String hostUrl, String user, String password, String vault, String securityContext) {
        this.name = name;
        this.hostUrl = hostUrl;
        this.user = user;
        this.password = password;
        this.vault = vault;
        this.securityContext = securityContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVault() {
        return vault;
    }

    public void setVault(String vault) {
        this.vault = vault;
    }

    public String getSecurityContext() {
        return securityContext;
    }

    public void setSecurityContext(String securityContext) {
        this.securityContext = securityContext;
    }

    @Transient
    public boolean isConnected() {
        return connected;
    }

    @Transient
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EnvironmentConfig that = (EnvironmentConfig) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Transient
    public String getRole() {
        return "ctx::" + securityContext;
    }

    public void update(EnvironmentConfig environment) {
        this.hostUrl = environment.hostUrl;
        this.user = environment.user;
        this.password = environment.password;
        this.vault = environment.vault;
        this.securityContext = environment.securityContext;
    }
}
