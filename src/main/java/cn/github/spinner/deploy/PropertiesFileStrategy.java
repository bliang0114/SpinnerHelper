package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class PropertiesFileStrategy extends AbstractFileStrategy {
    public PropertiesFileStrategy(FileOperationContext context) {
        super(context);
    }

    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_PRO;
    }

    @Override
    protected String buildRemoteRelativePath(String remoteSpinnerDir, String spinnerPath) {
        return remoteSpinnerDir + "/Business/PageFiles/";
    }

    @Override
    protected String buildSpinnerSubPath(String firstFilePath) {
        return CharSequenceUtil.EMPTY;
    }

    @Override
    protected String executeDeployCommand(String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) throws Exception {
        return WorkspaceUtil.runPageImportBatch(context.getMatrixConnection(), remoteSpinnerDir, remoteRelativePath, fileNames);
    }

    @Override
    protected void afterDeploySuccess(String fullRemoteSpinnerDir, String remoteBaseDir) {
        ProgressManager.getInstance().run(new Task.Backgroundable(context.getProject(), "Spinner Deploy") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting reCache Page...");
                try {
                    WorkspaceUtil.deleteRemoteTempDir(context.getMatrixConnection(), fullRemoteSpinnerDir, remoteBaseDir);
                    WorkspaceUtil.reCachePage(context.getMatrixConnection());
                } catch (Exception e) {
                    handleException(e);
                }
            }
        });
    }



}
