package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SpinnerDeployAction extends AnAction {

    private static final Logger logger = Logger.getInstance(SpinnerDeployAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        try {
            logger.info("Deploy Action start");
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            if (file == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, SpinnerBundle.message("message.file.unavailable"));
                return;
            }

            PsiDirectory parent = file.getParent();
            if (parent == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, SpinnerBundle.message("message.parent.directory.unavailable"));
                return;
            }

            MatrixConnection connection = UserInput.getInstance().connection.get(project);
            if (connection == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, SpinnerBundle.message("message.connect.required"));
                return;
            }

            String fileName = file.getName();
            String filePath = file.getViewProvider().getVirtualFile().getPath();
            if (fileName.endsWith(".java")) {
                importJpoFile(connection, project, filePath, file.getText());
                return;
            }

            if (fileName.endsWith(".xls")) {
                Editor editor = e.getData(CommonDataKeys.EDITOR);
                if (editor == null) {
                    UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, SpinnerBundle.message("message.editor.unavailable"));
                    return;
                }
                WorkspaceUtil.importSpinnerFile(connection, project, filePath, buildSelectedSpinnerContent(editor));
                return;
            }

            if (fileName.endsWith(".properties") && "PageFiles".equals(parent.getName())) {
                importPageFile(connection, project, file.getName(), file.getText());
                return;
            }

            UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.unsupported.file.type"), SpinnerBundle.message("message.unsupported.deploy.file.type"));
        } catch (Exception ex) {
            showDeployError(project, ex);
        }
    }

    private String buildSelectedSpinnerContent(@NotNull Editor editor) {
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        int lineCount = document.getLineCount();
        if (lineCount == 0) {
            return "";
        }
        String firstLineText = document.getText(new TextRange(
                document.getLineStartOffset(0),
                document.getLineEndOffset(0)
        ));
        if (lineCount == 1) {
            return firstLineText;
        }

        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        int safeSelectionEnd = selectionEnd > selectionStart ? selectionEnd - 1 : selectionEnd;
        int startLine = Math.max(1, document.getLineNumber(selectionStart));
        int endLine = Math.max(startLine, document.getLineNumber(safeSelectionEnd));
        startLine = Math.min(startLine, lineCount - 1);
        endLine = Math.min(endLine, lineCount - 1);
        String selectedLineContent = document.getText(new TextRange(
                document.getLineStartOffset(startLine),
                document.getLineEndOffset(endLine)
        ));
        return firstLineText + "\n" + selectedLineContent;
    }

    private void importPageFile(MatrixConnection connection, Project project, String fileName, String originalContent) {
        runDeployTask(project, () -> {
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = WorkspaceUtil.createRemoteSpinnerDirName();
            String remoteRelativePath = remoteSpinnerDir + "/Business/PageFiles";
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);

            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, fileName, originalContent);

            String result = WorkspaceUtil.runPageImport(
                    connection,
                    remoteBaseDir + "/" + remoteSpinnerDir,
                    remoteBaseDir + "/" + remoteRelativePath + "/" + fileName,
                    fileName
            );
            return normalizeDeployResult(result, remoteBaseDir, remoteSpinnerDir);
        });
    }

    public static String encodeToUnicode(String str) {
        if (str == null) {
            return null;
        }
        if (isUnicode(str)) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            result.append("\\u").append(String.format("%04x", (int) ch));
        }
        return result.toString();
    }

    public static boolean isUnicode(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                return true;
            }
            if (c > 127) {
                return true;
            }
        }
        return false;
    }

    private void importJpoFile(MatrixConnection connection, Project project, String filePath, String codeContent) {
        runDeployTask(project, () -> {
            File jpoFile = new File(filePath);
            if (!jpoFile.exists()) {
                throw new IllegalStateException(SpinnerBundle.message("message.file.not.found"));
            }

            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = WorkspaceUtil.createRemoteSpinnerDirName();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), codeContent);
            String packageName = extractPackageName(jpoFile);
            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");
            String result = WorkspaceUtil.runJPOImport(
                    connection,
                    remoteBaseDir + "/" + remoteSpinnerDir,
                    remoteBaseDir + "/" + remoteRelativePath,
                    jpoName,
                    packageName
            );
            return normalizeDeployResult(result, remoteBaseDir, remoteSpinnerDir);
        });
    }

    public static String extractPackageName(File javaFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("package ")) {
                    return line.substring(8).replace(";", "").trim();
                }
                if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*")) {
                    break;
                }
            }
        }
        return ""; // 默认包
    }

    private void runDeployTask(Project project, DeployOperation deployOperation) {
        new Task.Backgroundable(project, TitleConstant.SPINNER_DEPLOY) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(SpinnerBundle.message("progress.starting.deployment"));
                try {
                    String result = deployOperation.execute();
                    UIUtil.showNotification(project, SpinnerBundle.message("notification.title.deploy.result"), result);
                } catch (Exception e) {
                    logger.error("Deploy Error", e);
                    UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.deploy.error"), e.getLocalizedMessage());
                }
            }
        }.queue();
    }

    private String normalizeDeployResult(String result, String remoteBaseDir, String remoteSpinnerDir) {
        if (result == null || result.isEmpty()) {
            return WorkspaceUtil.buildDeploySuccessMessage(remoteBaseDir, remoteSpinnerDir);
        }
        return result;
    }

    private void showDeployError(Project project, Exception exception) {
        logger.error("Deploy Error", exception);
        UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.deploy.error"), exception.getLocalizedMessage());
    }

    @FunctionalInterface
    private interface DeployOperation {
        String execute() throws Exception;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
