package cn.github.spinner.deploy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;


/**
 * @author fzhang
 * @date 2025/11/10
 */
public class SingleFileCommand implements FileOperationCommand {
    private static final Logger LOGGER = Logger.getInstance(SingleFileCommand.class);
    private final PsiElement file;
    private final FileOperationContext context;


    public SingleFileCommand(FileOperationContext context,PsiElement file) {
        this.file = file;
        this.context = context;
    }

    @Override
    public void deploy() {
        LOGGER.info("处理单个文件....");
    }
}
