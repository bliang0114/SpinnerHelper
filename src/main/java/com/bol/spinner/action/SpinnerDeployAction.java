package com.bol.spinner.action;

import com.bol.spinner.auth.SpinnerToken;
import com.bol.spinner.util.SpinnerNotifier;
import com.bol.spinner.util.WorkspaceUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.StartupUiUtil;
import matrix.db.Context;
import matrix.util.MatrixException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Random;

public class SpinnerDeployAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            Project project = e.getData(CommonDataKeys.PROJECT);
            PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            if(file == null){
                SpinnerNotifier.showWarningNotification(project, "File is null", "");
                return;
            }
            String filePath = file.getViewProvider().getVirtualFile().getPath();
            PsiDirectory parent = file.getParent();
            if(parent == null){
                SpinnerNotifier.showWarningNotification(project, "Parent Dir is null", "");
                return;
            }
            String fileName = file.getName();
            Context context = SpinnerToken.context;
            if (context == null) {
                SpinnerNotifier.showWarningNotification(project, "Not Login, Please Login First", "");
                return;
            }
            if (fileName.endsWith(".java")) {
                String javaContent = file.getText();
                importJPOFile(context, project, filePath, javaContent);
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
                importSpinnerFile(context, project, filePath, firstLineText + "\n" + selectLineContent);
            } else if(fileName.endsWith(".properties")){
                if(parent.getName().equals("PageFiles")){
                    importPageFile(context, project, file);
                }

            } else {
                SpinnerNotifier.showErrorNotification(project, "Unsupported File Type, Supports .java .xls", "");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }


    }

    private void importPageFile(Context context, Project project, PsiFile file) {
        try {
            String remoteBaseDir = WorkspaceUtil.getTmpDir(context);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/Business/PageFiles";
            //创建目录
            WorkspaceUtil.createRemoteTempDir(context, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(context, remoteBaseDir + "/" + remoteRelativePath, file.getName(), file.getText());
            String res = WorkspaceUtil.runPageImport(context, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath + "/" + file.getName(), file.getName());
            if (res == null || res.isEmpty()) {
                res = "Deploy success, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
            }
            showMessage(res);
        } catch (MatrixException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importJPOFile(Context context, Project project, String filePath, String codeContent) {
        try {
            File jpoFile = new File(filePath);
            if (!jpoFile.exists()) {
                throw new RuntimeException("File not found.");
            }
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(context);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            //创建目录
            WorkspaceUtil.createRemoteTempDir(context, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(context, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), codeContent);
            //编译JPO
            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");
            String res = WorkspaceUtil.runJPOImport(context, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath, jpoName);
            if (res == null || res.isEmpty()) {
                res = "Deploy success, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
            }
            showMessage(res);
        } catch (MatrixException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importSpinnerFile(Context context, Project project, String filePath, String content) {
        try {
            File spinnerFile = new File(filePath);
            if (!spinnerFile.exists()) {
                throw new RuntimeException("File not found.");
            }
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(context);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            //创建目录
            WorkspaceUtil.createRemoteTempDir(context, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(context, remoteBaseDir + "/" + remoteRelativePath, spinnerFile.getName(), content);
            //编译JPO
            String res = WorkspaceUtil.runSpinnerImport(context, remoteBaseDir + "/" + remoteSpinnerDir);
            if (res == null || res.isEmpty()) {
                res = "Deploy success, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
            }
            showMessage(res);
        } catch (MatrixException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showMessage(String message) {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL);
        textArea.setHighlightCurrentLine(false);
        if (StartupUiUtil.INSTANCE.isDarkTheme()) {
            textArea.setBackground(Color.decode("#2B2D30"));
            textArea.setForeground(Color.WHITE);
        } else {
            textArea.setBackground(JBColor.WHITE);
            textArea.setForeground(JBColor.BLACK);
        }
        textArea.setEditable(false);
        textArea.setText(message);
        // 将文本区域放入滚动面板
        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        JOptionPane.showMessageDialog(null, scrollPane, "Deploy Result", JOptionPane.INFORMATION_MESSAGE);
    }

}
