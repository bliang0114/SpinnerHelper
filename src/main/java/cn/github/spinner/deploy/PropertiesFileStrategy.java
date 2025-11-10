package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
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
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public class PropertiesFileStrategy implements FileOperationStrategy{
   private static final Logger LOGGER = Logger.getInstance(PropertiesFileStrategy.class);
    @Override
    public void processSingleFile(FileOperationContext context,PsiElement file) {
        LOGGER.info("处理 properties 文件 ...");
        try {
            MatrixConnection connection = context.getMatrixConnection();
            Project project = context.getProject();
            String remoteBaseDir = WorkspaceUtil.getTmpDir(connection);
            String remoteSpinnerDir = "spinner" + new Random().nextInt();
            String remoteRelativePath = remoteSpinnerDir + "/Business/PageFiles";
            //创建目录
            WorkspaceUtil.createRemoteTempDir(connection, remoteBaseDir, remoteRelativePath);
            //上传文件
            byte[] content = file.getContainingFile().getVirtualFile().contentsToByteArray();
            String originalContent = new String(content, StandardCharsets.UTF_8);
            WorkspaceUtil.uploadTempFile(connection, remoteBaseDir + "/" + remoteRelativePath, file.getContainingFile().getName(), originalContent);
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Spinner Deploy") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    indicator.setText("Starting deployment...");
                    try {
                        //编译JPO
                        String res = WorkspaceUtil.runPageImport(connection, remoteBaseDir + "/" + remoteSpinnerDir,
                                remoteBaseDir + "/" + remoteRelativePath + "/" + file.getContainingFile().getName(), file.getContainingFile().getName());
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
            LOGGER.error("Deploy Error", e);
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Deploy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_PRO;
    }
}
