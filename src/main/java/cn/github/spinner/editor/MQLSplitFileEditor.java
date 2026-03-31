package cn.github.spinner.editor;

import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutionEntry;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

public class MQLSplitFileEditor extends UserDataHolderBase implements TextEditor {
    private final VirtualFile file;
    private final TextEditor textEditor;
    private final ConsoleManager consoleManager;
    private final JBSplitter splitter;
    private final CaretListener caretListener;

    public MQLSplitFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        this.file = file;
        this.textEditor = (TextEditor) TextEditorProvider.getInstance().createEditor(project, file);
        this.consoleManager = UserInput.getInstance().getConsole(project, file.getName()) != null
                ? UserInput.getInstance().getConsole(project, file.getName())
                : createConsoleManager(project, file);
        this.splitter = createSplitter();
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

    private JBSplitter createSplitter() {
        JBSplitter result = new JBSplitter(false, 0.65f);
        result.setDividerWidth(1);
        result.setFirstComponent(textEditor.getComponent());
        result.setSecondComponent(createResultPanel());
        return result;
    }

    private @NotNull JComponent createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Execution Result");
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        headerPanel.add(title, BorderLayout.WEST);

        JCheckBox wrapToggle = new JCheckBox("Wrap");
        wrapToggle.setOpaque(false);
        wrapToggle.setSelected(consoleManager.isSoftWrapsEnabled());
        wrapToggle.addActionListener(e -> consoleManager.setSoftWrapsEnabled(wrapToggle.isSelected()));
        headerPanel.add(wrapToggle, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(consoleManager.getResultComponent(), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return splitter;
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
}
