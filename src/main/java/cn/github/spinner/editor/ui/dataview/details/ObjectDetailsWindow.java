package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class ObjectDetailsWindow extends JFrame {
    private final Project project;
    private final String id;
    private JBTabbedPane tabbedPane;

    public ObjectDetailsWindow(Project project, String id) {
        this.project = project;
        this.id = id;
        setSize(JBUI.size(1200, 800));
        setTitle(id);
        initComponents();
    }

    public void updateFrameTitle(String newTitle) {
        // 关键：所有 UI 操作必须在 EDT 线程执行，避免线程安全问题
        if (SwingUtilities.isEventDispatchThread()) {
            setTitle(newTitle); // 直接更新
        } else {
            SwingUtilities.invokeLater(() -> setTitle(newTitle)); // 切换到 EDT 线程
        }
    }

    public static void showWindow(Project project, String id) {
        ObjectDetailsWindow window = new ObjectDetailsWindow(project, id);
        window.setVisible(true);
        window.setLocationRelativeTo(null);
    }

    private void initComponents() {
        try {
            String result = MQLUtil.execute(project, "print bus {} select id", id);
            String[] array = result.split("\n");
            String title = array[0];
            title = title.replaceAll("business object {2}", "");
            updateFrameTitle(title);
        } catch (MQLException ignored) {
        }
        tabbedPane = new JBTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setTabComponentInsets(JBInsets.create(new Insets(0, 8, 0, 0)));
        tabbedPane.addChangeListener(e -> loadTabData());
        createCenterPanel();
    }

    protected void createCenterPanel() {
        tabbedPane.add("Details", new ObjectBasicInformationComponent(project, id));
        tabbedPane.add("History", new ObjectHistoryComponent(project, id));
        tabbedPane.add("Connections (bus)", new ObjectBusConnectionsComponent(project, id));
        tabbedPane.add("Connections (rel)", new ObjectRelConnectionsComponent(project, id));
        tabbedPane.add("Paths", new ObjectPathsComponent(project, id));
        tabbedPane.add("Files", new ObjectFilesComponent(project, id));
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
