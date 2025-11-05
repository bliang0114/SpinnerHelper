package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.hutool.core.text.CharSequenceUtil;
import cn.github.spinner.config.SpinnerToken;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

public class MQLUtil {

    public static String execute(Project project, String mql) throws MQLException {
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        MatrixStatement matrixStatement = connection.executeStatement(mql);
        MatrixResultSet matrixResultSet = matrixStatement.executeQuery();
        if (!matrixResultSet.isSuccess()) {
            throw new MQLException(matrixResultSet.getMessage());
        }
        String result = matrixResultSet.getResult();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static String execute(String format, Object... args) throws MQLException {
        Project project = ProjectManager.getInstance().getDefaultProject();
        return execute(project, CharSequenceUtil.format(format, args));
    }

    public static String execute(Project project, String format, Object... args) throws MQLException {
        return execute(project, CharSequenceUtil.format(format, args));
    }
}
