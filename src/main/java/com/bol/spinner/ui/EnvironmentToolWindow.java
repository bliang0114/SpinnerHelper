package com.bol.spinner.ui;

import com.bol.spinner.config.EnvironmentConfig;
import com.bol.spinner.config.SpinnerSettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;

public class EnvironmentToolWindow extends SimpleToolWindowPanel{
    private final Project project;
    private Tree environmentTree;
    private DefaultTreeModel treeModel;
    private EnvironmentConfig environment;

    public EnvironmentToolWindow(@NotNull Project project) {
        super(true, true);
        this.project = project;
        createContentPanel();
    }

    private void createContentPanel() {
        // 创建配置树
        createEnvironmentTree();
        // 创建工具栏
        JComponent toolbar = createToolbar();
        setToolbar(toolbar);
        // 创建内容区域
        JComponent content = createTreeContent();
        setContent(content);
    }

    private JComponent createToolbar() {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("Spinner Config.Toolbar");
        ActionToolbar toolbar = actionManager.createActionToolbar("SpinnerConfigToolbar", actionGroup, false);
        toolbar.setTargetComponent(this);
        return toolbar.getComponent();
    }

    private JComponent createTreeContent() {
        JBScrollPane scrollPane = new JBScrollPane(environmentTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return environmentTree;
    }

    private void createEnvironmentTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Environments");
        treeModel = new DefaultTreeModel(root);
        environmentTree = new Tree(treeModel);
        refreshTree();
        environmentTree.setRootVisible(true);
        environmentTree.setShowsRootHandles(true);
        environmentTree.setCellRenderer(new EnvironmentTreeCellRenderer());
        environmentTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // 添加选择监听器
        environmentTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) environmentTree.getLastSelectedPathComponent();
            if (selectedNode instanceof EnvironmentTreeNode envNode) {
                environment = envNode.getEnvironment();
            } else if(selectedNode instanceof DetailTreeNode detailNode) {
                EnvironmentTreeNode envNode = (EnvironmentTreeNode) detailNode.getParent();
                environment = envNode.getEnvironment();
            } else {
                environment = null;
            }
        });
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<EnvironmentConfig> environments = spinnerSettings.getEnvironments();
        for (EnvironmentConfig env : environments) {
            EnvironmentTreeNode envNode = new EnvironmentTreeNode(env);
            root.add(envNode);
        }
        treeModel.reload();
        // 展开所有节点
        for (int i = 0; i < environmentTree.getRowCount(); i++) {
            environmentTree.expandRow(i);
        }
    }

    public EnvironmentConfig getEnvironment() {
        return environment;
    }
}
