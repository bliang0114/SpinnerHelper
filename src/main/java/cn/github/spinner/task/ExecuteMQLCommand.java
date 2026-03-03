package cn.github.spinner.task;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.UIUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class ExecuteMQLCommand extends Task.Backgroundable {
    private final List<String> commandList;
    private final String consoleName;

    public ExecuteMQLCommand(@Nullable Project project, @NotNull String consoleName, @NotNull List<String> commandList) {
        super(project, "Executing MQL Command", true);
        setCancelText("Stop Execute");
        this.consoleName = consoleName;
        this.commandList = commandList;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        assert myProject != null;
        MatrixConnection connection = UserInput.getInstance().connection.get(myProject);
        if (connection == null) {
            UIUtil.showWarningNotification(myProject, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Please connect to a matrix server first.");
            return;
        }
        int timeoutMinutes = SpinnerSettings.getInstance(myProject).getTimeoutMinutes();
        ConsoleManager consoleManager = UserInput.getInstance().getConsole(myProject, consoleName);
        if (!SpinnerSettings.getInstance(myProject).isKeepMQLExecuteHistory()) {
            consoleManager.clear();
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ToolWindow window = UIUtil.getToolWindow(myProject, "MQLExecutor");
            SwingUtilities.invokeLater(() -> window.show(() -> {
                MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(myProject);
                if (toolWindow == null) {
                    UIUtil.showWarningNotification(myProject, "MQL Executor", "MQL Executor initialize delayed, please retry.");
                    return;
                }
                log.info("consoleName: {}", consoleName);
                toolWindow.selectNode(consoleName);
            }));
            for (int i = 0; i < commandList.size(); i++) {
                if (indicator.isCanceled()) {
                    break;
                }
                String command = commandList.get(i);
                long startTime = System.currentTimeMillis();
                indicator.setText2("Processing " + (i + 1) + " / " + commandList.size());
                SwingUtilities.invokeLater(() -> consoleManager.print("MQL>" + command));
                try {
                    MatrixStatement statement = connection.executeStatement(command);
                    MatrixResultSet resultSet = statement.executeQuery();
                    if (resultSet.isSuccess()) {
                        SwingUtilities.invokeLater(() -> consoleManager.print(resultSet.getResult(), ConsoleViewContentType.LOG_INFO_OUTPUT));
                    } else {
                        SwingUtilities.invokeLater(() -> consoleManager.error(resultSet.getMessage()));
                    }
                } catch (MQLException e) {
                    SwingUtilities.invokeLater(() -> consoleManager.error(e.getLocalizedMessage()));
                }
                long endTime = System.currentTimeMillis();
                String formatStr = "Start Time: %s, End Time: %s, Duration: %dms";
                SwingUtilities.invokeLater(() ->
                        consoleManager.print(String.format(formatStr, dateFormat.format(new Date(startTime)), dateFormat.format(new Date(endTime)), endTime - startTime), ConsoleViewContentType.LOG_VERBOSE_OUTPUT));
                indicator.setFraction((double) (i + 1) / commandList.size());
            }
        });
        try {
            // 用 IDEA 提供的工具方法等待，不阻塞进度条
            future.get(timeoutMinutes, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Execute timeout (" + timeoutMinutes + "min exceeded).");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            UIUtil.showErrorNotification(myProject, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Execute failed: " + e.getCause().getMessage());
        } finally {
            executor.shutdownNow();
        }
    }
}
