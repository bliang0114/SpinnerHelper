package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;

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
}
