package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class PropertiesFileStrategy extends AbstractFileStrategy{
    @Override
    public void processSingleFile(FileOperationContext context,PsiElement file) {

    }

    @Override
    public String getSupportedFileExtension() {
        return FileConstant.SUFFIX_PRO;
    }

    @Override
    protected String buildRemoteRelativePath(String remoteSpinnerDir, String spinnerPath) {
        return remoteSpinnerDir + "/Business/PageFiles";
    }

    @Override
    protected String executeDeployCommand(MatrixConnection connection, String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) throws Exception {
        return WorkspaceUtil.runPageImportBatch(connection, remoteSpinnerDir, remoteRelativePath, fileNames);
    }
}
