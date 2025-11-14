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
    protected FileOperationContext context;

    public AbstractFileStrategy(FileOperationContext context) {
        this.context = context;
    }

    @Override
    public void processSingleFile(PsiElement file) {
        log.info("processSingleFile, file==>{}", file);
    }

    @Override
    public void processBatchFiles(List<PsiElement> files) {
        Project project = context.getProject();
        MatrixConnection connection = context.getMatrixConnection();
        try {
            // 1. 获取基础路径信息
            String firstFilePath = getFirstFilePath(files);
            String spinnerSubPath = buildSpinnerSubPath(firstFilePath);
            String remoteTmpDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner-" + UUID.randomUUID();
            String remoteRelativePath = buildRemoteRelativePath(remoteSpinnerDir, spinnerSubPath);
            log.info("spinnerSubPath==>{}, remoteBaseDir==>{}, remoteSpinnerDir==>{}, remoteRelativePath==>{}", spinnerSubPath, remoteTmpDir, remoteSpinnerDir, remoteRelativePath);

            // 2. 创建远程临时目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteTmpDir, remoteRelativePath);

            // 3. 上传文件并收集文件名
            List<String> fileNames = uploadFiles(remoteTmpDir, remoteRelativePath, files);

            // 4. 执行部署任务
            executeDeployTask(remoteTmpDir, remoteSpinnerDir, remoteRelativePath, fileNames);

        } catch (Exception e) {
            handleException(e);
        }

    }

    /**
     * 构建远程相对路径
     */
    protected abstract String buildRemoteRelativePath(String remoteSpinnerDir, String spinnerPath);

    /**
     * 文件所在目录spinner后部分内容
     * 如spinner/Business/SourceFiles，则返回 Business/SourceFiles
     * 部署 properties文件时，返回空字符串即可
     * @param firstFilePath 远程Spinner目录
     * @return 文件所在目录spinner后部分内容
     */
    protected abstract String buildSpinnerSubPath(String firstFilePath);

    /**
     * 执行部署命令
     */
    protected abstract String executeDeployCommand(String remoteSpinnerDir,
                                                   String remoteRelativePath, List<String> fileNames) throws Exception;

    /**
     * 上传文件公共逻辑
     */
    protected List<String> uploadFiles(String remoteBaseDir,
                                       String remoteRelativePath, List<PsiElement> files) throws Exception {
        MatrixConnection connection = context.getMatrixConnection();
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
     * 执行部署任务
     */
    protected void executeDeployTask(String remoteBaseDir, String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) {
        ProgressManager.getInstance().run(new Task.Backgroundable(context.getProject(), "Spinner Deploy") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting deployment...");
                try {
                    String fullRemoteSpinnerDir = remoteBaseDir + "/" + remoteSpinnerDir;
                    String fullRemoteRelativePath = remoteBaseDir + "/" + remoteRelativePath;

                    // 调用子类实现的部署命令
                    String res = executeDeployCommand( fullRemoteSpinnerDir, fullRemoteRelativePath, fileNames);
                    log.info("Deploy result==>{}", res);

                    String title = "Deploy success";
                    // 处理部署结果
                    if (CharSequenceUtil.isEmpty(res) || (!res.contains("Error") && !res.contains("failed"))) {
                        afterDeploySuccess(fullRemoteSpinnerDir, remoteBaseDir);
                    } else {
                        title = "Deploy failed";
                    }
                    UIUtil.showNotification(context.getProject(), title, res == null ? "success" : res);

                } catch (Exception e) {
                    UIUtil.showErrorNotification(context.getProject(), "Error", e.getLocalizedMessage());
                }
            }
        });
    }

    protected  void afterDeploySuccess(String fullRemoteSpinnerDir, String remoteBaseDir) {
        ProgressManager.getInstance().run(new Task.Backgroundable(context.getProject(), "Spinner Deploy") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting delete deploy temp dir...");
                try {
                    WorkspaceUtil.deleteRemoteTempDir(context.getMatrixConnection(), fullRemoteSpinnerDir, remoteBaseDir);
                } catch (Exception e) {
                    handleException(e);
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
