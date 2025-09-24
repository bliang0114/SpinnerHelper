package com.bol.spinner.config;

import com.intellij.util.xmlb.annotations.Transient;
import lombok.Data;

@Data
public class EnvironmentConfig {
    private String name;
    private String hostUrl;
    private String user;
    private String password;
    private String vault;
    private String securityContext;
    private String driver;
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

    public EnvironmentConfig(String name, String hostUrl, String user, String password, String vault, String securityContext, String driver) {
        this.name = name;
        this.hostUrl = hostUrl;
        this.user = user;
        this.password = password;
        this.vault = vault;
        this.securityContext = securityContext;
        this.driver = driver;
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
        this.driver = environment.driver;
    }
}
