package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.WorkspaceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public class XlsFileStrategy implements FileOperationStrategy {
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
}
