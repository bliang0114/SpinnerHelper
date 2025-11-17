package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.hutool.core.text.CharSequenceUtil;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.util.MQLUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RelationshipDataViewComponent extends JBPanel<RelationshipDataViewComponent> implements Disposable {
    private final Project project;
    private final VirtualFile virtualFile;
    private SearchTextField searchTextField;
    private DefaultListModel<String> listModel;
    private JBList<String> uiList;
    private DefaultActionGroup uiListToolbarGroup;
    private JBTabbedPane tabbedPane;
    private final List<String> rowList = new ArrayList<>();
    private final ScheduledExecutorService executor;

    public RelationshipDataViewComponent(@NotNull Project project, VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
        executor = Executors.newSingleThreadScheduledExecutor();
        initComponents();
        setupListener();
        setupLayout();
        loadRelationship();
    }

    private void initComponents() {
        tabbedPane = new JBTabbedPane(JTabbedPane.RIGHT);
        tabbedPane.setTabComponentInsets(JBInsets.create(new Insets(0, 8, 0, 0)));

        searchTextField = new SearchTextField(true);
        searchTextField.setHistorySize(10);
        listModel = new DefaultListModel<>();
        uiList = new JBList<>(listModel);
        uiList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        uiListToolbarGroup = new DefaultActionGroup();
        uiListToolbarGroup.add(new RefreshAction());
    }

    private void setupListener() {
        searchTextField.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterRelationship();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterRelationship();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterRelationship();
            }
        });
        uiList.addListSelectionListener(this::loadTypeInformation);
        tabbedPane.addChangeListener(e -> loadTabData());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        searchTextField.setPreferredSize(JBUI.size(180, 30));

        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        toolbarPanel.add(searchTextField, BorderLayout.WEST);
        ActionToolbar uiListToolbar = ActionManager.getInstance().createActionToolbar("Relationship UI List Toolbar", uiListToolbarGroup, true);
        uiListToolbar.setTargetComponent(uiList);
        toolbarPanel.add(uiListToolbar.getComponent(), BorderLayout.EAST);

        JPanel uiListPanel = new JPanel(new BorderLayout());
        uiListPanel.setPreferredSize(JBUI.size(210, -1));
        uiListPanel.add(toolbarPanel, BorderLayout.NORTH);
        uiListPanel.add(ScrollPaneFactory.createScrollPane(uiList), BorderLayout.CENTER);
        add(uiListPanel, BorderLayout.WEST);

        tabbedPane.add("Attributes", new AttributesTableComponent(project, virtualFile));
        tabbedPane.add("Properties", new PropertiesTableComponent(project, virtualFile));
        tabbedPane.add("Interfaces", new InterfacesTableComponent(project, virtualFile));
        tabbedPane.add("Relations", new RelationsTableComponent(project, virtualFile));
        tabbedPane.add("Triggers", new TriggersTableComponent(project, virtualFile));
        tabbedPane.add("Connections", new ConnectionsTableComponent(project, virtualFile));
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadRelationship() {
        rowList.clear();
        listModel.clear();
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null) {
            uiList.setEmptyText("Connection is closed");
            return;
        }
        uiList.setEmptyText("Loading Matrix Relationship...");
        executor.schedule(() -> {
            try {
                var result = MQLUtil.execute(project, "list relationship");
                List<String> allRelationships = CharSequenceUtil.split(result, "\n");
                rowList.addAll(allRelationships.stream().filter(CharSequenceUtil::isNotBlank).sorted(String.CASE_INSENSITIVE_ORDER).toList());
                listModel.addAll(rowList);
                uiList.setEmptyText("Nothing to show");
            } catch (MQLException e) {
                uiList.setEmptyText(e.getLocalizedMessage());
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private void filterRelationship() {
        String text = searchTextField.getText();
        List<String> filterList = rowList.stream().filter(item -> CharSequenceUtil.startWithIgnoreCase(item, text)).toList();
        listModel.removeAllElements();
        listModel.addAll(filterList);
    }

    private void loadTypeInformation(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;

        int tabCount = tabbedPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (component instanceof AbstractDataViewTableComponent<?> tableComponent) {
                tableComponent.setLoaded(false);
            }
        }
        loadTabData();
    }

    private void loadTabData() {
        int selectedIndex = uiList.getSelectedIndex();
        if (selectedIndex < 0) return;

        String relationship = listModel.elementAt(selectedIndex);
        int tabIndex = tabbedPane.getSelectedIndex();
        Component component = tabbedPane.getComponentAt(tabIndex);
        if (component instanceof AbstractDataViewTableComponent<?> dataViewTableComponent) {
            log.info("Loading relationship data for {}", relationship);
            dataViewTableComponent.setName(relationship);
            dataViewTableComponent.reloadData();
        }
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
    }

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            searchTextField.reset();
            searchTextField.setText("");
            loadRelationship();
        }
    }
}
