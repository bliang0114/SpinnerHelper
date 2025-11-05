package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerToken;
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
import com.intellij.ui.JBColor;
import com.intellij.util.ui.StartupUiUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SpinnerDeployAction extends AnAction {

    private static final Logger logger = Logger.getInstance(SpinnerDeployAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;

        try {
            logger.info("Deploy Action start");
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            if(file == null){
                UIUtil.showWarningNotification(project, "File is null", "");
                return;
            }
            String filePath = file.getViewProvider().getVirtualFile().getPath();
            PsiDirectory parent = file.getParent();
            if(parent == null){
                UIUtil.showWarningNotification(project, "Parent Dir is null", "");
                return;
            }
            String fileName = file.getName();
            MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
            if (connection== null) {
                UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
                return;
            }
            if (fileName.endsWith(".java")) {
                String javaContent = file.getText();
                importJPOFile(connection, project, filePath, javaContent);
            } else if (fileName.endsWith(".xls")) {
                Editor editor = e.getData(CommonDataKeys.EDITOR);
                assert editor != null;
                Document document = editor.getDocument();
                SelectionModel selectionModel = editor.getSelectionModel();
                int firstLine = 0;
                String firstLineText = editor.getDocument().getText(new TextRange(
                        editor.getDocument().getLineStartOffset(firstLine),
                        editor.getDocument().getLineEndOffset(firstLine)
                ));
                int selectionStart = selectionModel.getSelectionStart();
                int selectionEnd = selectionModel.getSelectionEnd();
                int startLine = document.getLineNumber(selectionStart);
                if (startLine == 0) {
                    startLine++;
                }
                int endLine = document.getLineNumber(selectionEnd);
                String selectLineContent = editor.getDocument().getText(new TextRange(
                        editor.getDocument().getLineStartOffset(startLine),
                        editor.getDocument().getLineEndOffset(endLine)
                ));
                WorkspaceUtil.importSpinnerFile(connection, project, filePath, firstLineText + "\n" + selectLineContent);
            } else if(fileName.endsWith(".properties")){
                if(parent.getName().equals("PageFiles")){
                    importPageFile(connection, project, file);
                }

            } else {
                UIUtil.showErrorNotification(project, "Unsupported File Type, Supports .java .xls", "");
            }
        } catch (Exception ex) {
            logger.error("Deploy Error", ex);
            JOptionPane.showMessageDialog(null, ex.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importPageFile(MatrixConnection connection, Project project, PsiFile file) {
        try {
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/Business/PageFiles";
            //创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            //上传文件
            byte[] content = file.getVirtualFile().contentsToByteArray();
            String originalContent = new String(content, StandardCharsets.UTF_8);
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, file.getName(), originalContent);
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        //编译JPO
                        String res = WorkspaceUtil.runPageImport(connection, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath + "/" + file.getName(), file.getName());
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
            logger.error("Deploy Error", e);
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String encodeToUnicode(String str) {
        if(str == null){
            return null;
        }
        if(isUnicode(str)){
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
            // 检查是否为基本多文种平面之外的字符(需要代理对表示)
            if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
                return true;
            }
            // 检查是否为非ASCII字符
            if (c > 127) {
                return true;
            }
        }
        return false;
    }

    private void importJPOFile(MatrixConnection connection, Project project, String filePath, String codeContent) {
        try {
            File jpoFile = new File(filePath);
            if (!jpoFile.exists()) {
                throw new RuntimeException("File not found.");
            }
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            //创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), codeContent);
            //编译JPO
            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        //编译JPO
                        String res = WorkspaceUtil.runJPOImport(connection, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath, jpoName);
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
            logger.error("Deploy Error", e);
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
