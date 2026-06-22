package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.service.MatrixTaskExecutor;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MQLUtil {

    public static String execute(Project project, String mql) throws MQLException {
        MatrixConnection connection = project == null ? null : UserInput.getInstance().connection.get(project);
        MatrixResultSet matrixResultSet = executeQuery(project, connection, mql);
        if (!matrixResultSet.isSuccess()) {
            throw new MQLException(matrixResultSet.getMessage());
        }
        String result = matrixResultSet.getResult();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static MatrixResultSet executeQuery(Project project,
                                               MatrixConnection connection,
                                               String mql) throws MQLException {
        if (connection == null) {
            throw new MQLException("Please connect to a matrix server first.");
        }

        int timeoutMinutes = getTimeoutMinutes(project);
        Future<MatrixResultSet> future;
        try {
            future = MatrixTaskExecutor.getInstance().submit(() -> {
                MatrixConnectionUtil.assertCurrentServerReachable(project);
                MatrixStatement matrixStatement = connection.executeStatement(mql);
                return matrixStatement.executeQuery();
            });
        } catch (RejectedExecutionException e) {
            throw new MQLException(SpinnerBundle.message("message.matrix.operation.busy"), e);
        }
        try {
            return future.get(timeoutMinutes, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MQLException(SpinnerBundle.message("message.execute.timeout", timeoutMinutes), e);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new MQLException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof MQLException mqlException) {
                throw mqlException;
            }
            throw new MQLException(cause);
        }
    }

    public static String execute(String format, Object... args) throws MQLException {
        Project project = ProjectManager.getInstance().getDefaultProject();
        return execute(project, CharSequenceUtil.format(format, args));
    }

    public static String execute(Project project, String format, Object... args) throws MQLException {
        return execute(project, CharSequenceUtil.format(format, args));
    }

    private static int getTimeoutMinutes(Project project) {
        if (project == null || project.isDisposed()) {
            return 10;
        }
        int timeoutMinutes = SpinnerSettings.getInstance(project).getTimeoutMinutes();
        return timeoutMinutes <= 0 ? 10 : timeoutMinutes;
    }
}
