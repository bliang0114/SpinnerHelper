package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.action.editor.SpinnerBatchDeployAction;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.Random;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public class JpoFileStrategy implements FileOperationStrategy {
    private static final Logger LOGGER = Logger.getInstance(JpoFileStrategy.class);

    @Override
    public void processSingleFile(FileOperationContext context,PsiElement file) {
         // todo-fzhang 待优化
        Project project = context.getProject();
        MatrixConnection connection = context.getMatrixConnection();
        LOGGER.info("处理 jpo.......");
        String filePath = file.getContainingFile().getVirtualFile().getPath();
        try {
            File jpoFile = new File(filePath);
            if (!jpoFile.exists()) {
                throw new RuntimeException("File not found.");
            }
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(filePath);
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/" + spinnerPath;
            // 创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            // 上传文件
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, jpoFile.getName(), file.getText());
            // 编译JPO
            String jpoName = jpoFile.getName().replace("_mxJPO.java", "");

            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        // 编译JPO
                        String res = WorkspaceUtil.runJPOImport(connection, remoteBaseDir + "/" + remoteSpinnerDir, remoteBaseDir + "/" + remoteRelativePath, jpoName);
                        if (res == null || res.isEmpty()) {
                            res = "Deploy success, log path is: " + remoteBaseDir + "/" + remoteSpinnerDir + "/" + "spinner.log";
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
