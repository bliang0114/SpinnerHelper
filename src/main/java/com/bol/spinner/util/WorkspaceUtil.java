package com.bol.spinner.util;

import com.bol.spinner.MatrixContext;
import com.bol.spinner.MatrixUtil;

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

    public static String getTmpDir(MatrixContext ctx) {
        return getEnvironmentVariable(ctx, "TMPDIR");
    }

    public static String getEnvironmentVariable(MatrixContext ctx, String var) {
        try {
            return MatrixUtil.getEnvironmentVariable(ctx, var);
        } catch (Exception var5) {
            throw new RuntimeException(var5);
        }
    }

    public static void createRemoteTempDir(MatrixContext context, String baseDir, String newDir) throws Exception {
        String script = "mkdir";
        String output = baseDir + "/mkdir.txt";
        String[] cmdArray = new String[]{script, "-p", newDir, baseDir, output};
        MatrixUtil.invokeJPOMethod(context, "EnoBrowserJPO", "runScript", cmdArray);
    }

    public static void uploadTempFile(MatrixContext context, String dir, String fileName, String content) throws Exception {
        String filePath = dir + "/" + fileName;
        String[] cmdArray = new String[]{filePath, content};
        MatrixUtil.invokeJPOMethod(context, "EnoBrowserJPO", "writeFile", cmdArray);
    }
    public static String runSpinnerImport(MatrixContext context, String baseDir) throws Exception {
        String script = "mql";
        String output = baseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;exec prog emxSpinnerAgent.tcl;quit;", baseDir, output};
        return MatrixUtil.invokeJPOMethod(context, "EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runJPOImport(MatrixContext context, String spinnerBaseDir, String baseDir, String jpoName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;insert prog "+baseDir +"/"+jpoName+"_mxJPO.java;compile prog "+jpoName+" force update;print context;quit;", spinnerBaseDir, output};
        return MatrixUtil.invokeJPOMethod(context, "EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runPageImport(MatrixContext context, String spinnerBaseDir, String filePath, String fileName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;mod page \""+fileName+"\" file \""+filePath+"\";print context;quit;", spinnerBaseDir, output};
        return MatrixUtil.invokeJPOMethod(context, "EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

}
