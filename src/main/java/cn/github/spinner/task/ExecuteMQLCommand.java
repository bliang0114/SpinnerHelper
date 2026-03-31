package cn.github.spinner.task;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutionEntry;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.MQLExecutionGutterManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ExecuteMQLCommand extends Task.Backgroundable {
    private final List<MQLCommandEntry> commandList;
    private final String consoleName;

    public ExecuteMQLCommand(@Nullable Project project, @NotNull String consoleName, @NotNull List<MQLCommandEntry> commandList) {
        super(project, "Executing MQL Command", true);
        setCancelText("Stop Execute");
        this.consoleName = consoleName;
        this.commandList = commandList;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        Project project = myProject;
        if (project == null) {
            return;
        }

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Please connect to a matrix server first.");
            return;
        }

        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
        if (consoleManager == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Console not found: " + consoleName);
            return;
        }

        SpinnerSettings settings = SpinnerSettings.getInstance(project);
        if (!settings.isKeepMQLExecuteHistory()) {
            consoleManager.clear();
            consoleManager.clearExecutionEntries();
        }
        MQLExecutionGutterManager.clear(project, consoleManager.getConsoleFile());

        int timeoutMinutes = settings.getTimeoutMinutes();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> executeCommands(indicator, project, connection, consoleManager));
        try {
            future.get(timeoutMinutes, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            UIUtil.showErrorNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Execute timeout (" + timeoutMinutes + "min exceeded).");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            UIUtil.showErrorNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Execute failed: " + cause.getMessage());
        } finally {
            executor.shutdownNow();
        }
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
            indicator.setText2("Processing " + (i + 1) + " / " + commandList.size());
            consoleManager.printSync("MQL>" + command);
            int consoleResultOffset = consoleManager.getCurrentOutputOffset();

            try {
                MatrixStatement statement = connection.executeStatement(command);
                MatrixResultSet resultSet = statement.executeQuery();
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
