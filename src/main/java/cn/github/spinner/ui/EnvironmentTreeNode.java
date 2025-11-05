package cn.github.spinner.ui;

import cn.github.spinner.config.EnvironmentConfig;
import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;

@Getter
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

}

@Getter
class DetailTreeNode extends DefaultMutableTreeNode {
    private final String key;
    private final String value;

    public DetailTreeNode(String key, String value) {
        super(key + ": " + value);
        this.key = key;
        this.value = value;
    }

}
