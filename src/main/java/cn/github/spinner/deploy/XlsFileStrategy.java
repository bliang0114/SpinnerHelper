package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class XlsFileStrategy extends AbstractFileStrategy {
    private static final Logger LOGGER = Logger.getInstance(XlsFileStrategy.class);

    public XlsFileStrategy(FileOperationContext context) {
        super(context);
    }


    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_XLS;
    }

    @Override
    protected String buildRemoteRelativePath(String remoteSpinnerDir, String spinnerPath) {
        return remoteSpinnerDir + "/" + spinnerPath;
    }

    @Override
    protected String buildSpinnerSubPath(String firstFilePath) {
        return WorkspaceUtil.extractSpinnerSubPath(firstFilePath);
    }

    @Override
    protected String executeDeployCommand(String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) throws Exception {
        return WorkspaceUtil.runSpinnerImport(context.getMatrixConnection(), remoteSpinnerDir);
    }

    @Override
    protected void afterDeploySuccess(String fullRemoteSpinnerDir, String remoteBaseDir) {
        super.afterDeploySuccess(fullRemoteSpinnerDir, remoteBaseDir);
        ProgressManager.getInstance().run(new TrackedBackgroundTask(context.getProject(), TitleConstant.SPINNER_DEPLOY) {
            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(SpinnerBundle.message("progress.delete.deploy.temp.dir"));
                try {
                    WorkspaceUtil.reloadSpinnerCache(context.getMatrixConnection());
                } catch (Exception e) {
                    handleException(e);
                }
            }
        });
    }
}
