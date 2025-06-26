package com.bol.spinner.config;

public class LoginConfig {
    private String url;

    private String username;

    private String password;

    private String role;

    public LoginConfig() {
    }

    public LoginConfig(String url, String username, String password, String role) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
