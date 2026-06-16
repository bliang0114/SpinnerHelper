package cn.github.spinner.ui;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.ConsoleFileManager;
import cn.github.spinner.util.UIUtil;
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
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

public class EnvironmentToolWindow extends SimpleToolWindowPanel {
    private final Project project;
    @Getter
    private Tree environmentTree;
    private DefaultTreeModel treeModel;
//    @Getter
//    private EnvironmentConfig environment;

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
        return scrollPane;
    }

    private void createEnvironmentTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("SpinnerHelper");
        treeModel = new DefaultTreeModel(root) {
            @Override
            public void valueForPathChanged(TreePath path, Object newValue) {
                renameConsoleNode(path, newValue);
            }
        };
        environmentTree = new Tree(treeModel);
        refreshTree();
        environmentTree.setRootVisible(false);
        environmentTree.setShowsRootHandles(true);
        environmentTree.setCellRenderer(new EnvironmentTreeCellRenderer(project));
        environmentTree.setCellEditor(new ConsoleTreeCellEditor());
        environmentTree.setEditable(true);
        environmentTree.setInvokesStopCellEditing(true);
        installConsoleRenameShortcut();
        environmentTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        environmentTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showConsolePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showConsolePopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2) {
                    return;
                }
                TreePath path = environmentTree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (selectedNode instanceof ConsoleTreeNode consoleNode) {
                    ConsoleFileManager.openConsole(project, consoleNode.getConsoleManager());
                }
            }
        });
        // 添加选择监听器
        environmentTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) environmentTree.getLastSelectedPathComponent();
            EnvironmentConfig environment;
            if (selectedNode instanceof EnvironmentTreeNode envNode) {
                environment = envNode.getEnvironment();
            } else if(selectedNode instanceof DetailTreeNode detailNode) {
                EnvironmentTreeNode envNode = (EnvironmentTreeNode) detailNode.getParent();
                environment = envNode.getEnvironment();
            } else {
                environment = null;
            }
            if (environment == null) {
                UserInput.getInstance().clickEnvironment.remove(project);
            } else {
                UserInput.getInstance().clickEnvironment.put(project, environment);
            }
        });
    }

    private void installConsoleRenameShortcut() {
        environmentTree.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "renameConsole");
        environmentTree.getActionMap().put("renameConsole", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startConsoleRename(environmentTree.getSelectionPath());
            }
        });
    }

    private void showConsolePopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        TreePath path = environmentTree.getPathForLocation(e.getX(), e.getY());
        if (!isConsolePath(path)) {
            return;
        }
        environmentTree.setSelectionPath(path);
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem(SpinnerBundle.message("action.rename.console.text"));
        renameItem.addActionListener(event -> startConsoleRename(path));
        popupMenu.add(renameItem);
        popupMenu.show(environmentTree, e.getX(), e.getY());
    }

    private void startConsoleRename(TreePath path) {
        if (!isConsolePath(path)) {
            return;
        }
        environmentTree.setSelectionPath(path);
        environmentTree.startEditingAtPath(path);
    }

    private boolean isConsolePath(TreePath path) {
        return path != null && path.getLastPathComponent() instanceof ConsoleTreeNode;
    }

    private void renameConsoleNode(TreePath path, Object newValue) {
        if (!isConsolePath(path)) {
            return;
        }
        ConsoleTreeNode consoleNode = (ConsoleTreeNode) path.getLastPathComponent();
        try {
            String newName = ConsoleFileManager.renameConsole(project, consoleNode.getConsoleManager(), String.valueOf(newValue));
            consoleNode.setUserObject(newName);
            treeModel.nodeChanged(consoleNode);
            selectConsole(newName);
        } catch (Exception e) {
            treeModel.nodeChanged(consoleNode);
            UIUtil.showErrorNotification(project,
                    SpinnerBundle.message("notification.title.mql.console"),
                    SpinnerBundle.message("message.console.rename.failed", e.getMessage()));
        }
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();

        EnvironmentGroupTreeNode environmentsRoot = new EnvironmentGroupTreeNode(SpinnerBundle.message("tree.environments"));
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<EnvironmentConfig> environments = spinnerSettings.getEnvironments();
        for (EnvironmentConfig env : environments) {
            EnvironmentTreeNode envNode = new EnvironmentTreeNode(env);
            environmentsRoot.add(envNode);
        }
        root.add(environmentsRoot);

        ConsoleGroupTreeNode consolesRoot = new ConsoleGroupTreeNode(SpinnerBundle.message("tree.consoles"));
        ConsoleFileManager.loadProjectConsoles(project)
                .forEach(consoleManager -> consolesRoot.add(new ConsoleTreeNode(consoleManager)));
        root.add(consolesRoot);
        treeModel.reload();
        // 展开所有节点
        for (int i = 0; i < environmentTree.getRowCount(); i++) {
            environmentTree.expandRow(i);
        }
    }

    public void selectConsole(String consoleName) {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            Enumeration<TreeNode> groups = root.children();
            while (groups.hasMoreElements()) {
                DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) groups.nextElement();
                if (!(groupNode instanceof ConsoleGroupTreeNode)) {
                    continue;
                }
                Enumeration<TreeNode> consoles = groupNode.children();
                while (consoles.hasMoreElements()) {
                    DefaultMutableTreeNode consoleNode = (DefaultMutableTreeNode) consoles.nextElement();
                    if (consoleNode instanceof ConsoleTreeNode treeNode
                            && consoleName.equals(treeNode.getConsoleManager().getConsoleName())) {
                        TreePath path = new TreePath(consoleNode.getPath());
                        environmentTree.expandPath(path.getParentPath());
                        environmentTree.setSelectionPath(path);
                        environmentTree.scrollPathToVisible(path);
                        return;
                    }
                }
            }
        });
    }

    private final class ConsoleTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {
        private final JTextField textField = new JTextField();

        @Override
        public Component getTreeCellEditorComponent(JTree tree,
                                                    Object value,
                                                    boolean isSelected,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row) {
            if (value instanceof ConsoleTreeNode consoleNode) {
                textField.setText(consoleNode.getConsoleManager().getConsoleName());
            } else {
                textField.setText(String.valueOf(value));
            }
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            return !(event instanceof MouseEvent) && isConsolePath(environmentTree.getSelectionPath());
        }
    }
}
