package cn.github.spinner.task;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.config.SpinnerToken;
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

@Slf4j
public class MQLCommandExecutor extends Task.Backgroundable {
    private final List<String> commandList;
    private final String consoleName;

    public MQLCommandExecutor(@Nullable Project project, @NotNull String consoleName, @NotNull List<String> commandList) {
        super(project, "Executing MQL Command", true);
        setCancelText("Stop Execute");
        this.consoleName = consoleName;
        this.commandList = commandList;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        assert myProject != null;
        MatrixConnection connection = SpinnerToken.getCurrentConnection(myProject);
        if (connection == null) {
            UIUtil.showWarningNotification(myProject, "MQL Executor", "Not Login, Please Login First");
            return;
        }
        ConsoleManager consoleManager = SpinnerToken.getConsoleManager(myProject, consoleName);
        if (consoleManager == null) {
            UIUtil.showWarningNotification(myProject, "MQL Executor", "Console Manager Not Initialized");
            return;
        }
        MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(myProject);
        if (toolWindow == null) {
            UIUtil.showWarningNotification(myProject, "MQL Executor", "MQL Executor initialize delayed, please retry.");
            return;
        }
        toolWindow.selectNode(consoleName);

        if (!SpinnerSettings.getInstance(myProject).isKeepMQLExecuteHistory()) {
            consoleManager.clear();
        }
        ToolWindow window = UIUtil.getToolWindow(myProject, "MQLExecutor");
        SwingUtilities.invokeLater(() -> {
            window.show(showExecutor(indicator, consoleManager, connection));
        });

    }

    private Runnable showExecutor(@NotNull ProgressIndicator indicator, @NotNull ConsoleManager consoleManager, @NotNull MatrixConnection connection) {
        return () -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < commandList.size(); i++) {
                String command = commandList.get(i);
                long startTime = System.currentTimeMillis();
                indicator.setText2("Processing " + (i + 1) + " / " + commandList.size());
                consoleManager.print("MQL>" + command);
                try {
                    MatrixStatement statement = connection.executeStatement(command);
                    MatrixResultSet resultSet = statement.executeQuery();
                    if (resultSet.isSuccess()) {
                        consoleManager.print(resultSet.getResult(), ConsoleViewContentType.LOG_INFO_OUTPUT);
                    } else {
                        consoleManager.error(resultSet.getMessage());
                    }
                } catch (MQLException e) {
                    consoleManager.error(e.getLocalizedMessage());
                }
                long endTime = System.currentTimeMillis();
                String formatStr = "Start Time: %s, End Time: %s, Duration: %dms";
                consoleManager.print("");
                consoleManager.print(String.format(formatStr, dateFormat.format(new Date(startTime)), dateFormat.format(new Date(endTime)), endTime - startTime), ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
            }
        };
    }
}
