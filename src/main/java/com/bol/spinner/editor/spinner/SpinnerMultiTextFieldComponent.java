package com.bol.spinner.editor.spinner;

import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SpinnerMultiTextFieldComponent extends JPanel {
    private final String header;
    private final String value;
    private final String separator;
    private DefaultActionGroup actionGroup;
    private List<JComponent> components;

    public SpinnerMultiTextFieldComponent(String header, String value) {
        this(header, value, "|");
    }

    public SpinnerMultiTextFieldComponent(String header, String value, String separator) {
        this.header = header;
        this.value = CharSequenceUtil.nullToEmpty(value);
        this.separator = separator;
        initComponents();
        setupLayout();
    }

    private void initComponents() {
        String[] values = this.value.split("[" + separator + "]");
        components =  new ArrayList<>(values.length);
        for (String s : values) {
            components.add(new JBTextField(s));
        }
        actionGroup = new DefaultActionGroup();
        actionGroup.add(new AddValueAction());
        actionGroup.add(new RemoveValueAction());
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.emptyInsets();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        for (int i = 0; i < components.size(); i++) {
            gbc.gridy = i;
            gbc.gridx = 0;
            gbc.weightx = 0.95;
            panel.add(components.get(i), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.05;
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner " + header + ".ActionGroup" + i, actionGroup, true);
            toolbar.setTargetComponent(panel);
            panel.add(toolbar.getComponent(), gbc);
        }
        gbc.gridy = components.size();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; // 垂直权重
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }

    public String getValue() {
        StringBuilder value = new StringBuilder();
        for (JComponent component : components) {
            JBTextField textField = (JBTextField) component;
            String text = textField.getText();
            if (!text.isEmpty()) {
                value.append(textField.getText()).append(separator);
            }
        }
        if (!value.isEmpty()) {
            value.deleteCharAt(value.length() - 1);
        }
        return value.toString();
    }

    public class AddValueAction extends AnAction {
        public AddValueAction() {
            super("Add Value", "Add Value", AllIcons.General.Add);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String place = e.getPlace();
            place = place.replace("Spinner " + header + ".ActionGroup", "");
            int index = Integer.parseInt(place);
            components.add(index + 1, new JBTextField());

            removeAll();
            setupLayout();
            revalidate();
            repaint();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class RemoveValueAction extends AnAction {
        public RemoveValueAction() {
            super("Remove Value", "Remove Value", AllIcons.General.Remove);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String place = e.getPlace();
            place = place.replace("Spinner " + header + ".ActionGroup", "");
            int index = Integer.parseInt(place);
            components.remove(index);

            removeAll();
            setupLayout();
            revalidate();
            repaint();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            if (components.isEmpty() || components.size() == 1) {
                e.getPresentation().setEnabledAndVisible(false);
            }
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }
}
