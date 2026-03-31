package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MQLFileType;
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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
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

        VirtualFile sourceFile = editor.getVirtualFile();
        boolean isMqlFile = sourceFile.getFileType() == MQLFileType.INSTANCE;
        String consoleName = sourceFile.getFileType() == MQLFileType.INSTANCE
                ? sourceFile.getName()
                : UserInput.DEFAULT_MQL_CONSOLE;
        String selectedText = EditorUtil.getSelectedText(editor);
        log.info("Selected Text: {}", selectedText);
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        List<MQLCommandEntry> commandList;
        if (!isMqlFile) {
            if (StrUtil.isEmpty(selectedText)) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Please select the MQL statement to execute.");
                return;
            }
            commandList = getSelectedTextCommandEntries(editor, spinnerSettings.getLineDelimiter());
        } else {
            commandList = StrUtil.isNotEmpty(selectedText)
                    ? getSelectedCommandEntries(editor, spinnerSettings.getLineDelimiter())
                    : getCurrentLineCommandEntries(editor, spinnerSettings.getLineDelimiter());
        }
        log.info("commandList: {}", commandList);
        if (commandList.isEmpty()) {
            return;
        }

        List<String> placeholders = collectPlaceholders(commandList);
        if (placeholders.isEmpty()) {
            executeCommands(project, consoleName, sourceFile, commandList);
            return;
        }

        MQLPlaceholderInputDialog dialog = new MQLPlaceholderInputDialog(
                project,
                placeholders,
                values -> {
                    List<MQLCommandEntry> resolvedCommands = applyPlaceholderValues(commandList, values);
                    executeCommands(project, consoleName, sourceFile, resolvedCommands);
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

    private List<MQLCommandEntry> getSelectedTextCommandEntries(@NotNull Editor editor, @NotNull String lineDelimiter) {
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        int selectionEnd = editor.getSelectionModel().getSelectionEnd();
        if (selectionEnd <= selectionStart) {
            return List.of();
        }
        String selectedText = editor.getDocument().getText(new TextRange(selectionStart, selectionEnd));
        return getCommandEntriesFromText(editor, selectionStart, selectedText, lineDelimiter);
    }

    private List<MQLCommandEntry> getCurrentLineCommandEntries(@NotNull Editor editor, @NotNull String lineDelimiter) {
        List<MQLCommandEntry> entries = new ArrayList<>();
        int line = editor.getCaretModel().getCurrentCaret().getLogicalPosition().line;
        int lineStartOffset = editor.getDocument().getLineStartOffset(line);
        addCommandEntries(entries, line, lineStartOffset, EditorUtil.getLineContent(editor), lineDelimiter);
        return entries;
    }

    private List<MQLCommandEntry> getCommandEntriesFromText(@NotNull Editor editor,
                                                            int baseOffset,
                                                            @NotNull String rawText,
                                                            @NotNull String lineDelimiter) {
        List<MQLCommandEntry> entries = new ArrayList<>();
        Pattern pattern = Pattern.compile(lineDelimiter);
        Matcher matcher = pattern.matcher(rawText);
        int segmentStart = 0;
        while (matcher.find()) {
            addCommandEntry(entries, editor, baseOffset, rawText, segmentStart, matcher.start());
            segmentStart = matcher.end();
        }
        addCommandEntry(entries, editor, baseOffset, rawText, segmentStart, rawText.length());
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
                                 @NotNull Editor editor,
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
        int lineNumber = editor.getDocument().getLineNumber(sourceStartOffset);
        entries.add(new MQLCommandEntry(lineNumber, sourceStartOffset, sourceEndOffset, normalized));
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
        ConsoleManager consoleManager;
        if (virtualFile.getFileType() == MQLFileType.INSTANCE) {
            consoleManager = UserInput.getInstance().getConsole(project, consoleName);
            if (consoleManager == null) {
                consoleManager = new ConsoleManager(project, consoleName, virtualFile);
                UserInput.getInstance().putConsole(project, consoleName, consoleManager);
            }
        } else {
            consoleManager = ensureDefaultConsole(project);
            commandList = appendCommandsToDefaultConsole(project, consoleManager, commandList);
            consoleName = consoleManager.getConsoleName();
        }
        ExecuteMQLCommand executeMQLCommand = new ExecuteMQLCommand(project, consoleName, commandList);
        executeMQLCommand.queue();
    }

    private @NotNull ConsoleManager ensureDefaultConsole(@NotNull Project project) {
        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, UserInput.DEFAULT_MQL_CONSOLE);
        if (consoleManager == null) {
            LightVirtualFile consoleFile = new LightVirtualFile(UserInput.DEFAULT_MQL_CONSOLE);
            consoleFile.setLanguage(cn.github.spinner.editor.MQLLanguage.INSTANCE);
            consoleFile.setWritable(true);
            consoleFile.setCharset(StandardCharsets.UTF_8);
            consoleManager = new ConsoleManager(project, UserInput.DEFAULT_MQL_CONSOLE, consoleFile);
            UserInput.getInstance().putConsole(project, consoleManager.getConsoleName(), consoleManager);
        }
        FileEditorManager.getInstance(project).openFile(consoleManager.getConsoleFile(), true);
        return consoleManager;
    }

    private @NotNull List<MQLCommandEntry> appendCommandsToDefaultConsole(@NotNull Project project,
                                                                          @NotNull ConsoleManager consoleManager,
                                                                          @NotNull List<MQLCommandEntry> commandList) {
        VirtualFile consoleFile = consoleManager.getConsoleFile();
        Document document = FileDocumentManager.getInstance().getDocument(consoleFile);
        if (document == null) {
            return commandList;
        }

        AtomicReference<List<MQLCommandEntry>> appendedEntriesRef = new AtomicReference<>(commandList);
        WriteCommandAction.runWriteCommandAction(project, "Append MQL To Default Console", null, () -> {
            int insertOffset = document.getTextLength();
            StringBuilder textBuilder = new StringBuilder();
            if (insertOffset > 0) {
                CharSequence currentText = document.getCharsSequence();
                if (currentText.charAt(insertOffset - 1) != '\n') {
                    textBuilder.append('\n');
                }
                if (textBuilder.isEmpty() || textBuilder.charAt(textBuilder.length() - 1) != '\n') {
                    textBuilder.append('\n');
                }
            }

            List<AppendedCommand> appendedCommands = new ArrayList<>(commandList.size());
            int currentOffset = insertOffset + textBuilder.length();
            for (MQLCommandEntry entry : commandList) {
                String command = entry.command().trim();
                if (command.isEmpty()) {
                    continue;
                }
                int startOffset = currentOffset;
                textBuilder.append(command).append('\n');
                currentOffset += command.length() + 1;
                appendedCommands.add(new AppendedCommand(startOffset, command));
            }
            document.insertString(insertOffset, textBuilder.toString());
            FileDocumentManager.getInstance().saveDocument(document);
            List<MQLCommandEntry> appendedEntries = new ArrayList<>(appendedCommands.size());
            for (AppendedCommand appendedCommand : appendedCommands) {
                int lineNumber = document.getLineNumber(appendedCommand.startOffset());
                appendedEntries.add(new MQLCommandEntry(
                        lineNumber,
                        appendedCommand.startOffset(),
                        appendedCommand.startOffset() + appendedCommand.command().length(),
                        appendedCommand.command()
                ));
            }
            appendedEntriesRef.set(appendedEntries);
        });
        return appendedEntriesRef.get();
    }

    private record AppendedCommand(int startOffset, @NotNull String command) {
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
