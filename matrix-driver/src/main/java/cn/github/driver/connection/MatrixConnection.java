package cn.github.driver.connection;

import cn.github.driver.MQLException;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;

/**
 * 3DExperience Matrix 连接
 */
public interface MatrixConnection extends Closeable {

    /**
     * 执行MQL查询语句
     *
     * @param mql MQL查询语句
     * @return {@link MatrixStatement}
     * @author zaydenwang
     */
    MatrixStatement executeStatement(String mql) throws MQLException;

    /**
     *
     * @param objectQuery 对象查询条件
     * @param fields      查询指定属性
     * @return {@link MatrixQueryResult}
     * @author zaydenwang
     */
    default MatrixQueryResult queryObject(MatrixObjectQuery objectQuery, String... fields) throws MQLException {
        return queryObject(objectQuery, fields.length == 0 ? List.of("type", "name", "revision") : List.of(fields));
    }

    /**
     *
     * @param objectQuery 对象查询条件
     * @param fields      查询指定属性
     * @return {@link MatrixQueryResult}
     * @author zaydenwang
     */
    MatrixQueryResult queryObject(MatrixObjectQuery objectQuery, List<String> fields) throws MQLException;

    /**
     *
     * @param connectionQuery 关系查询条件
     * @param fields          查询指定属性
     * @return {@link MatrixQueryResult}
     * @author zaydenwang
     */
    default MatrixQueryResult queryConnection(MatrixConnectionQuery connectionQuery, String... fields) throws MQLException {
        return queryConnection(connectionQuery, fields.length == 0 ? List.of("type", "id") : List.of(fields));
    }

    /**
     *
     * @param connectionQuery 关系查询条件
     * @param fields          查询指定属性
     * @return {@link MatrixQueryResult}
     * @author zaydenwang
     */
    MatrixQueryResult queryConnection(MatrixConnectionQuery connectionQuery, List<String> fields) throws MQLException;

    /**
     * 获取系统环境变量
     *
     * @param var 变量名称
     * @return {@link String}
     * @author zaydenwang
     */
    String getEnvironmentVariable(String var) throws MQLException;

    /**
     * 远程执行JPO方法
     *
     * @param jpoName    JPO名称
     * @param methodName 方法名
     * @param params     参数
     * @return {@link int}
     * @author zaydenwang
     */
    int invokeJPOMethod(String jpoName, String methodName, String[] params) throws MQLException;

    /**
     * 远程执行JPO方法
     *
     * @param jpoName    JPO名称
     * @param methodName 方法名
     * @param params     参数
     * @param clazz      指定返回对象类型
     * @return {@link T}
     * @author zaydenwang
     */
    <T> T invokeJPOMethod(String jpoName, String methodName, String[] params, Class<T> clazz) throws MQLException;
}
