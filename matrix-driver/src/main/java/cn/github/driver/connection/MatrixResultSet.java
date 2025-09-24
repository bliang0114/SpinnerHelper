package cn.github.driver.connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Matrix MQL 查询结果集
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatrixResultSet {
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 查询结果
     */
    private String result;
    /**
     * 错误信息
     */
    private String message;
}
