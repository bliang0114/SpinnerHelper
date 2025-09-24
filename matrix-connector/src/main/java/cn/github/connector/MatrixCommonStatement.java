package cn.github.connector;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;

@Slf4j
public class MatrixCommonStatement implements MatrixStatement {
    private final Context context;
    private final String mql;

    public MatrixCommonStatement(@NonNull Context context, @NonNull String mql) {
        this.context = context;
        this.mql = mql;
    }

    @Override
    public MatrixResultSet executeQuery() throws MQLException {
        MQLCommand mqlCommand = new MQLCommand();
        try {
            MatrixResultSet resultSet = new MatrixResultSet();
            boolean success = mqlCommand.executeCommand(this.context, this.mql, true, false, false);
            resultSet.setSuccess(success);
            if (success) {
                resultSet.setResult(mqlCommand.getResult());
            } else {
                resultSet.setMessage(mqlCommand.getError());
            }
            return resultSet;
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }
}
