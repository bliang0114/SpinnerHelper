package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ConnectionDetailsWindow extends JFrame {
    private final Project project;
    private final String id;
    private JBTabbedPane tabbedPane;

    public ConnectionDetailsWindow(Project project, String id) {
        this.project = project;
        this.id = id;
        setSize(JBUI.size(1200, 800));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(SpinnerBundle.message("dialog.connections.of.title", id));
        initComponents();
    }

    public static void showWindow(Project project, String id) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showWindow(project, id));
            return;
        }
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
        tabbedPane.add(SpinnerBundle.message("tab.details"), new ConnectionBasicInformationComponent(project, id));
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        loadEndpoints();
    }

    private void loadEndpoints() {
        new TrackedBackgroundTask(project, SpinnerBundle.message("message.loading.data"), true) {
            private final List<Endpoint> endpoints = new ArrayList<>();

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String result = MQLUtil.execute(project,
                            "print connection {} select from.id fromrel.id to.id torel.id", id);
                    String[] values = result.split("\n");
                    for (int i = 1; i < values.length; i++) {
                        String[] attribute = values[i].split(" = ");
                        String attributeValue = attribute.length > 1 ? attribute[1] : "";
                        if (CharSequenceUtil.isNotBlank(attributeValue)) {
                            endpoints.add(new Endpoint(attribute[0].replaceAll(" {4}", ""), attributeValue));
                        }
                    }
                } catch (MQLException ignored) {
                }
            }

            @Override
            public void onSuccess() {
                if (!isDisplayable()) {
                    return;
                }
                for (Endpoint endpoint : endpoints) {
                    switch (endpoint.attributeName()) {
                        case "from.id" -> tabbedPane.add(SpinnerBundle.message("tab.from"),
                                new ObjectBasicInformationComponent(project, endpoint.id()));
                        case "fromrel.id" -> tabbedPane.add(SpinnerBundle.message("tab.from.rel"),
                                new ConnectionBasicInformationComponent(project, endpoint.id()));
                        case "to.id" -> tabbedPane.add(SpinnerBundle.message("tab.to"),
                                new ObjectBasicInformationComponent(project, endpoint.id()));
                        case "torel.id" -> tabbedPane.add(SpinnerBundle.message("tab.to.rel"),
                                new ConnectionBasicInformationComponent(project, endpoint.id()));
                    }
                }
            }
        }.queue();
    }

    private record Endpoint(String attributeName, String id) {
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
