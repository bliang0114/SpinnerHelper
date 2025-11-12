package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.UIUtil;
import cn.github.spinner.util.WorkspaceUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        return WorkspaceUtil.runSpinnerImport(connection, remoteSpinnerDir);
    }
}
