package cn.github.spinner.editor;

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
            JButton minimize = findButton(root, "最小化", "Minimize");
            JButton maximize = findButton(root, "最大化", "Maximize");
            JButton reset = findButton(root, "重置", "Reset");

            assertNotNull(minimize);
            assertNotNull(maximize);
            assertNotNull(reset);
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

    private static JButton findButton(Container container, String... labels) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) {
                for (String label : labels) {
                    if (label.equals(button.getText())) {
                        return button;
                    }
                }
            }
            if (component instanceof Container childContainer) {
                JButton result = findButton(childContainer, labels);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
