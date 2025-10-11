package com.bol.spinner.util;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.hutool.core.text.CharSequenceUtil;
import com.bol.spinner.config.SpinnerToken;

public class MQLUtil {

    public static String execute(String mql) throws MQLException {
        MatrixConnection connection = SpinnerToken.connection;
        MatrixStatement matrixStatement = connection.executeStatement(mql);
        MatrixResultSet matrixResultSet = matrixStatement.executeQuery();
        if (!matrixResultSet.isSuccess()) {
            throw new MQLException(matrixResultSet.getMessage());
        }
        return matrixResultSet.getResult();
    }

    public static String execute(String format, Object... args) throws MQLException {
        return execute(CharSequenceUtil.format(format, args));
    }
}
