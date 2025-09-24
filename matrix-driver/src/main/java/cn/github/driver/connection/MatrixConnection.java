package cn.github.driver.connection;

import cn.github.driver.MQLException;

import java.io.Closeable;

/**
 * 3DExperience Matrix 连接
 */
public interface MatrixConnection extends Closeable {

    /**
     * 执行MQL查询语句
     *
     * @param mql MQL查询语句
     * @return {@link MatrixStatement}
     * @author xlwang
     */
    MatrixStatement executeStatement(String mql) throws MQLException;

    /**
     * 执行MQL 更新/删除语句
     *
     * @param mql MQL 更新/删除语句
     * @author xlwang
     */
    void executeUpdate(String mql) throws MQLException;

    /**
     * 获取系统环境变量
     *
     * @param var 变量名称
     * @return {@link String}
     * @author xlwang
     */
    String getEnvironmentVariable(String var) throws MQLException;

    /**
     * 远程执行JPO方法
     *
     * @param jpoName    JPO名称
     * @param methodName 方法名
     * @param params     参数
     * @return {@link int}
     * @author xlwang
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
     * @author xlwang
     */
    <T> T invokeJPOMethod(String jpoName, String methodName, String[] params, Class<T> clazz) throws MQLException;
}
