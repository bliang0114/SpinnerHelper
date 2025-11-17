package cn.github.spinner.deploy;

import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.constant.TitleConstant;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fzhang
 * @date 2025/11/10
 */
public class BatchFileCommand implements FileOperationCommand {
    private static final Logger LOGGER = Logger.getInstance(BatchFileCommand.class);
    private final List<PsiElement> files;
    private final FileOperationContext context;

    public BatchFileCommand(FileOperationContext context, List<PsiElement> files) {
        this.files = files;
        this.context = context;
    }

    @Override
    public void deploy() {
        ProgressManager.getInstance().run(new Task.Backgroundable(context.getProject(), TitleConstant.SPINNER_DEPLOY) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Starting deployment...");
                try {
                    Map<String, List<PsiElement>> typeFilesMap = files.stream()
                            .filter(file -> file.getContainingFile().getVirtualFile().getExtension() != null)
                            .collect(HashMap::new,
                                    (map, file) -> {
                                        String ext = file.getContainingFile().getVirtualFile().getExtension();
                                        String name = file.getContainingFile().getName();
                                        if (FileConstant.SUFFIX_JAVA.equals(ext) && !name.contains(FileConstant.SUFFIX_JPO)) {
                                            return;
                                        }
                                        map.computeIfAbsent(ext, k -> new ArrayList<>()).add(file);
                                    },
                                    HashMap::putAll);
                    if (typeFilesMap.isEmpty()) {
                        UIUtil.showNotification(context.getProject(), "Spinner Deploy Tip", "不支持的文件");
                        return;
                    }

                    for (Map.Entry<String, List<PsiElement>> entry : typeFilesMap.entrySet()) {
                        PsiElement virtualFile = entry.getValue().getFirst();
                        FileOperationStrategy strategy = FileOperationStrategyFactory.getStrategy(context, virtualFile);
                        if (strategy != null) {
                            strategy.processBatchFiles(entry.getValue());
                        } else {
                            LOGGER.debug("批量部署不支持的文件类型：{}", entry.getKey());
                        }
                    }
                } catch (Exception e) {
                    UIUtil.showErrorNotification(context.getProject(), "Error", e.getLocalizedMessage());
                }

            }
        });

    }
}
