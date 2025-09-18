package com.bol.spinner.ui;

import com.bol.spinner.config.EnvironmentConfig;

import javax.swing.tree.DefaultMutableTreeNode;

public class EnvironmentTreeNode extends DefaultMutableTreeNode {
    private final EnvironmentConfig environment;

    public EnvironmentTreeNode(EnvironmentConfig environment) {
        super(environment.getName());
        this.environment = environment;

        // 添加详细信息作为子节点
        add(new DetailTreeNode("Host URL", environment.getHostUrl()));
        add(new DetailTreeNode("User", environment.getUser()));
        add(new DetailTreeNode("Vault", environment.getVault()));
        add(new DetailTreeNode("Security Context", environment.getSecurityContext()));
    }

    public EnvironmentConfig getEnvironment() {
        return environment;
    }
}

class DetailTreeNode extends DefaultMutableTreeNode {
    private final String key;
    private final String value;

    public DetailTreeNode(String key, String value) {
        super(key + ": " + value);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
