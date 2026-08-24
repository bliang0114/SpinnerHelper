package cn.github.spinner.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.swing.*;
import java.awt.*;

public class MQLSplitFileEditorTest extends BasePlatformTestCase {
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
