package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.task.ExecuteMQLCommand;
import cn.github.spinner.task.MQLCommandEntry;
import cn.github.spinner.ui.MQLPlaceholderInputDialog;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RunMQLAction extends AnAction {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$(\\d+)");

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
        if (commandList.isEmpty()) {
            return;
        }

        List<String> placeholders = collectPlaceholders(commandList);
        if (placeholders.isEmpty()) {
            executeCommands(project, editor.getVirtualFile().getName(), editor.getVirtualFile(), commandList);
            return;
        }

        MQLPlaceholderInputDialog dialog = new MQLPlaceholderInputDialog(
                project,
                placeholders,
                values -> {
                    List<MQLCommandEntry> resolvedCommands = applyPlaceholderValues(commandList, values);
                    executeCommands(project, editor.getVirtualFile().getName(), editor.getVirtualFile(), resolvedCommands);
                }
        );
        dialog.show();
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
            int lineStartOffset = editor.getDocument().getLineStartOffset(line);
            addCommandEntries(entries, line, lineStartOffset, EditorUtil.getLineContent(editor, line), lineDelimiter);
        }
        return entries;
    }

    private List<MQLCommandEntry> getCurrentLineCommandEntries(@NotNull Editor editor, @NotNull String lineDelimiter) {
        List<MQLCommandEntry> entries = new ArrayList<>();
        int line = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
        int lineStartOffset = editor.getDocument().getLineStartOffset(line);
        addCommandEntries(entries, line, lineStartOffset, EditorUtil.getLineContent(editor), lineDelimiter);
        return entries;
    }

    private void addCommandEntries(@NotNull List<MQLCommandEntry> entries,
                                   int lineNumber,
                                   int baseOffset,
                                   @NotNull String rawText,
                                   @NotNull String lineDelimiter) {
        Pattern pattern = Pattern.compile(lineDelimiter);
        Matcher matcher = pattern.matcher(rawText);
        int segmentStart = 0;
        while (matcher.find()) {
            addCommandEntry(entries, lineNumber, baseOffset, rawText, segmentStart, matcher.start());
            segmentStart = matcher.end();
        }
        addCommandEntry(entries, lineNumber, baseOffset, rawText, segmentStart, rawText.length());
    }

    private void addCommandEntry(@NotNull List<MQLCommandEntry> entries,
                                 int lineNumber,
                                 int baseOffset,
                                 @NotNull String rawText,
                                 int segmentStart,
                                 int segmentEnd) {
        if (segmentStart > segmentEnd) {
            return;
        }
        String command = rawText.substring(segmentStart, segmentEnd);
        String normalized = command.replace('\n', ' ').trim();
        if (normalized.isEmpty() || normalized.startsWith("#")) {
            return;
        }

        int leadingWhitespace = 0;
        while (leadingWhitespace < command.length() && Character.isWhitespace(command.charAt(leadingWhitespace))) {
            leadingWhitespace++;
        }

        int trailingWhitespace = 0;
        while (trailingWhitespace < command.length()
                && Character.isWhitespace(command.charAt(command.length() - 1 - trailingWhitespace))) {
            trailingWhitespace++;
        }

        int sourceStartOffset = baseOffset + segmentStart + leadingWhitespace;
        int sourceEndOffset = baseOffset + segmentEnd - trailingWhitespace;
        entries.add(new MQLCommandEntry(lineNumber, sourceStartOffset, sourceEndOffset, normalized));
    }

    private void executeCommands(@NotNull Project project,
                                 @NotNull String consoleName,
                                 @NotNull com.intellij.openapi.vfs.VirtualFile virtualFile,
                                 @NotNull List<MQLCommandEntry> commandList) {
        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
        if (consoleManager == null) {
            consoleManager = new ConsoleManager(project, consoleName, virtualFile);
            UserInput.getInstance().putConsole(project, consoleName, consoleManager);
        }
        ExecuteMQLCommand executeMQLCommand = new ExecuteMQLCommand(project, consoleName, commandList);
        executeMQLCommand.queue();
    }

    private @NotNull List<String> collectPlaceholders(@NotNull List<MQLCommandEntry> commandList) {
        TreeSet<Integer> indexes = new TreeSet<>();
        for (MQLCommandEntry entry : commandList) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(entry.command());
            while (matcher.find()) {
                indexes.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return indexes.stream().map(index -> "$" + index).toList();
    }

    private @NotNull List<MQLCommandEntry> applyPlaceholderValues(@NotNull List<MQLCommandEntry> commandList,
                                                                  @NotNull Map<String, String> values) {
        List<Map.Entry<String, String>> orderedEntries = new ArrayList<>(new LinkedHashMap<>(values).entrySet());
        orderedEntries.sort(Comparator.comparingInt((Map.Entry<String, String> entry) ->
                Integer.parseInt(entry.getKey().substring(1))
        ).reversed());

        List<MQLCommandEntry> resolvedEntries = new ArrayList<>(commandList.size());
        for (MQLCommandEntry entry : commandList) {
            String resolvedCommand = entry.command();
            for (Map.Entry<String, String> valueEntry : orderedEntries) {
                resolvedCommand = resolvedCommand.replace(valueEntry.getKey(), valueEntry.getValue());
            }
            resolvedEntries.add(new MQLCommandEntry(
                    entry.lineNumber(),
                    entry.sourceStartOffset(),
                    entry.sourceEndOffset(),
                    resolvedCommand
            ));
        }
        return resolvedEntries;
    }
}
