package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class ConnectionDetailsWindow extends JFrame {
    private final Project project;
    private final String id;
    private JBTabbedPane tabbedPane;

    public ConnectionDetailsWindow(Project project, String id) {
        this.project = project;
        this.id = id;
        setSize(JBUI.size(1200, 800));
        setTitle("Connections of " + id);
        initComponents();
    }

    public static void showWindow(Project project, String id) {
        ConnectionDetailsWindow window = new ConnectionDetailsWindow(project, id);
        window.setVisible(true);
        window.setLocationRelativeTo(null);
    }

    private void initComponents() {
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setTabComponentInsets(JBInsets.create(new Insets(0, 8, 0, 0)));
        tabbedPane.addChangeListener(e -> loadTabData());
        createCenterPanel();
    }

    protected void createCenterPanel() {
        tabbedPane.add("Details", new ConnectionBasicInformationComponent(project, id));
        try {
            String result = MQLUtil.execute(project, "print connection {} select from.id fromrel.id to.id torel.id", id);
            String[] array = result.split("\n");
            for (int i = 1; i < array.length; i++) {
                String item = array[i];
                String[] attribute = item.split(" = ");
                String attributeName = attribute[0];
                attributeName = attributeName.replaceAll(" {4}", "");
                String attributeValue = attribute.length > 1 ? attribute[1] : "";
                if (CharSequenceUtil.isNotBlank(attributeValue)) {
                    switch (attributeName) {
                        case "from.id" -> tabbedPane.add("From", new ObjectBasicInformationComponent(project, attributeValue));
                        case "fromrel.id" -> tabbedPane.add("From Rel", new ConnectionBasicInformationComponent(project, attributeValue));
                        case "to.id" -> tabbedPane.add("To", new ObjectBasicInformationComponent(project, attributeValue));
                        case "torel.id" -> tabbedPane.add("To Rel", new ConnectionBasicInformationComponent(project, attributeValue));
                    }
                }
            }
        } catch (MQLException ignored) {
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
    }

    private void loadTabData() {
        int tabIndex = tabbedPane.getSelectedIndex();
        Component component = tabbedPane.getComponentAt(tabIndex);
        if (component instanceof AbstractObjectDetailsTableComponent tableComponent) {
            tableComponent.reload();
        } else if (component instanceof ObjectPathsComponent pathsComponent) {
            pathsComponent.reload();
        }
    }
}
