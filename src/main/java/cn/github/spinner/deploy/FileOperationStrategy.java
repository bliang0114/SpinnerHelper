package cn.github.spinner.deploy;

import com.intellij.psi.PsiElement;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public interface FileOperationStrategy {

    void processSingleFile(FileOperationContext context, PsiElement file);

    default void processBatchFiles(FileOperationContext context, List<PsiElement> files) {
        for (PsiElement file : files) {
            processSingleFile(context, file);
        }
    }

    String getSupportedFileExtension();
}
