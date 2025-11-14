package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.spinner.editor.ui.dataview.details.ObjectBasicInformationComponent;
import cn.github.spinner.editor.ui.dataview.details.ObjectHistoryComponent;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class ObjectDetailsDialog extends DialogWrapper {
    private final Project project;
    private final String id;
    private JBTabbedPane tabbedPane;

    public ObjectDetailsDialog(Project project, String id) {
        super(true);
        this.project = project;
        this.id = id;
        setOKActionEnabled(false);
        setSize(1200, 800);
        initComponents();
        init();
    }

    private void initComponents() {
        try {
            String result = MQLUtil.execute(project, "print bus {} select id", id);
            String[] array = result.split("\n");
            String title = array[0];
            title = title.replaceAll("business object {2}", "");
            setTitle(title);
        } catch (MQLException e) {
            setTitle(id);
        }
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setTabComponentInsets(JBInsets.create(new Insets(0, 8, 0, 0)));
        tabbedPane.addChangeListener(e -> loadTabData());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        tabbedPane.add("Details", new ObjectBasicInformationComponent(project, id));
        tabbedPane.add("History", new ObjectHistoryComponent(project, id));
        tabbedPane.add("Connections", new JBLabel("Not Provided"));
        tabbedPane.add("Paths", new JBLabel("Not Provided"));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadTabData() {
        int tabIndex = tabbedPane.getSelectedIndex();
        Component component = tabbedPane.getComponentAt(tabIndex);
        if (component instanceof ObjectBasicInformationComponent basicInformationComponent) {
            basicInformationComponent.reload();
        } else if (component instanceof ObjectHistoryComponent historyComponent) {
            historyComponent.reload();
        }
    }
}
