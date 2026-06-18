package cn.github.spinner.util;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class WorkspaceUtil {

    public static void importSpinnerFile(MatrixConnection connection, Project project, String filePath, String content) {
        new TrackedBackgroundTask(project, TitleConstant.SPINNER_DEPLOY) {
            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(SpinnerBundle.message("progress.starting.deployment"));
                try {
                    File spinnerFile = new File(filePath);
                    if (!spinnerFile.exists()) {
                        throw new RuntimeException(SpinnerBundle.message("message.file.not.found"));
                    }
                    String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
                    String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
                    String remoteSpinnerDir = createRemoteSpinnerDirName();
                    String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
                    WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
                    WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, spinnerFile.getName(), content);

                    String res = WorkspaceUtil.runSpinnerImport(connection, remoteBaseDir + "/" + remoteSpinnerDir);
                    if (res == null || res.isEmpty()) {
                        res = buildDeploySuccessMessage(remoteBaseDir, remoteSpinnerDir);
                    }
                    UIUtil.showNotification(project, SpinnerBundle.message("notification.title.deploy.result"), res);
                } catch (Exception e) {
                    log.error("Deploy Error", e);
                    UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.deploy.error"), e.getLocalizedMessage());
                }
            }
        }.queue();
    }

    public static String createRemoteSpinnerDirName() {
        return "spinner" + ThreadLocalRandom.current().nextInt(1_000_000_000);
    }

    public static String buildDeploySuccessMessage(String remoteBaseDir, String remoteSpinnerDir) {
        return SpinnerBundle.message("message.deploy.success", remoteBaseDir, remoteSpinnerDir);
    }

    public static String extractSpinnerSubPath(String fullPath) {
        fullPath = fullPath.replace("\\", "/");

        int spinnerIndex = fullPath.indexOf("/spinner");
        if (spinnerIndex == -1) {
            throw new IllegalArgumentException(SpinnerBundle.message("message.spinner.path.missing"));
        }
        String subPath = fullPath.substring(spinnerIndex + "/spinner".length());
        int lastSlashIndex = subPath.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            subPath = "";
        } else {
            subPath = subPath.substring(0, lastSlashIndex);
        }
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
        connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray);
    }

    public static void deleteRemoteTempDir(MatrixConnection connection, String deleteDir, String logDir) throws Exception {
        String script = "rm";
        String output = logDir + "/mkdir.txt";
        String[] cmdArray = new String[]{script, "-rf", deleteDir, logDir, output};
        connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray);
    }

    public static void uploadTempFile(MatrixConnection connection, String dir, String fileName, String content) throws Exception {
        String filePath = dir + "/" + fileName;
        String[] cmdArray = new String[]{filePath, content};
        connection.invokeJPOMethod("SpinnerDeployJPO", "writeFile", cmdArray);
    }

    public static String runSpinnerImport(MatrixConnection connection, String baseDir) throws Exception {
        String script = "mql";
        String output = baseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;exec prog emxSpinnerAgent.tcl;quit;", baseDir, output};
        return connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray, String.class);
    }

    public static String runJPOImport(MatrixConnection connection, String spinnerBaseDir, String baseDir, String jpoName, String packageName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String className;
        if (StrUtil.isNotEmpty(packageName)) {
            className = packageName + "." + jpoName;
        } else {
            className = jpoName;
        }
        String[] cmdArray = new String[]{script, "-c", "set context user creator;insert prog " + baseDir + "/" + jpoName + "_mxJPO.java;compile prog " + className + " force update;print context;quit;", spinnerBaseDir, output};
        return connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray, String.class);
    }

    public static String runJPOImportBath(MatrixConnection connection, String spinnerBaseDir, String baseDir, List<String> fileNames) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        StringBuilder cmdBuild = new StringBuilder();
        cmdBuild.append("set context user creator;");
        for (String fileName : fileNames) {
            cmdBuild.append("insert prog ")
                    .append(baseDir)
                    .append("/")
                    .append(fileName)
                    .append(";")
                    .append("compile prog ")
                    .append(fileName.replace(FileConstant.SUFFIX_JPO, ""))
                    .append(" force update;");
        }
        cmdBuild.append("print context;quit;");
        String[] cmdArray = new String[]{script, "-c", cmdBuild.toString(), spinnerBaseDir, output};
        return connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray, String.class);
    }

    public static String runPageImport(MatrixConnection connection, String spinnerBaseDir, String filePath, String fileName) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";
        String[] cmdArray = new String[]{script, "-c", "set context user creator;mod page \"" + fileName + "\" file \"" + filePath + "\";print context;quit;", spinnerBaseDir, output};
        return connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray, String.class);
    }

    public static String runPageImportBatch(Project project, MatrixConnection connection, String spinnerBaseDir, String filePath, List<String> fileNames) throws Exception {
        String script = "mql";
        String output = spinnerBaseDir + "/spinner.log";

        StringBuilder cmdBuild = new StringBuilder();
        cmdBuild.append("set context user creator;");
        for (String fileName : fileNames) {
            String execute = MQLUtil.execute(project, "list page " + fileName);
            if (CharSequenceUtil.isEmpty(execute)) {
                cmdBuild.append("add");
            } else {
                cmdBuild.append("mod");
            }
            cmdBuild.append(" page ").append(fileName).append(" file ").append(filePath).append(fileName).append(";");
        }
        cmdBuild.append("print context;quit;");

        String[] cmdArray = new String[]{script, "-c", cmdBuild.toString(), spinnerBaseDir, output};
        return connection.invokeJPOMethod("SpinnerDeployJPO", "runScript", cmdArray, String.class);
    }

    public static void reloadPageCache(MatrixConnection connection) throws Exception {
        connection.invokeJPOMethod("SpinnerDeployJPO", "reloadPageCache", new String[]{}, String.class);
    }

    public static void reloadSpinnerCache(MatrixConnection connection) throws Exception {
        connection.invokeJPOMethod("SpinnerDeployJPO", "reloadSpinnerCache", new String[]{}, String.class);
    }

    public static void reloadCache(MatrixConnection connection) throws Exception {
        connection.invokeJPOMethod("SpinnerDeployJPO", "reloadCache", new String[]{}, String.class);
    }
}
