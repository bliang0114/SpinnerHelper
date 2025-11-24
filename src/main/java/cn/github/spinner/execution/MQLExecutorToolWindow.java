package cn.github.spinner.execution;

import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

public class MQLExecutorToolWindow extends SimpleToolWindowPanel {
    private final Project project;
    private Tree consoleTree;
    private DefaultTreeModel treeModel;
    private JBSplitter splitter;

    public MQLExecutorToolWindow(@NotNull Project project) {
        super(true, true);
        this.project = project;
        createContentPanel();
    }

    private void createContentPanel() {
        // 创建配置树
        createConsoleTree();
        // 创建内容区域
        JComponent content = createTreeContent();
        setContent(content);
    }

    private JComponent createTreeContent() {
        splitter = new JBSplitter();
        splitter.setProportion(0.3f);
        JBScrollPane scrollPane = new JBScrollPane(consoleTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(new JPanel());
        return splitter;
    }

    private void createConsoleTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Consoles");
        treeModel = new DefaultTreeModel(root);
        consoleTree = new Tree(treeModel);
        addNodeToTree(SpinnerToken.DEFAULT_MQL_CONSOLE);
        consoleTree.expandRow(0);
        consoleTree.setRootVisible(true);
        consoleTree.setShowsRootHandles(true);
        consoleTree.setCellRenderer(new ConsoleTreeCellRenderer());
        consoleTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        consoleTree.addMouseListener(new  MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    TreePath path = consoleTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (!node.isRoot()) {
                        String nodeName = String.valueOf(node.getUserObject());
                        LightVirtualFile consoleFile = SpinnerToken.getMQLConsoleFile(project, nodeName);
                        if (consoleFile == null) return;

                        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                        fileEditorManager.openFile(consoleFile, true);
                    }
                }
            }
        });
        // 添加选择监听器
        consoleTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) consoleTree.getLastSelectedPathComponent();
            if (node == null) return;
            String nodeName = String.valueOf(node.getUserObject());
            ConsoleManager consoleManager = SpinnerToken.getConsoleManager(project, nodeName);
            if (consoleManager == null) {
                splitter.setSecondComponent(new JPanel());
                return;
            }

            splitter.setSecondComponent(consoleManager.getConsoleView().getComponent());
        });
    }

    public void addNodeToTree(String name) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        boolean existed = false;
        Enumeration<TreeNode> children = root.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            String existNodeName = String.valueOf(child.getUserObject());
            if (existNodeName.equals(name)) {
                existed = true;
                break;
            }
        }
        if (!existed) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(name);
            root.add(treeNode);
            SpinnerToken.putConsoleManager(project, name, new ConsoleManager(project));
            treeModel.reload();
            consoleTree.expandRow(0);
        }
    }

    public void selectNode(String name) {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            TreePath treePath = new TreePath(root.getPath());
            Enumeration<TreeNode> children = root.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                String existNodeName = String.valueOf(child.getUserObject());
                if (existNodeName.equals(name)) {
                    treePath = treePath.pathByAddingChild(child);
                    break;
                }
            }
            consoleTree.setSelectionPath(treePath);
            consoleTree.scrollPathToVisible(treePath);
        });
    }
}
