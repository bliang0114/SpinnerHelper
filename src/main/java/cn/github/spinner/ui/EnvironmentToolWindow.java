package cn.github.spinner.ui;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerSettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;

public class EnvironmentToolWindow extends SimpleToolWindowPanel {
    private final Project project;
    @Getter
    private Tree environmentTree;
    private DefaultTreeModel treeModel;
    @Getter
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
        JComponent toolbar = createAdvancedToolbar();
        setToolbar(toolbar);
        // 创建内容区域
        JComponent content = createTreeContent();
        setContent(content);
    }

    private JComponent createAdvancedToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        // 左侧操作按钮
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionManager.getAction("Spinner Config.Toolbar");
        ActionToolbar leftToolbar = actionManager.createActionToolbar("SpinnerConfigToolbar", actionGroup, true);
        leftToolbar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        leftToolbar.setTargetComponent(this);

        // 右侧操作按钮
        ActionGroup rightActionGroup = (ActionGroup) actionManager.getAction("Spinner Config.RightToolbar");
        ActionToolbar rightToolbar = actionManager.createActionToolbar("SpinnerConfigRightToolbar", rightActionGroup, true);
        rightToolbar.setTargetComponent(this);
        rightToolbar.setLayoutStrategy(ToolbarLayoutStrategy.NOWRAP_STRATEGY);

        toolbarPanel.add(leftToolbar.getComponent(), BorderLayout.WEST);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
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
        environmentTree.setCellRenderer(new EnvironmentTreeCellRenderer(project));
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
}
