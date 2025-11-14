package cn.github.spinner.util;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.FileConstant;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Random;

@Slf4j
public class WorkspaceUtil {

    public static void importSpinnerFile(MatrixConnection connection, Project project, String filePath, String content) {
        try {
            File spinnerFile = new File(filePath);
            if (!spinnerFile.exists()) {
                throw new RuntimeException("File not found.");
            }
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            //创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, spinnerFile.getName(), content);
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        //编译JPO
                        String res = WorkspaceUtil.runSpinnerImport(connection, remoteBaseDir + "/" + remoteSpinnerDir);
                        if (res == null || res.isEmpty()) {
                            res = "Deploy success, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
                        }
                        UIUtil.showNotification(project, "Deploy Result",res);
                    } catch (Exception e) {
                        UIUtil.showErrorNotification(project, "Error", e.getLocalizedMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Deploy Error", e);
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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

    public static String getTmpDir(MatrixConnection connection) {
        return getEnvironmentVariable(connection, "TMPDIR");
    }

    public static String getEnvironmentVariable(MatrixConnection connection, String var) {
        try {
            return connection.getEnvironmentVariable(var);
        } catch (Exception var5) {
            throw new RuntimeException(var5);
        }
    }

    public static void createRemoteTempDir(MatrixConnection connection, String baseDir, String newDir) throws Exception {
        String script = "mkdir";
        String output = baseDir + "/mkdir.txt";
        String[] cmdArray = new String[]{script, "-p", newDir, baseDir, output};
        connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray);
    }

    public static void deleteRemoteTempDir(MatrixConnection connection, String deleteDir,String logDir) throws Exception {
        String script = "rm";
        String output = logDir + "/mkdir.txt";
        String[] cmdArray = new String[]{script, "-rf", deleteDir, logDir, output};
        connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray);
    }

    public static void uploadTempFile(MatrixConnection connection, String dir, String fileName, String content) throws Exception {
        String filePath = dir + "/" + fileName;
        String[] cmdArray = new String[]{filePath, content};
        connection.invokeJPOMethod("EnoBrowserJPO", "writeFile", cmdArray);
    }

    public static String runSpinnerImport(MatrixConnection connection, String baseDir) throws Exception {
        String script = "mql";
        String output = baseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;exec prog emxSpinnerAgent.tcl;quit;", baseDir, output};
        return connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runJPOImport(MatrixConnection connection, String spinnerBaseDir, String baseDir, String jpoName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;insert prog " + baseDir + "/" + jpoName + "_mxJPO.java;compile prog " + jpoName + " force update;print context;quit;", spinnerBaseDir, output};
        return connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runJPOImportBath(MatrixConnection connection, String spinnerBaseDir, String baseDir, List<String> fileNames) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        StringBuilder cmdBuild = new StringBuilder();
        cmdBuild.append("set context user creator;");
        for (String fileName : fileNames) {
            cmdBuild.append("insert prog ").append(baseDir).append("/").append(fileName).append(";").append("compile prog ").append(fileName.replace(FileConstant.SUFFIX_JPO, "")).append(" force update;");
        }
        cmdBuild.append("print context;quit;");
        String[] cmdArray = new String[]{script, "-c", cmdBuild.toString(), spinnerBaseDir, output};
        return connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runPageImport(MatrixConnection connection, String spinnerBaseDir, String filePath, String fileName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;mod page \"" + fileName + "\" file \"" + filePath + "\";print context;quit;", spinnerBaseDir, output};
        return connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String runPageImportBatch(MatrixConnection connection, String spinnerBaseDir, String filePath, List<String> fileNames) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";

        StringBuilder cmdBuild = new StringBuilder();
        cmdBuild.append("set context user creator;");
        for (String fileName : fileNames) {
            cmdBuild.append("mod page ").append(fileName).append(" file ").append(filePath).append(fileName).append(";");
        }
        cmdBuild.append("print context;quit;");

        String[] cmdArray = new String[]{script, "-c", cmdBuild.toString(), spinnerBaseDir, output};
        return connection.invokeJPOMethod("EnoBrowserJPO", "runScript", cmdArray, String.class);
    }

    public static String reCachePage(MatrixConnection connection) throws Exception {
        return connection.invokeJPOMethod("SpinnerDeployJPO", "reCachePage", new String[]{}, String.class);
    }

}
