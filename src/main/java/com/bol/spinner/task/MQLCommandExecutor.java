package com.bol.spinner.task;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.execution.MQLExecutorToolWindow;
import com.bol.spinner.util.ConsoleManager;
import com.bol.spinner.util.UIUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
public class MQLCommandExecutor extends Task.Backgroundable {
    private final List<String> commandList;

    public MQLCommandExecutor(@Nullable Project project, @NotNull List<String> commandList) {
        super(project, "Executing MQL Command", true);
        setCancelText("Stop Execute");
        this.commandList = commandList;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        assert myProject != null;
        MatrixConnection connection = SpinnerToken.getCurrentConnection(myProject);
        if (connection == null) {
            UIUtil.showWarningNotification(myProject, "Not Login, Please Login First", "");
            return;
        }

        MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(myProject);
        log.info("toolWindow: {}", toolWindow);
        if (toolWindow == null) {
            UIUtil.showWarningNotification(myProject, "Wait for executor window initialize, 2 seconds", "");
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ConsoleManager consoleManager = toolWindow.getConsoleManager();
        if (!SpinnerSettings.getInstance(myProject).isKeepMQLExecuteHistory()) {
            consoleManager.clear();
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow window = UIUtil.getToolWindow(myProject, "MQLExecutor");
            window.show(() -> {
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
            });
        });
    }
}
