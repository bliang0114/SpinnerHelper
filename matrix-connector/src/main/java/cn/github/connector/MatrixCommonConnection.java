package cn.github.connector;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixStatement;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.JPO;
import matrix.util.MatrixException;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class MatrixCommonConnection implements MatrixConnection {
    private Context context;
    public static String systemprops;

    public MatrixCommonConnection(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public MatrixStatement executeStatement(String mql) throws MQLException {
        return new MatrixCommonStatement(context, mql);
    }

    @Override
    public void executeUpdate(String mql) throws MQLException {
        log.info("executeUpdate: {}", mql);
    }

    @Override
    public String getEnvironmentVariable(String var) throws MQLException {
        try {
            if (Character.isUpperCase(var.charAt(0))) {
                return Environment.getValue(context, var);
            } else {
                if (systemprops == null) {
                    Properties props = JPO.invoke(context, "EnoBrowserJPO", (String[])null, "getProperties", (String[])null, Properties.class);
                    systemprops = props.toString().replace(", ", "\n").replace("{", "").replace("}", "");
                }
                int a = systemprops.indexOf(var + "=");
                int b = systemprops.indexOf("\n", a);
                return systemprops.substring(a + var.length() + 1, b);
            }
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public int invokeJPOMethod(String jpoName, String methodName, String[] params) throws MQLException {
        try {
            return JPO.invoke(context, jpoName, null, methodName, params);
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public <T> T invokeJPOMethod(String jpoName, String methodName, String[] params, Class<T> clazz) throws MQLException {
        try {
            return JPO.invoke(context, jpoName, null, methodName, params, clazz);
        } catch (MatrixException e) {
            throw new MQLException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.context.shutdown();
        } catch (MatrixException e) {
            throw new IOException(e);
        }
        this.context = null;
    }
}
