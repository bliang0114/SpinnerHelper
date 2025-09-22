package com.bol.spinner;

import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;

import java.util.Properties;

public class MatrixUtil {
    public static String systemprops;

    public static boolean hasMatrixRuntime() {
        try {
            Class.forName(Context.class.getName());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static MatrixMQLResult executeMQL(MatrixContext matrixContext, String mqlCommand, boolean flag) throws Exception {
        try {
            MatrixMQLResult result = new MatrixMQLResult();
            var mql = new MQLCommand();
            if (mql.executeCommand(matrixContext.context, mqlCommand, flag)) {
                result.setSuccess(true);
                result.setResult(mql.getResult());
            } else {
                result.setSuccess(false);
                result.setResult(mql.getError());
            }
            return result;
        } catch (MatrixException ex) {
            Util.trace(ex);
            throw new Exception(ex);
        }
    }

    public static String execMQL(MatrixContext ctx, String command) throws Exception {
        return execMQL(ctx, command, true);
    }

    public static String execMQLquite(MatrixContext ctx, String command) throws Exception {
        return execMQL(ctx, command, false);
    }

    private static String execMQL(MatrixContext ctx, String command, boolean showMessage) throws Exception {
        var mql = new MQLCommand();
        if (!mql.executeCommand(ctx.context, command, true)) {
            Util.trace("MQL Command: " + command);
            Util.trace("MQL Error: " + mql.getError());
            if (showMessage)
                Util.displayError(mql.getError());
            return "";
        }
        var result = mql.getResult();
        if (result.endsWith("\n"))
            result = result.substring(0, result.length() - 1);
        return result;
    }

    public static String getEnvironmentVariable(MatrixContext ctx, String var) throws Exception {
        try {
            if (Character.isUpperCase(var.charAt(0))) {
                return Environment.getValue(ctx.context, var);
            } else {
                if (systemprops == null) {
                    Properties props = JPO.invoke(ctx.context, "EnoBrowserJPO", (String[])null, "getProperties", (String[])null, Properties.class);
                    systemprops = props.toString().replace(", ", "\n").replace("{", "").replace("}", "");
                }

                int a = systemprops.indexOf(var + "=");
                int b = systemprops.indexOf("\n", a);
                String s = systemprops.substring(a + var.length() + 1, b);
                return s;
            }
        } catch (MatrixException var5) {
            throw new Exception(var5);
        }
    }

    public static int invokeJPOMethod(MatrixContext ctx, String jpoName, String methodName, String[] cmdArray) throws Exception {
        try {
            return JPO.invoke(ctx.context, jpoName, null, methodName, cmdArray);
        } catch (MatrixException e) {
            throw new Exception(e);
        }
    }

    public static <T> T invokeJPOMethod(MatrixContext ctx, String jpoName, String methodName, String[] cmdArray, Class<T> clazz) throws Exception {
        try {
            return JPO.invoke(ctx.context, jpoName, null, methodName, cmdArray, clazz);
        } catch (MatrixException e) {
            throw new Exception(e);
        }
    }
}
