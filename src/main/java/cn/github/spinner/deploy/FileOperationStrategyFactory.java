package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Slf4j
public class FileOperationStrategyFactory {
    private  static final Map<String,Class<? extends FileOperationStrategy>> STRATEGY_MAP = new HashMap<>();
    static {
        STRATEGY_MAP.put(FileConstant.SUFFIX_JAVA,JpoFileStrategy.class);
        STRATEGY_MAP.put(FileConstant.SUFFIX_XLS,XlsFileStrategy.class);
        STRATEGY_MAP.put(FileConstant.SUFFIX_PRO,PropertiesFileStrategy.class);
    }



    public static FileOperationStrategy getStrategy(FileOperationContext context, PsiElement psiElement) {
        String extension = psiElement.getContainingFile().getVirtualFile().getExtension();
        if(extension == null) {
            return null;
        }
        Class<? extends FileOperationStrategy> strategyClass = STRATEGY_MAP.get(extension);
        if(strategyClass == null) {
            return null;
        }
        try {
            return strategyClass.getDeclaredConstructor(FileOperationContext.class).newInstance(context);
        } catch (Exception e) {
            log.error("Failed to create instance of strategy class: {}", strategyClass.getSimpleName(), e);
            return null;
        }
    }
}
