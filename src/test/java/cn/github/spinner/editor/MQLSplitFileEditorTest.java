package cn.github.spinner.editor;

import com.intellij.icons.AllIcons;
import cn.github.spinner.components.EnvironmentIndicator;
import cn.github.spinner.components.FilterTable;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLResultTabbedPane;
import cn.github.spinner.execution.MQLResultViewData;
import cn.github.spinner.util.ConsoleFileManager;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.treeStructure.treetable.TreeTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class MQLSplitFileEditorTest extends BasePlatformTestCase {
    public void testDefaultResultViewFillsItsTabContent() {
        JPanel defaultView = new JPanel();
        MQLResultTabbedPane tabs = new MQLResultTabbedPane(defaultView, MQLResultViewData.defaultView());

        assertTrue(tabs.getComponentAt(0) instanceof JPanel);
        JPanel fillContainer = (JPanel) tabs.getComponentAt(0);
        assertTrue(fillContainer.getLayout() instanceof BorderLayout);
        assertSame(defaultView, ((BorderLayout) fillContainer.getLayout()).getLayoutComponent(BorderLayout.CENTER));

        fillContainer.setSize(800, 600);
        fillContainer.doLayout();
        assertEquals(new Rectangle(0, 0, 800, 600), defaultView.getBounds());
    }

    public void testRepeatedResultUpdatesDoNotDetachOrShrinkDefaultView() {
        JPanel defaultView = new JPanel();
        MQLResultTabbedPane tabs = new MQLResultTabbedPane(defaultView, MQLResultViewData.defaultView());
        Component defaultTab = tabs.getComponentAt(0);
        AtomicInteger defaultTabRemovals = new AtomicInteger();
        tabs.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent event) {
                if (event.getChild() == defaultTab) {
                    defaultTabRemovals.incrementAndGet();
                }
            }
        });

        for (int i = 0; i < 10; i++) {
            tabs.updateResult(MQLResultViewData.defaultView());
            tabs.updateResult(new MQLResultViewData(
                    MQLResultViewData.Style.TABLE,
                    java.util.List.of("id", "name"),
                    java.util.List.of(java.util.List.of(String.valueOf(i), "value"))
            ));
        }

        assertSame(defaultTab, tabs.getComponentAt(0));
        assertEquals(0, defaultTabRemovals.get());
        assertEquals(2, tabs.getTabCount());
        Component structuredTab = tabs.getComponentAt(1);
        assertNotNull(findComponent((Container) structuredTab, FilterTable.class));
        assertTrue(structuredTab instanceof JPanel);
        JPanel structuredFillContainer = (JPanel) structuredTab;
        assertTrue(structuredFillContainer.getLayout() instanceof BorderLayout);
        Component structuredContent = ((BorderLayout) structuredFillContainer.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        assertNotNull(structuredContent);
        structuredFillContainer.setSize(800, 600);
        structuredFillContainer.doLayout();
        assertEquals(new Rectangle(0, 0, 800, 600), structuredContent.getBounds());
    }

    public void testResultHeaderControlsChangeAndResetTheLayout() {
        VirtualFile file = myFixture.configureByText(MQLFileType.INSTANCE, "list type;").getVirtualFile();
        MQLSplitFileEditor splitEditor = new MQLSplitFileEditor(getProject(), file);
        try {
            JComponent root = splitEditor.getComponent();
            JComponent sourceEditor = splitEditor.getEditor().getComponent();
            JButton minimize = findButtonByTooltip(root, "最小化", "Minimize");
            JButton maximize = findButtonByTooltip(root, "最大化", "Maximize");
            JButton reset = findButtonByTooltip(root, "重置", "Reset");

            assertNotNull(minimize);
            assertNotNull(maximize);
            assertNotNull(reset);
            assertNotNull(minimize.getIcon());
            assertNotNull(maximize.getIcon());
            assertNotNull(reset.getIcon());
            assertSame(AllIcons.Windows.Minimize, minimize.getIcon());
            assertSame(AllIcons.Windows.Maximize, maximize.getIcon());
            assertSame(AllIcons.Windows.Restore, reset.getIcon());
            assertTrue(minimize.getText() == null || minimize.getText().isEmpty());
            assertTrue(maximize.getText() == null || maximize.getText().isEmpty());
            assertTrue(reset.getText() == null || reset.getText().isEmpty());
            assertNull(findComponent(root, JCheckBox.class));
            assertNotNull(findComponent(root, EnvironmentIndicator.class));
            assertTrue(SwingUtilities.isDescendingFrom(sourceEditor, root));

            maximize.doClick();
            assertFalse(SwingUtilities.isDescendingFrom(sourceEditor, root));

            reset.doClick();
            assertTrue(SwingUtilities.isDescendingFrom(sourceEditor, root));

            minimize.doClick();
            assertTrue(SwingUtilities.isDescendingFrom(sourceEditor, root));
            assertTrue(SwingUtilities.isDescendingFrom(reset, root));
        } finally {
            splitEditor.dispose();
        }
    }

    public void testStructuredResultTabsFollowTheLatestResultShape() {
        VirtualFile file = myFixture.configureByText(MQLFileType.INSTANCE, "print bus Product A 1;").getVirtualFile();
        MQLSplitFileEditor splitEditor = new MQLSplitFileEditor(getProject(), file);
        try {
            MQLResultTabbedPane tabs = findComponent(splitEditor.getComponent(), MQLResultTabbedPane.class);
            assertNotNull(tabs);
            assertEquals(1, tabs.getTabCount());
            assertTabPresent(tabs, "默认", "Default");

            ConsoleManager consoleManager = UserInput.getInstance().getConsole(
                    getProject(),
                    ConsoleFileManager.getConsoleName(getProject(), file)
            );
            assertNotNull(consoleManager);

            consoleManager.showStructuredResult(
                    "print bus Product A 1",
                    "business object Product A 1\nvault = Main\nowner = creator"
            );
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            assertEquals(2, tabs.getTabCount());
            assertTabPresent(tabs, "表单", "Form");
            assertNotNull(findComponent((Container) tabs.getComponentAt(1), FilterTable.class));

            consoleManager.showStructuredResult(
                    "temp query bus * * * select id dump |",
                    "Part|A|1|1001\nPart|B|1|1002"
            );
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            assertEquals(2, tabs.getTabCount());
            assertTabPresent(tabs, "表格", "Table");
            assertTabAbsent(tabs, "表单", "Form");
            assertNotNull(findComponent((Container) tabs.getComponentAt(1), FilterTable.class));

            consoleManager.showStructuredResult(
                    "expand bus VPMReference A0ST1013 A.1 from rel VPMInstance recurse to all "
                            + "select bus attribute.value dump ||",
                    "1||VPMInstance||to||VPMReference||B0X01022||A.1||root\n"
                            + "2||VPMInstance||to||VPMReference||B0X01024||A.1||child"
            );
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            assertEquals(2, tabs.getTabCount());
            assertTabPresent(tabs, "树形表格", "Tree Table");
            TreeTable treeTable = findComponent((Container) tabs.getComponentAt(1), TreeTable.class);
            assertNotNull(treeTable);
            assertEquals(2, treeTable.getRowCount());

            consoleManager.showStructuredResult("list type", "Part\nDocument");
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            assertEquals(1, tabs.getTabCount());
            assertTabPresent(tabs, "默认", "Default");
        } finally {
            splitEditor.dispose();
        }
    }

    private static void assertTabPresent(JTabbedPane tabs, String... titles) {
        assertTrue(findTab(tabs, titles) >= 0);
    }

    private static void assertTabAbsent(JTabbedPane tabs, String... titles) {
        assertTrue(findTab(tabs, titles) < 0);
    }

    private static int findTab(JTabbedPane tabs, String... titles) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            for (String title : titles) {
                if (title.equals(tabs.getTitleAt(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static JButton findButtonByTooltip(Container container, String... labels) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                for (String label : labels) {
                    if (label.equals(button.getToolTipText())) {
                        return button;
                    }
                }
            }
            if (component instanceof Container childContainer) {
                JButton result = findButtonByTooltip(childContainer, labels);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(Container container, Class<T> componentType) {
        for (Component component : container.getComponents()) {
            if (componentType.isInstance(component)) {
                return componentType.cast(component);
            }
            if (component instanceof Container childContainer) {
                T result = findComponent(childContainer, componentType);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
