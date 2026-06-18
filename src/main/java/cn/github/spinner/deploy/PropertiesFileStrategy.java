package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
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
        return WorkspaceUtil.runPageImportBatch(context.getProject(), context.getMatrixConnection(), remoteSpinnerDir, remoteRelativePath, fileNames);
    }

    @Override
    protected void afterDeploySuccess(String fullRemoteSpinnerDir, String remoteBaseDir) {
        ProgressManager.getInstance().run(new TrackedBackgroundTask(context.getProject(), TitleConstant.SPINNER_DEPLOY) {
            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(SpinnerBundle.message("progress.recache.page"));
                try {
                    WorkspaceUtil.deleteRemoteTempDir(context.getMatrixConnection(), fullRemoteSpinnerDir, remoteBaseDir);
                    WorkspaceUtil.reloadPageCache(context.getMatrixConnection());
                } catch (Exception e) {
                    handleException(e);
                }
            }
        });
    }



}
