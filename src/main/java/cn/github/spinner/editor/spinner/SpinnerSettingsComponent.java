package cn.github.spinner.editor.spinner;

import cn.github.spinner.components.ComboBoxWithFilter;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

public class SpinnerSettingsComponent extends JPanel {
    private final SpinnerType spinnerType;
    private final String settingName;
    private final String settingValue;
    private final Runnable onValueChanged;
    private DefaultActionGroup actionGroup;
    private List<ComboBoxWithFilter<String>> settingNameComponents;
    private List<ExpandableTextField> settingValueComponents;

    public SpinnerSettingsComponent(SpinnerType spinnerType, String settingName, String settingValue) {
        this(spinnerType, settingName, settingValue, null);
    }

    public SpinnerSettingsComponent(SpinnerType spinnerType, String settingName, String settingValue, Runnable onValueChanged) {
        this.spinnerType = spinnerType;
        this.settingName = settingName;
        this.settingValue = settingValue;
        this.onValueChanged = onValueChanged;
        initComponents();
        setupLayout();
    }

    private void initComponents() {
        String[] settingNames = this.settingName.split("(?<!\\|)\\|(?!\\|)");
        String[] settingValues = this.settingValue.split("(?<!\\|)\\|(?!\\|)");
        settingNameComponents =  new ArrayList<>(settingNames.length);
        settingValueComponents =  new ArrayList<>(settingNames.length);
        List<String> settingNameItems = SpinnerSettingNameConfig.getSettingNames(this.spinnerType);
        for (int i = 0; i < settingNames.length; i++) {
            settingNameComponents.add(createSettingNameComponent(settingNameItems, settingNames[i]));
            String value = i >= settingValues.length ? "" : settingValues[i];
            settingValueComponents.add(createSettingValueComponent(value));
        }
        actionGroup = new DefaultActionGroup();
        actionGroup.add(new AddSettingAction());
        actionGroup.add(new RemoveSettingAction());
        actionGroup.add(new CommentAction());
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.emptyInsets();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        for (int i = 0; i < settingNameComponents.size(); i++) {
            gbc.gridy = i;
            gbc.gridx = 0;
            gbc.weightx = 0.3;
            panel.add(settingNameComponents.get(i), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.6;
            panel.add(settingValueComponents.get(i), gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.1;
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner Setting.ActionGroup" + i, actionGroup, true);
            toolbar.setTargetComponent(panel);
            panel.add(toolbar.getComponent(), gbc);
        }
        gbc.gridy = settingNameComponents.size();
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
        getComponentValue(value, settingNameComponents);
        value.append("\t");
        getComponentValue(value, settingValueComponents);
        return value.toString();
    }

    private void getComponentValue(StringBuilder value, List<?> settingComponents) {
        for (Object component : settingComponents) {
            String text = "";
            if (component instanceof ExpandableTextField textField) {
                text = textField.getText();
            } else if (component instanceof ComboBoxWithFilter<?> comboBox) {
                text = String.valueOf(comboBox.getItem());
            }
            if (!text.isEmpty()) {
                value.append(text).append("|");
            }
        }
        if (!value.isEmpty()) {
            value.deleteCharAt(value.length() - 1);
        }
    }

    private ComboBoxWithFilter<String> createSettingNameComponent(List<String> settingNameItems, String value) {
        ComboBoxWithFilter<String> comboBox = new ComboBoxWithFilter<>(settingNameItems, value);
        comboBox.addActionListener(e -> notifyValueChanged());
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField textField) {
            textField.addActionListener(e -> notifyValueChanged());
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    notifyValueChanged();
                }
            });
        }
        return comboBox;
    }

    private ExpandableTextField createSettingValueComponent(String value) {
        ExpandableTextField textField = new ExpandableTextField();
        textField.setText(value);
        textField.addActionListener(e -> notifyValueChanged());
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                notifyValueChanged();
            }
        });
        return textField;
    }

    private void notifyValueChanged() {
        if (onValueChanged != null) {
            onValueChanged.run();
        }
    }

    public class AddSettingAction extends AnAction {
        public AddSettingAction() {
            super("Add Setting", "Add Setting", AllIcons.General.Add);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String place = e.getPlace();
            place = place.replace("Spinner Setting.ActionGroup", "");
            int index = Integer.parseInt(place);
            List<String> settingNameItems = SpinnerSettingNameConfig.getSettingNames(spinnerType);
            settingNameComponents.add(index + 1, createSettingNameComponent(settingNameItems, ""));
            settingValueComponents.add(index + 1, createSettingValueComponent(""));

            removeAll();
            setupLayout();
            revalidate();
            repaint();
            notifyValueChanged();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class RemoveSettingAction extends AnAction {
        public RemoveSettingAction() {
            super("Remove Setting", "Remove Setting", AllIcons.General.Remove);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String place = e.getPlace();
            place = place.replace("Spinner Setting.ActionGroup", "");
            int index = Integer.parseInt(place);
            settingNameComponents.remove(index);
            settingValueComponents.remove(index);

            removeAll();
            setupLayout();
            revalidate();
            repaint();
            notifyValueChanged();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            if (settingNameComponents.isEmpty() || settingNameComponents.size() == 1) {
                e.getPresentation().setEnabledAndVisible(false);
            }
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }

    public class CommentAction extends AnAction {
        public CommentAction() {
            super("Comment", "Comment", AllIcons.Actions.RefactoringBulb);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String place = e.getPlace();
            place = place.replace("Spinner Setting.ActionGroup", "");
            int index = Integer.parseInt(place);
            ComboBoxWithFilter<String> component = settingNameComponents.get(index);
            String text = component.getItem();
            if (text == null || text.isEmpty()) return;

            int originLength = text.length();
            text = text.replace("<<", "");
            text = text.replace(">>", "");
            if  (originLength == text.length()) {
                text = "<<" + text + ">>";
            }
            component.setItem(text);
            notifyValueChanged();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }
}
