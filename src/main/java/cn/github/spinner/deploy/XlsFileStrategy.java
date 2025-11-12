package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class XlsFileStrategy extends AbstractFileStrategy {
    private static final Logger LOGGER = Logger.getInstance(XlsFileStrategy.class);

    @Override
    public void processSingleFile(FileOperationContext context, PsiElement file) {
        String filePath = file.getContainingFile().getVirtualFile().getPath();
        WorkspaceUtil.importSpinnerFile(context.getMatrixConnection(), context.getProject(), filePath, file.getText());
        LOGGER.info("processSingleFile: " + filePath);
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
    protected String executeDeployCommand(MatrixConnection connection, String remoteSpinnerDir, String remoteRelativePath, List<String> fileNames) throws Exception {
         // todo-fzhang 使用阳光部署方式
        return WorkspaceUtil.runSpinnerImport(connection, remoteSpinnerDir);
    }
}
