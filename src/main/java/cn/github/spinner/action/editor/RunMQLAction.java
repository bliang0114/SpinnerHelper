package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.task.ExecuteMQLCommand;
import cn.github.spinner.task.MQLCommandEntry;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.EditorUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RunMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Please connect to a matrix server first.");
            return;
        }
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || editor.getVirtualFile() == null) return;

        String consoleName = editor.getVirtualFile().getName();
        String selectedText = EditorUtil.getSelectedText(editor);
        log.info("Selected Text: {}", selectedText);
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<MQLCommandEntry> commandList = StrUtil.isNotEmpty(selectedText)
                ? getSelectedCommandEntries(editor, spinnerSettings.getLineDelimiter())
                : getCurrentLineCommandEntries(editor, spinnerSettings.getLineDelimiter());
        log.info("commandList: {}", commandList);
        if (!commandList.isEmpty()) {
            ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
            if (consoleManager == null) {
                consoleManager = new ConsoleManager(project, consoleName, editor.getVirtualFile());
                UserInput.getInstance().putConsole(project, consoleName, consoleManager);
            }
            MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(project);
            if (toolWindow != null) {
                toolWindow.addNodeToTree(consoleName);
            }
            ExecuteMQLCommand executeMQLCommand = new ExecuteMQLCommand(project, consoleName, commandList);
            executeMQLCommand.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        e.getPresentation().setEnabled(connection != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private List<MQLCommandEntry> getSelectedCommandEntries(@NotNull Editor editor, @NotNull String lineDelimiter) {
        List<MQLCommandEntry> entries = new ArrayList<>();
        int startLine = editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionStart());
        int endLine = editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionEnd());
        for (int line = startLine; line <= endLine; line++) {
            addCommandEntries(entries, line, EditorUtil.getLineContent(editor, line), lineDelimiter);
        }
        return entries;
    }

    private List<MQLCommandEntry> getCurrentLineCommandEntries(@NotNull Editor editor, @NotNull String lineDelimiter) {
        List<MQLCommandEntry> entries = new ArrayList<>();
        int line = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
        addCommandEntries(entries, line, EditorUtil.getLineContent(editor), lineDelimiter);
        return entries;
    }

    private void addCommandEntries(@NotNull List<MQLCommandEntry> entries,
                                   int lineNumber,
                                   @NotNull String rawText,
                                   @NotNull String lineDelimiter) {
        for (String command : rawText.split(lineDelimiter)) {
            String normalized = command.replaceAll("\n", " ").trim();
            if (!normalized.isEmpty() && !normalized.startsWith("#")) {
                entries.add(new MQLCommandEntry(lineNumber, normalized));
            }
        }
    }
}
