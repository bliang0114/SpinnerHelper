package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.deploy.BatchFileCommand;
import cn.github.spinner.deploy.FileOperationCommand;
import cn.github.spinner.deploy.FileOperationContext;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author fzhang
 * @date 2025/11/7
 */
public class SpinnerBatchDeployAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(SpinnerBatchDeployAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }


        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
            return;
        }
        PsiElement[] psiElements = ObjectUtils.notNull(PlatformCoreDataKeys.PSI_ELEMENT_ARRAY.getData(e.getDataContext()), PsiElement.EMPTY_ARRAY);
        List<PsiElement> psiElementList = getAllChildren(psiElements);
        FileOperationContext context = new FileOperationContext(project, connection);
        FileOperationCommand fileOperationCommand = new BatchFileCommand(context, psiElementList);
        fileOperationCommand.deploy();

    }


    protected List<PsiElement> getAllChildren(@NotNull PsiElement[] psiElements) {
        List<PsiElement> psiElementList = new ArrayList<>();
        for (PsiElement psiElement : psiElements) {
            if (psiElement instanceof PsiDirectory) {
                psiElementList.addAll(getAllChildren(psiElement.getChildren()));
            } else {
                psiElementList.add(psiElement);
            }
        }
        return psiElementList;
    }


}
