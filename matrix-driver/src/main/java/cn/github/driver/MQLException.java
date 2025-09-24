package cn.github.driver;

import java.sql.SQLException;

/**
 * 基于SQL封装的MQL异常类型
 */
public class MQLException extends SQLException {
    public MQLException(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public MQLException(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public MQLException(String reason) {
        super(reason);
    }

    public MQLException() {
        super();
    }

    public MQLException(Throwable cause) {
        super(cause);
    }

    public MQLException(String reason, Throwable cause) {
        super(reason, cause);
    }

    public MQLException(String reason, String sqlState, Throwable cause) {
        super(reason, sqlState, cause);
    }

    public MQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
    }
}
