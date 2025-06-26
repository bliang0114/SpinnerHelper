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
import com.intellij.psi.PsiFile;
import matrix.db.Context;
import matrix.util.MatrixException;

import javax.swing.*;
import java.io.File;
import java.util.Random;

public class SpinnerDeployAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        assert file != null;
        String filePath = file.getViewProvider().getVirtualFile().getPath();
        Project project = e.getData(CommonDataKeys.PROJECT);
        String fileName = file.getName();
        Context context = SpinnerToken.context;
        //indeterminate
        if(context == null){
            SpinnerNotifier.showWarningNotification(project, "not login", "");
            return;
        }
        if(fileName.endsWith(".java")){
            String javaContent = file.getText();
            System.out.println("javaContent = \n" + javaContent);
            importJPOFile(context, project, filePath, javaContent);
        }else if(fileName.endsWith(".xls")){
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            assert editor != null;
            Document document = editor.getDocument();
            SelectionModel selectionModel = editor.getSelectionModel();
                int firstLine = 0;
                String firstLineText = editor.getDocument().getText(new TextRange(
                        editor.getDocument().getLineStartOffset(firstLine),
                        editor.getDocument().getLineEndOffset(firstLine)
                ));
                System.out.println("firstLineText = \n" + firstLineText);
                int selectionStart = selectionModel.getSelectionStart();
                int selectionEnd = selectionModel.getSelectionEnd();
                int startLine = document.getLineNumber(selectionStart);
                if(startLine == 0){
                    startLine++;
                }
                int endLine = document.getLineNumber(selectionEnd);
                String selectLineContent = editor.getDocument().getText(new TextRange(
                        editor.getDocument().getLineStartOffset(startLine),
                        editor.getDocument().getLineEndOffset(endLine)
                ));
                System.out.println("currentLineText = \n" + selectLineContent);
                importSpinnerFile(context, project, filePath, firstLineText+"\n"+selectLineContent);
        }else{
            SpinnerNotifier.showErrorNotification(project, "不支持的文件类型", "");
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
            System.out.println("create tmp dir:" + remoteBaseDir + "/" + remoteRelativePath);
            WorkspaceUtil.createRemoteTempDir(context, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(context, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), codeContent);
            //编译JPO
            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");
            String res = WorkspaceUtil.runJPOImport(context, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath, jpoName);
            if(res == null || res.isEmpty()){
                res = "deploy success but no response, see it in " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
            }
            JOptionPane.showMessageDialog(null, res, "Deploy Result", JOptionPane.INFORMATION_MESSAGE);
//            SpinnerNotifier.showNotification(project, "DeployResult", res);
        } catch (MatrixException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
//            SpinnerNotifier.showErrorNotification(project, "DeployError", e.getLocalizedMessage());
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
            System.out.println("create tmp dir:" + remoteBaseDir + "/" + remoteRelativePath);
            WorkspaceUtil.createRemoteTempDir(context, remoteBaseDir, remoteRelativePath);
            //上传文件
            WorkspaceUtil.uploadTempFile(context, remoteBaseDir + "/" + remoteRelativePath, spinnerFile.getName(), content);
            //编译JPO
            String res = WorkspaceUtil.runSpinnerImport(context, remoteBaseDir + "/" + remoteSpinnerDir);
            if(res == null || res.isEmpty()){
                res = "deploy success but no response, see it in " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
            }
            JOptionPane.showMessageDialog(null, res, "Deploy Result", JOptionPane.INFORMATION_MESSAGE);
//            SpinnerNotifier.showNotification(project, "DeployResult", res);
        } catch (MatrixException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
//            SpinnerNotifier.showErrorNotification(project, "DeployError", e.getLocalizedMessage());
        }
    }

}
