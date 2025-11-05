package cn.github.spinner.editor.spinner;

import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SpinnerTextFieldComponent extends JPanel {
    private final String header;
    private final String value;
    private DefaultActionGroup actionGroup;
    private JBTextField textField;

    public SpinnerTextFieldComponent(String header, String value) {
        this.header = header;
        this.value = CharSequenceUtil.nullToEmpty(value);
        initComponents();
        setupLayout();
    }

    private void initComponents() {
        textField = new JBTextField(this.value);
        actionGroup = new DefaultActionGroup();
        actionGroup.add(new CommentAction());
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.emptyInsets();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.95;
        panel.add(textField, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.05;
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Spinner " + header + ".ActionGroup", actionGroup, true);
        toolbar.setTargetComponent(panel);
        panel.add(toolbar.getComponent(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; // 垂直权重
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }

    public String getValue() {
        return textField.getText();
    }

    public class CommentAction extends AnAction {
        public CommentAction() {
            super("Comment", "Comment", AllIcons.Actions.RefactoringBulb);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            String text = textField.getText();
            if (text == null || text.isEmpty()) return;

            int originLength = text.length();
            text = text.replace("<<", "");
            text = text.replace(">>", "");
            if  (originLength == text.length()) {
                text = "<<" + text + ">>";
            }
            textField.setText(text);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return super.getActionUpdateThread();
        }
    }
}
