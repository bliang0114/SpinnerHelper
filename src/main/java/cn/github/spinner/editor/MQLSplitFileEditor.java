package cn.github.spinner.editor;

import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutionEntry;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

public class MQLSplitFileEditor extends UserDataHolderBase implements TextEditor {
    private final VirtualFile file;
    private final TextEditor textEditor;
    private final ConsoleManager consoleManager;
    private final SpinnerSettings spinnerSettings;
    private final JPanel rootPanel;
    private final JPanel editorContainer;
    private final JPanel resultContainer;
    private final JComponent resultPanel;
    private final JComponent resultContentComponent;
    private final CaretListener caretListener;
    private JBSplitter splitter;
    private ResultPosition dragPreviewPosition;

    public MQLSplitFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.file = file;
        this.textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
        this.spinnerSettings = SpinnerSettings.getInstance(project);
        this.consoleManager = UserInput.getInstance().getConsole(project, file.getName()) != null
                ? UserInput.getInstance().getConsole(project, file.getName())
                : createConsoleManager(project, file);
        this.rootPanel = new JPanel(new BorderLayout());
        this.editorContainer = wrapComponent(textEditor.getComponent());
        this.resultContentComponent = consoleManager.createResultComponent();
        this.resultPanel = createResultPanel();
        this.resultContainer = wrapComponent(resultPanel);
        this.rootPanel.setBorder(BorderFactory.createEmptyBorder());
        this.rootPanel.add(createEditorToolbar(), BorderLayout.NORTH);
        applyResultPosition(ResultPosition.from(spinnerSettings.getMqlResultPosition()));
        this.caretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                scrollResultToCurrentStatement();
            }
        };
        this.textEditor.getEditor().getCaretModel().addCaretListener(caretListener);
    }

    private ConsoleManager createConsoleManager(@NotNull Project project, @NotNull VirtualFile file) {
        ConsoleManager manager = new ConsoleManager(project, file.getName(), file);
        UserInput.getInstance().putConsole(project, manager.getConsoleName(), manager);
        return manager;
    }

    private @NotNull JPanel wrapComponent(@NotNull JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JComponent createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("Execution Result");
        title.setToolTipText("Drag to dock to top, left, bottom, or right");
        title.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        headerPanel.add(title, BorderLayout.WEST);

        DockDragHandler dockDragHandler = new DockDragHandler(title);
        title.addMouseListener(dockDragHandler);
        title.addMouseMotionListener(dockDragHandler);

        JCheckBox wrapToggle = new JCheckBox("Wrap");
        wrapToggle.setOpaque(false);
        wrapToggle.setSelected(consoleManager.isSoftWrapsEnabled());
        wrapToggle.addActionListener(e -> consoleManager.setSoftWrapsEnabled(wrapToggle.isSelected()));

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(wrapToggle);
        headerPanel.add(actionsPanel, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(resultContentComponent, BorderLayout.CENTER);
        return panel;
    }

    private @NotNull JComponent createEditorToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        ActionManager actionManager = ActionManager.getInstance();

        DefaultActionGroup leftActionGroup = new DefaultActionGroup();
        addActionIfPresent(leftActionGroup, actionManager.getAction("MQL Editor.Run"));
        addActionIfPresent(leftActionGroup, actionManager.getAction("MQL Editor.LoadDefinition"));
        addActionIfPresent(leftActionGroup, actionManager.getAction("MQL Editor.SaveAs"));

        ActionToolbar leftToolbar = actionManager.createActionToolbar("MQLEditorToolbar", leftActionGroup, true);
        leftToolbar.setLayoutStrategy(ToolbarLayoutStrategy.AUTOLAYOUT_STRATEGY);
        leftToolbar.setTargetComponent(textEditor.getPreferredFocusedComponent());

        DefaultActionGroup rightActionGroup = new DefaultActionGroup();
        addActionIfPresent(rightActionGroup, actionManager.getAction("MQL Editor.Settings"));

        ActionToolbar rightToolbar = actionManager.createActionToolbar("MQLEditorRightToolbar", rightActionGroup, true);
        rightToolbar.setLayoutStrategy(ToolbarLayoutStrategy.NOWRAP_STRATEGY);
        rightToolbar.setTargetComponent(textEditor.getPreferredFocusedComponent());

        toolbarPanel.add(leftToolbar.getComponent(), BorderLayout.WEST);
        toolbarPanel.add(rightToolbar.getComponent(), BorderLayout.EAST);
        return toolbarPanel;
    }

    private void addActionIfPresent(@NotNull DefaultActionGroup actionGroup, @Nullable AnAction action) {
        if (action != null) {
            actionGroup.add(action);
        }
    }

    private void applyResultPosition(@NotNull ResultPosition position) {
        JBSplitter newSplitter = new JBSplitter(position.verticalSplit, position.proportion);
        newSplitter.setDividerWidth(1);
        if (position.resultFirst) {
            newSplitter.setFirstComponent(resultContainer);
            newSplitter.setSecondComponent(editorContainer);
        } else {
            newSplitter.setFirstComponent(editorContainer);
            newSplitter.setSecondComponent(resultContainer);
        }

        if (splitter != null) {
            rootPanel.remove(splitter);
        }
        splitter = newSplitter;
        rootPanel.add(splitter, BorderLayout.CENTER);
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private void applyDockPreview(@Nullable ResultPosition position) {
        if (position == null) {
            rootPanel.setBorder(BorderFactory.createEmptyBorder());
            rootPanel.repaint();
            return;
        }
        Color color = JBColor.namedColor("Component.accentColor", new JBColor(0x4C89FF, 0x4C89FF));
        Border border = switch (position) {
            case TOP -> BorderFactory.createMatteBorder(3, 0, 0, 0, color);
            case LEFT -> BorderFactory.createMatteBorder(0, 3, 0, 0, color);
            case BOTTOM -> BorderFactory.createMatteBorder(0, 0, 3, 0, color);
            case RIGHT -> BorderFactory.createMatteBorder(0, 0, 0, 3, color);
        };
        rootPanel.setBorder(border);
        rootPanel.repaint();
    }

    private @NotNull ResultPosition resolveDockPosition(@NotNull Point pointOnRootPanel) {
        int width = Math.max(rootPanel.getWidth(), 1);
        int height = Math.max(rootPanel.getHeight(), 1);
        int leftDistance = Math.abs(pointOnRootPanel.x);
        int rightDistance = Math.abs(width - pointOnRootPanel.x);
        int topDistance = Math.abs(pointOnRootPanel.y);
        int bottomDistance = Math.abs(height - pointOnRootPanel.y);

        ResultPosition position = ResultPosition.LEFT;
        int bestDistance = leftDistance;
        if (rightDistance < bestDistance) {
            position = ResultPosition.RIGHT;
            bestDistance = rightDistance;
        }
        if (topDistance < bestDistance) {
            position = ResultPosition.TOP;
            bestDistance = topDistance;
        }
        if (bottomDistance < bestDistance) {
            position = ResultPosition.BOTTOM;
        }
        return position;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return rootPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return textEditor.getPreferredFocusedComponent();
    }

    @Override
    public @NotNull Editor getEditor() {
        return textEditor.getEditor();
    }

    @Override
    public boolean canNavigateTo(@NotNull Navigatable navigatable) {
        return textEditor.canNavigateTo(navigatable);
    }

    @Override
    public void navigateTo(@NotNull Navigatable navigatable) {
        textEditor.navigateTo(navigatable);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return textEditor.getName();
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        textEditor.setState(state);
    }

    @Override
    public boolean isModified() {
        return textEditor.isModified();
    }

    @Override
    public boolean isValid() {
        return textEditor.isValid();
    }

    @Override
    public void selectNotify() {
        textEditor.selectNotify();
        scrollResultToCurrentStatement();
    }

    @Override
    public void deselectNotify() {
        textEditor.deselectNotify();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        textEditor.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        textEditor.removePropertyChangeListener(listener);
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return textEditor.getCurrentLocation();
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public void dispose() {
        applyDockPreview(null);
        consoleManager.releaseResultComponent(resultContentComponent);
        textEditor.getEditor().getCaretModel().removeCaretListener(caretListener);
        TextEditorProvider.getInstance().disposeEditor(textEditor);
    }

    private void scrollResultToCurrentStatement() {
        int sourceOffset = textEditor.getEditor().getCaretModel().getOffset();
        int lineNumber = textEditor.getEditor().getDocument().getLineNumber(sourceOffset);
        MQLExecutionEntry entry = consoleManager.findExecutionEntry(sourceOffset, lineNumber);
        if (entry != null) {
            consoleManager.scrollToExecutionEntry(entry);
        }
    }

    private enum ResultPosition {
        TOP("Top", true, true, 0.35f),
        LEFT("Left", false, true, 0.35f),
        BOTTOM("Bottom", true, false, 0.65f),
        RIGHT("Right", false, false, 0.65f);

        private final String displayName;
        private final boolean verticalSplit;
        private final boolean resultFirst;
        private final float proportion;

        ResultPosition(String displayName, boolean verticalSplit, boolean resultFirst, float proportion) {
            this.displayName = displayName;
            this.verticalSplit = verticalSplit;
            this.resultFirst = resultFirst;
            this.proportion = proportion;
        }

        static @NotNull ResultPosition from(@Nullable String value) {
            if (value == null || value.isBlank()) {
                return RIGHT;
            }
            try {
                return ResultPosition.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return RIGHT;
            }
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final class DockDragHandler extends MouseAdapter {
        private final JComponent dragSource;
        private boolean dragging;
        private final AWTEventListener eventListener = event -> {
            if (!(event instanceof MouseEvent mouseEvent) || !dragging) {
                return;
            }
            if (mouseEvent.getID() == MouseEvent.MOUSE_DRAGGED) {
                updatePreview(mouseEvent);
            } else if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                finishDragging(mouseEvent);
            }
        };

        private DockDragHandler(@NotNull JComponent dragSource) {
            this.dragSource = dragSource;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            dragging = true;
            Toolkit.getDefaultToolkit().addAWTEventListener(
                    eventListener,
                    AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
            );
            updatePreview(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                finishDragging(e);
            }
        }

        private void updatePreview(@NotNull MouseEvent event) {
            Point pointOnRoot = convertToRootPoint(event);
            if (pointOnRoot == null) {
                return;
            }
            dragPreviewPosition = resolveDockPosition(pointOnRoot);
            applyDockPreview(dragPreviewPosition);
        }

        private void finishDragging(@NotNull MouseEvent event) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
            dragging = false;
            Point pointOnRoot = convertToRootPoint(event);
            ResultPosition targetPosition = pointOnRoot != null
                    ? resolveDockPosition(pointOnRoot)
                    : dragPreviewPosition != null ? dragPreviewPosition : ResultPosition.from(spinnerSettings.getMqlResultPosition());
            dragPreviewPosition = null;
            applyDockPreview(null);
            spinnerSettings.setMqlResultPosition(targetPosition.name());
            applyResultPosition(targetPosition);
        }

        private @Nullable Point convertToRootPoint(@NotNull MouseEvent event) {
            Component sourceComponent = event.getComponent();
            if (sourceComponent == null) {
                return null;
            }
            Window sourceWindow = SwingUtilities.getWindowAncestor(sourceComponent);
            Window targetWindow = SwingUtilities.getWindowAncestor(dragSource);
            if (sourceWindow != targetWindow) {
                return null;
            }
            return SwingUtilities.convertPoint(sourceComponent, event.getPoint(), rootPanel);
        }
    }
}
