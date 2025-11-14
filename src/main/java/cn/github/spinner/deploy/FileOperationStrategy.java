package cn.github.spinner.deploy;

import com.intellij.psi.PsiElement;

import java.util.List;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public interface FileOperationStrategy {

    void processSingleFile(PsiElement file);

    default void processBatchFiles(List<PsiElement> files) {
        for (PsiElement file : files) {
            processSingleFile(file);
        }
    }

    String getSupportedFileExtension();
}
