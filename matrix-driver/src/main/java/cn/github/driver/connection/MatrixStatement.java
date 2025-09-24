package cn.github.driver.connection;

import cn.github.driver.MQLException;

/**
 * 3DExperience Matrix 查询封装
 */
public interface MatrixStatement {

    /**
     * 执行MQL查询
     *
     * @return {@link MatrixResultSet}
     * @author xlwang
     */
    MatrixResultSet executeQuery() throws MQLException;
}
