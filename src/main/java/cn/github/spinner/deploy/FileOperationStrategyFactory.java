package cn.github.spinner.deploy;

import com.intellij.psi.PsiElement;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public class FileOperationStrategyFactory {
    private  static final Map<String,FileOperationStrategy> STRATEGY_MAP = new HashMap<>();
    static {
        registerStrategy(new JpoFileStrategy());
        registerStrategy(new XlsFileStrategy());
        registerStrategy(new PropertiesFileStrategy());
    }

    private static void registerStrategy(FileOperationStrategy strategy) {
        STRATEGY_MAP.put(strategy.getSupportedFileExtension(), strategy);
    }

    public static FileOperationStrategy getStrategy(PsiElement psiElement) {
        String extension = psiElement.getContainingFile().getVirtualFile().getExtension();
        if(extension == null) {
            return null;
        }
        return STRATEGY_MAP.get(extension);
    }
}
