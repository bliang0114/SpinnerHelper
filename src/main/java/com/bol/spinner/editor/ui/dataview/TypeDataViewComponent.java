package com.bol.spinner.editor.ui.dataview;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TypeDataViewComponent extends JPanel implements Disposable {
    private SearchTextField searchTextField;
    private DefaultListModel<String> typeListModel;
    private JBList<String> typeUIList;
    private DefaultActionGroup typeUIListToolbarGroup;
    private JBTabbedPane tabbedPane;
    private AttributesTableComponent attributesTableComponent;

    public TypeDataViewComponent() {
        initComponents();
        setupLayout();
    }

    private void initComponents() {
        tabbedPane = new JBTabbedPane(JTabbedPane.RIGHT);
        tabbedPane.setTabComponentInsets(JBInsets.create(0, 0));
        attributesTableComponent = new AttributesTableComponent();

        searchTextField = new SearchTextField(false);
        searchTextField.setHistorySize(10);
        typeListModel = new DefaultListModel<>();
        typeUIList = new JBList<>(typeListModel);
        typeUIList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        typeUIListToolbarGroup = new  DefaultActionGroup();
        typeUIListToolbarGroup.add(new TypeRefreshAction());

        for (int i = 0; i < 1000; i++) {
            typeListModel.addElement("SAAAAAAAAAA" + i);
        }
//        searchTextField.addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//
//            }
//        });
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        searchTextField.setPreferredSize(JBUI.size(160, 30));

        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        toolbarPanel.add(searchTextField, BorderLayout.WEST);
        ActionToolbar typeUIListToolbar = ActionManager.getInstance().createActionToolbar("Type UI List Toolbar", typeUIListToolbarGroup, true);
        typeUIListToolbar.setTargetComponent(typeUIList);
        toolbarPanel.add(typeUIListToolbar.getComponent(), BorderLayout.EAST);

        JPanel typeUIListPanel = new JPanel(new BorderLayout());
        typeUIListPanel.setPreferredSize(JBUI.size(200, -1));
        typeUIListPanel.add(toolbarPanel, BorderLayout.NORTH);
        typeUIListPanel.add(ScrollPaneFactory.createScrollPane(typeUIList), BorderLayout.CENTER);
        add(typeUIListPanel, BorderLayout.WEST);

        tabbedPane.add("Attributes", attributesTableComponent);
        tabbedPane.add("Properties", new JPanel());
        tabbedPane.add("Interfaces", new JPanel());
        tabbedPane.add("Relations", new JPanel());
        tabbedPane.add("Triggers", new JPanel());
        tabbedPane.add("Policy Triggers", new JPanel());
        tabbedPane.add("Objects", new JPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {

    }

    public static class TypeRefreshAction extends AnAction {
        public TypeRefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        }
    }
}
