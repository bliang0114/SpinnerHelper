package cn.github.driver;

import cn.github.driver.connection.MatrixConnection;

/**
 * 3DExperience Matrix 驱动
 */
public interface MatrixDriver {

    /**
     * 连接数据源
     *
     * @param matrixDriverProperty 数据源信息
     * @return {@link MatrixConnection}
     * @author xlwang
     */
    MatrixConnection connect(MatrixDriverProperty matrixDriverProperty) throws MQLException;

    /**
     * 获取 Matrix 版本
     *
     * @return {@link String}
     * @author xlwang
     */
    String getVersion();
}
