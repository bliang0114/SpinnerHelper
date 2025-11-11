package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.action.editor.SpinnerBatchDeployAction;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class JpoFileStrategy implements FileOperationStrategy {

    @Override
    public void processSingleFile(FileOperationContext context, PsiElement file) {

    }

    @Override
    public void processBatchFiles(FileOperationContext context, List<PsiElement> psiElementList) {
        Project project = context.getProject();
        MatrixConnection connection = context.getMatrixConnection();
        log.info("处理 jpo.......");
        String filePath = psiElementList.getFirst().getContainingFile().getVirtualFile().getPath();
        try {
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            log.info("spinnerPath==>{}", spinnerPath);
            log.info("remoteBaseDir==>{}", remoteBaseDir);
            log.info("remoteSpinnerDir==>{}", remoteSpinnerDir);
            log.info("remoteRelativePath==>{}", remoteRelativePath);
            // 创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            List<String> fileNames = new ArrayList<>();
            for (PsiElement psiElement : psiElementList) {
                String path = psiElement.getContainingFile().getVirtualFile().getPath();
                File jpoFile = new File(path);
                // 上传文件
                WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), psiElement.getContainingFile().getText());
                String fileName = psiElement.getContainingFile().getName();
                fileNames.add(fileName);
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        // 编译JPO
                        String res = WorkspaceUtil.runJPOImportBath(connection, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath, fileNames);
                        log.info("res==>{}", res);
                        if (CharSequenceUtil.isEmpty(res)|| (!res.contains("Error") && !res.contains("failed"))) {
                            res = "Deploy success.";
                            // 删除临时目录
                            WorkspaceUtil.deleteRemoteTempDir(connection, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir );
                        } else {
                            res = "Deploy failed, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
                        }
                        UIUtil.showNotification(project, "Deploy Result", res);
                    } catch (Exception e) {
                        UIUtil.showErrorNotification(project, "Error", e.getLocalizedMessage());
                    }
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_JAVA;
    }
}
