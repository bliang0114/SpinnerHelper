package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/11
 */
@Slf4j
public abstract class AbstractFileStrategy implements FileOperationStrategy {

    @Override
    public void processSingleFile(FileOperationContext context, PsiElement file) {
        log.info("processSingleFile, file==>{}", file);
    }

    @Override
    public void processBatchFiles(FileOperationContext context, List<PsiElement> files) {
        Project project = context.getProject();
        MatrixConnection connection = context.getMatrixConnection();
        try {
            // 1. 获取基础路径信息
            String firstFilePath = getFirstFilePath(files);
            String spinnerPath = WorkspaceUtil.extractSpinnerSubPath(firstFilePath);
            String remoteTmpDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner-" + UUID.randomUUID();
            String remoteRelativePath = buildRemoteRelativePath(remoteSpinnerDir, spinnerPath);
            log.info("spinnerPath==>{}, remoteBaseDir==>{}, remoteSpinnerDir==>{}, remoteRelativePath==>{}", spinnerPath, remoteTmpDir, remoteSpinnerDir, remoteRelativePath);

            // 2. 创建远程临时目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteTmpDir, remoteRelativePath);

            // 3. 上传文件并收集文件名
            List<String> fileNames = uploadFiles(connection, remoteTmpDir, remoteRelativePath, files);

            // 4. 执行部署任务
            executeDeployTask(project, connection, remoteTmpDir, remoteSpinnerDir, remoteRelativePath, fileNames);

        } catch (Exception e) {
            handleException(e);
        }

    }
    /**
     * 构建远程相对路径
     */
    protected abstract String buildRemoteRelativePath(String remoteSpinnerDir, String spinnerPath);

    /**
     * 执行部署命令
     */
    protected abstract String executeDeployCommand(MatrixConnection connection, String remoteSpinnerDir,
                                                   String remoteRelativePath, List<String> fileNames) throws Exception;

    /**
     * 上传文件公共逻辑
     */
    protected List<String> uploadFiles(MatrixConnection connection, String remoteBaseDir,
                                       String remoteRelativePath, List<PsiElement> files) throws Exception {
        List<String> fileNames = new ArrayList<>(files.size());
        for (PsiElement element : files) {
            String fileName = element.getContainingFile().getName();
            fileNames.add(fileName);
            WorkspaceUtil.uploadTempFile(
                    connection,
                    remoteBaseDir + "/" + remoteRelativePath,
                    fileName,
                    element.getContainingFile().getText()
            );
        }
        return fileNames;
    }

    /**
     * 执行部署任务（带进度提示）
     */
    protected void executeDeployTask(Project project, MatrixConnection connection, String remoteBaseDir,
                                     String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting deployment...");
                try {
                    String fullRemoteSpinnerDir = remoteBaseDir + "/" + remoteSpinnerDir;
                    String fullRemoteRelativePath = remoteBaseDir + "/" + remoteRelativePath;

                    // 调用子类实现的部署命令
                    String res = executeDeployCommand(connection, fullRemoteSpinnerDir, fullRemoteRelativePath, fileNames);
                    log.info("Deploy result==>{}", res);

                    String title= "Deploy success";
                    // 处理部署结果
                    if (CharSequenceUtil.isEmpty(res) || (!res.contains("Error") && !res.contains("failed"))) {
                        WorkspaceUtil.deleteRemoteTempDir(connection, fullRemoteSpinnerDir, remoteBaseDir);
                    } else {
                        title= "Deploy failed";
                    }
                    UIUtil.showNotification(project, title, res);

                } catch (Exception e) {
                    UIUtil.showErrorNotification(project, "Error", e.getLocalizedMessage());
                }
            }
        });
    }


    String getFirstFilePath(List<PsiElement> files) {
        return files.getFirst().getContainingFile().getVirtualFile().getPath();
    }

    /**
     * 统一异常处理
     */
    protected void handleException(Exception e) {
        log.error("Batch processing failed", e);
        String message = "部署失败: " + e.getLocalizedMessage();
        JOptionPane.showMessageDialog(null, message, "Deploy Error", JOptionPane.ERROR_MESSAGE);
    }
}
