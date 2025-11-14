package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class JpoFileStrategy extends AbstractFileStrategy {

    public JpoFileStrategy(FileOperationContext context) {
        super(context);
    }

    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_JAVA;
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
        return WorkspaceUtil.runJPOImportBath(context.getMatrixConnection(), remoteSpinnerDir, remoteRelativePath, fileNames);
    }

}
