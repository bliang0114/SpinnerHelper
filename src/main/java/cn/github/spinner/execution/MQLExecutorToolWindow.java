package cn.github.spinner.execution;

import cn.github.spinner.context.UserInput;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.ToolTipManager;
import javax.swing.tree.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;

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
        splitter.setDividerWidth(10);
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(new JPanel());
        return splitter;
    }

    private void createConsoleTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Consoles");
        treeModel = new DefaultTreeModel(root);
        consoleTree = new Tree(treeModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                TreePath path = getPathForLocation(event.getX(), event.getY());
                if (path == null) {
                    return null;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObject = node.getUserObject();
                if (userObject instanceof MQLExecutionEntry entry) {
                    return entry.success() ? null : entry.message();
                }
                return null;
            }
        };
        reloadConsoleTree();
        consoleTree.expandRow(0);
        consoleTree.setRootVisible(true);
        consoleTree.setShowsRootHandles(true);
        consoleTree.setCellRenderer(new ConsoleTreeCellRenderer());
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.setInitialDelay(150);
        toolTipManager.setReshowDelay(0);
        toolTipManager.registerComponent(consoleTree);
        consoleTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        consoleTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    TreePath path = consoleTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (!node.isRoot()) {
                        ConsoleManager consoleManager = getConsoleManager(node);
                        if (consoleManager == null || consoleManager.getConsoleFile() == null) return;

                        Object userObject = node.getUserObject();
                        if (userObject instanceof MQLExecutionEntry entry) {
                            new OpenFileDescriptor(project, consoleManager.getConsoleFile(), entry.lineNumber(), 0).navigate(true);
                            return;
                        }

                        FileEditorManager.getInstance(project).openFile(consoleManager.getConsoleFile(), true);
                    }
                }
            }
        });
        // 添加选择监听器
        consoleTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) consoleTree.getLastSelectedPathComponent();
            if (node == null) return;
            ConsoleManager consoleManager = getConsoleManager(node);
            if (consoleManager == null) {
                splitter.setSecondComponent(new JPanel());
                return;
            }
            splitter.setSecondComponent(consoleManager.getConsoleView().getComponent());
        });
    }

    public void reloadConsoleTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        Map<String, ConsoleManager> consoleMap = UserInput.getInstance().mqlConsole.get(project);
        if (consoleMap != null && !consoleMap.isEmpty()) {
            consoleMap.values().stream()
                    .sorted(Comparator.comparing(ConsoleManager::getConsoleName, String.CASE_INSENSITIVE_ORDER))
                    .map(consoleManager -> {
                        DefaultMutableTreeNode consoleNode = new DefaultMutableTreeNode(consoleManager.getConsoleName());
                        consoleManager.getExecutionEntries().forEach(entry -> consoleNode.add(new DefaultMutableTreeNode(entry)));
                        return consoleNode;
                    })
                    .forEach(root::add);
        }
        treeModel.reload();
        consoleTree.expandRow(0);
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
            treeModel.reload();
            consoleTree.expandRow(0);
        }
        selectNode(name);
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
            consoleTree.expandPath(treePath);
            consoleTree.setSelectionPath(treePath);
            consoleTree.scrollPathToVisible(treePath);
        });
    }

    private ConsoleManager getConsoleManager(@NotNull DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof String nodeName) {
            return UserInput.getInstance().getConsole(project, nodeName);
        }
        TreeNode parent = node.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            return getConsoleManager(parentNode);
        }
        return null;
    }
}
