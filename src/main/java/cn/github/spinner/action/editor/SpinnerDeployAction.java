package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
                UIUtil.showWarningNotification(project, "Deploy", "File is unavailable.");
                return;
            }

            PsiDirectory parent = file.getParent();
            if (parent == null) {
                UIUtil.showWarningNotification(project, "Deploy", "Parent directory is unavailable.");
                return;
            }

            MatrixConnection connection = UserInput.getInstance().connection.get(project);
            if (connection == null) {
                UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, "Please connect to a matrix server first.");
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
                    UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_DEPLOY, "Editor is unavailable for the selected file.");
                    return;
                }
                WorkspaceUtil.importSpinnerFile(connection, project, filePath, buildSelectedSpinnerContent(editor));
                return;
            }

            if (fileName.endsWith(".properties") && "PageFiles".equals(parent.getName())) {
                importPageFile(connection, project, file);
                return;
            }

            UIUtil.showErrorNotification(project, "Unsupported File Type", "Supports .java, .xls and PageFiles/.properties");
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

    private void importPageFile(MatrixConnection connection, Project project, PsiFile file) {
        try {
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = WorkspaceUtil.createRemoteSpinnerDirName();
            String remoteRelativePath = remoteSpinnerDir + "/Business/PageFiles";
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);

            byte[] content = file.getVirtualFile().contentsToByteArray();
            String originalContent = new String(content, StandardCharsets.UTF_8);
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, file.getName(), originalContent);

            runDeployTask(project, remoteBaseDir, remoteSpinnerDir, () -> WorkspaceUtil.runPageImport(
                    connection,
                    remoteBaseDir + "/" + remoteSpinnerDir,
                    remoteBaseDir + "/" + remoteRelativePath + "/" + file.getName(),
                    file.getName()
            ));
        } catch (Exception e) {
            showDeployError(project, e);
        }
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
        try {
            File jpoFile = new File(filePath);
            if (!jpoFile.exists()) {
                throw new IllegalStateException("File not found.");
            }

            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = WorkspaceUtil.createRemoteSpinnerDirName();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), codeContent);

            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");
            runDeployTask(project, remoteBaseDir, remoteSpinnerDir, () -> WorkspaceUtil.runJPOImport(
                    connection,
                    remoteBaseDir + "/" + remoteSpinnerDir,
                    remoteBaseDir + "/" + remoteRelativePath,
                    jpoName
            ));
        } catch (Exception e) {
            showDeployError(project, e);
        }
    }

    private void runDeployTask(Project project, String remoteBaseDir, String remoteSpinnerDir, DeployOperation deployOperation) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TitleConstant.SPINNER_DEPLOY) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting deployment...");
                try {
                    String result = deployOperation.execute();
                    if (result == null || result.isEmpty()) {
                        result = WorkspaceUtil.buildDeploySuccessMessage(remoteBaseDir, remoteSpinnerDir);
                    }
                    UIUtil.showNotification(project, "Deploy Result", result);
                } catch (Exception e) {
                    logger.error("Deploy Error", e);
                    UIUtil.showErrorNotification(project, "Deploy Error", e.getLocalizedMessage());
                }
            }
        });
    }

    private void showDeployError(Project project, Exception exception) {
        logger.error("Deploy Error", exception);
        UIUtil.showErrorNotification(project, "Deploy Error", exception.getLocalizedMessage());
    }

    @FunctionalInterface
    private interface DeployOperation {
        String execute() throws Exception;
    }
}
