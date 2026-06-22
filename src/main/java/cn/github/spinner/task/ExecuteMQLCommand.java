package cn.github.spinner.task;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutionEntry;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.MQLUtil;
import cn.github.spinner.util.MQLExecutionGutterManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class ExecuteMQLCommand extends TrackedBackgroundTask {
    private final List<MQLCommandEntry> commandList;
    private final String consoleName;

    public ExecuteMQLCommand(@Nullable Project project, @NotNull String consoleName, @NotNull List<MQLCommandEntry> commandList) {
        super(project, SpinnerBundle.message("progress.executing.mql"), true);
        setCancelText(SpinnerBundle.message("progress.stop.execute"));
        this.consoleName = consoleName;
        this.commandList = commandList;
    }

    @Override
    protected void runTracked(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        Project project = myProject;
        if (project == null) {
            return;
        }

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, SpinnerBundle.message("message.connect.required"));
            return;
        }

        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
        if (consoleManager == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, SpinnerBundle.message("message.console.not.found", consoleName));
            return;
        }

        SpinnerSettings settings = SpinnerSettings.getInstance(project);
        if (!settings.isKeepMQLExecuteHistory()) {
            consoleManager.clear();
            consoleManager.clearExecutionEntries();
        }
        MQLExecutionGutterManager.clear(project, consoleManager.getConsoleFile());

        executeCommands(indicator, project, connection, consoleManager);
    }

    private void executeCommands(@NotNull ProgressIndicator indicator,
                                 @NotNull Project project,
                                 @NotNull MatrixConnection connection,
                                 @NotNull ConsoleManager consoleManager) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < commandList.size(); i++) {
            if (indicator.isCanceled()) {
                break;
            }

            MQLCommandEntry commandEntry = commandList.get(i);
            String command = commandEntry.command();
            long startTime = System.currentTimeMillis();
            indicator.setText2(SpinnerBundle.message("progress.processing", i + 1, commandList.size()));
            consoleManager.printSync("MQL>" + command);
            int consoleResultOffset = consoleManager.getCurrentOutputOffset();

            try {
                MatrixResultSet resultSet = MQLUtil.executeQuery(project, connection, command);
                if (resultSet.isSuccess()) {
                    String successMessage = normalizeMessage("Success", true);
                    MQLExecutionGutterManager.markResult(project, consoleManager.getConsoleFile(), commandEntry.lineNumber(), true, successMessage);
                    recordExecutionEntry(project, consoleManager, commandEntry, consoleResultOffset, true, successMessage);
                    consoleManager.printSync(resultSet.getResult(), ConsoleViewContentType.LOG_INFO_OUTPUT);
                } else {
                    String errorMessage = normalizeMessage(resultSet.getMessage(), false);
                    MQLExecutionGutterManager.markResult(project, consoleManager.getConsoleFile(), commandEntry.lineNumber(), false, errorMessage);
                    recordExecutionEntry(project, consoleManager, commandEntry, consoleResultOffset, false, errorMessage);
                    consoleManager.printSync("[ERROR] " + errorMessage, ConsoleViewContentType.LOG_ERROR_OUTPUT);
                }
            } catch (MQLException e) {
                String errorMessage = normalizeMessage(e.getLocalizedMessage(), false);
                MQLExecutionGutterManager.markResult(project, consoleManager.getConsoleFile(), commandEntry.lineNumber(), false, errorMessage);
                recordExecutionEntry(project, consoleManager, commandEntry, consoleResultOffset, false, errorMessage);
                consoleManager.printSync("[ERROR] " + errorMessage, ConsoleViewContentType.LOG_ERROR_OUTPUT);
            }

            long endTime = System.currentTimeMillis();
            String formatStr = "Start Time: %s, End Time: %s, Duration: %dms";
            consoleManager.printSync(
                    String.format(formatStr, dateFormat.format(new Date(startTime)), dateFormat.format(new Date(endTime)), endTime - startTime),
                    ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
            indicator.setFraction((double) (i + 1) / commandList.size());
        }
    }

    private void recordExecutionEntry(@NotNull Project project,
                                      @NotNull ConsoleManager consoleManager,
                                      @NotNull MQLCommandEntry commandEntry,
                                      int consoleResultOffset,
                                      boolean success,
                                      @NotNull String message) {
        consoleManager.addExecutionEntry(new MQLExecutionEntry(
                commandEntry.lineNumber(),
                commandEntry.sourceStartOffset(),
                commandEntry.sourceEndOffset(),
                consoleResultOffset,
                commandEntry.command(),
                success,
                message
        ));
    }

    private @NotNull String normalizeMessage(@Nullable String message, boolean success) {
        if (message == null) {
            return success ? "Success" : "Unknown error";
        }
        String normalized = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return success ? "Success" : "Unknown error";
    }
}
