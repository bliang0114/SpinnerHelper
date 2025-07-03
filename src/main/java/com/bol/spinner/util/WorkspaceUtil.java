package com.bol.spinner.util;

import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.JPO;
import matrix.util.MatrixException;

import java.util.Properties;

public class WorkspaceUtil {

    public static String systemprops;

    public static String extractSpinnerSubPath(String fullPath) {
        // 统一替换为标准斜杠
        fullPath = fullPath.replace("\\", "/");

        // 查找 spinner 出现的位置
        int spinnerIndex = fullPath.indexOf("/spinner");
        if (spinnerIndex == -1) {
            throw new IllegalArgumentException("The path does not contain the 'spinner' folder");
        }
        // 截取 spinner 后面的部分
        String subPath = fullPath.substring(spinnerIndex + "/spinner".length());
        // 去除文件名部分（保留目录）
        int lastSlashIndex = subPath.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            subPath = "";  // 如果没有目录结构，直接返回空
        } else {
            subPath = subPath.substring(0, lastSlashIndex);
        }
        // 去除开头和结尾的斜杠
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }
        if (subPath.endsWith("/")) {
            subPath = subPath.substring(0, subPath.length() - 1);
        }
        return subPath;
    }

    public static String getTmpDir(Context ctx) {
        return getEnvironmentVariable(ctx, "TMPDIR");
    }

    public static String getEnvironmentVariable(Context ctx, String var) {
        try {
            if (Character.isUpperCase(var.charAt(0))) {
                return Environment.getValue(ctx, var);
            } else {
                if (systemprops == null) {
                    Properties props = (Properties) JPO.invoke(ctx, "EnoBrowserJPO", (String[])null, "getProperties", (String[])null, Properties.class);
                    systemprops = props.toString().replace(", ", "\n").replace("{", "").replace("}", "");
                }

                int a = systemprops.indexOf(var + "=");
                int b = systemprops.indexOf("\n", a);
                String s = systemprops.substring(a + var.length() + 1, b);
                return s;
            }
        } catch (MatrixException var5) {
            throw new RuntimeException(var5);
        }
    }

    public static void createRemoteTempDir(Context context, String baseDir, String newDir) throws MatrixException {
        String script = "mkdir";
        String output = baseDir + "/mkdir.txt";
        String[] cmdArray = new String[]{script, "-p", newDir, baseDir, output};
        JPO.invoke(context, "EnoBrowserJPO", (String[])null, "runScript", cmdArray);
    }

    public static void uploadTempFile(Context context, String dir, String fileName, String content) throws MatrixException {
        String filePath = dir + "/" + fileName;
        String[] cmdArray = new String[]{filePath, content};
        JPO.invoke(context, "EnoBrowserJPO", (String[])null, "writeFile", cmdArray);
    }
    public static String runSpinnerImport(Context context, String baseDir) throws MatrixException {
        String script = "mql";
        String output = baseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;exec prog emxSpinnerAgent.tcl;quit;", baseDir, output};
        return JPO.invoke(context, "EnoBrowserJPO", (String[]) null, "runScript", cmdArray, String.class);
    }

    public static String runJPOImport(Context context, String spinnerBaseDir, String baseDir, String jpoName) throws MatrixException {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;insert prog "+baseDir +"/"+jpoName+"_mxJPO.java;compile prog "+jpoName+" force update;print context;quit;", spinnerBaseDir, output};
        return JPO.invoke(context, "EnoBrowserJPO", (String[])null, "runScript", cmdArray, String.class);
    }

    public static String runPageImport(Context context, String spinnerBaseDir, String filePath, String fileName) throws MatrixException {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;mod page \""+fileName+"\" file \""+filePath+"\";print context;quit;", spinnerBaseDir, output};
        return JPO.invoke(context, "EnoBrowserJPO", (String[])null, "runScript", cmdArray, String.class);
    }

}
