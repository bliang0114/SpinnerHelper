package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.constant.FileConstant;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fzhang
 * @date 2025/11/7
 */
public class SpinnerBatchDeployAction extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(SpinnerBatchDeployAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection== null) {
            UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
            return;
        }
        DataContext dataContext = e.getDataContext();
        Map<String, List<PsiElement>> psiFileMap = getSelectedElements(dataContext);
        importJpoFiles(connection, psiFileMap);
        importSpinnerFiles(connection, psiFileMap);
        importPropertiesFiles(connection, psiFileMap);

    }

    private void importPropertiesFiles(MatrixConnection connection, Map<String, List<PsiElement>> psiFileMap) {
        List<PsiElement> proPsiElements = psiFileMap.get(FileConstant.SUFFIX_PRO);
        if(proPsiElements == null || proPsiElements.isEmpty()){
            return;
        }
    }

    private void importSpinnerFiles(MatrixConnection connection, Map<String, List<PsiElement>> psiFileMap) {
        List<PsiElement> xlsPsiElements = psiFileMap.get(FileConstant.SUFFIX_XLS);
        if(xlsPsiElements == null || xlsPsiElements.isEmpty()){
            return;
        }

    }

    private void importJpoFiles(MatrixConnection connection, Map<String, List<PsiElement>> psiFileMap) {
        List<PsiElement> jpoPsiElements = psiFileMap.get(FileConstant.SUFFIX_JPO);
        if(jpoPsiElements == null || jpoPsiElements.isEmpty()){
            return;
        }
        for (PsiElement jpoPsiElement : jpoPsiElements) {
            String jpoFilePath = jpoPsiElement.getContainingFile().getVirtualFile().getPath();
        }
    }


    protected Map<String, List<PsiElement>> getSelectedElements(@NotNull DataContext dataContext) {
        Map<String,List<PsiElement>> psiFileMap = new HashMap<>();
        PsiElement[] psiElements = ObjectUtils.notNull(PlatformCoreDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext), PsiElement.EMPTY_ARRAY);
        List<PsiElement> psiElementList = getAllChildren(psiElements);
        LOGGER.debug("psiElementList:{}", psiElementList);
        for (PsiElement psiElement : psiElementList) {
            String fileName = psiElement.getContainingFile().getName();
            LOGGER.debug("fileName:{}", fileName);
            if (fileName.endsWith(FileConstant.SUFFIX_JPO)) {
                psiFileMap.computeIfAbsent(FileConstant.SUFFIX_JPO, k -> new ArrayList<>()).add(psiElement);
            } else if (fileName.endsWith(FileConstant.SUFFIX_XLS)) {
                psiFileMap.computeIfAbsent(FileConstant.SUFFIX_XLS, k -> new ArrayList<>()).add(psiElement);
            } else if (fileName.endsWith(FileConstant.SUFFIX_PRO)) {
                psiFileMap.computeIfAbsent(FileConstant.SUFFIX_PRO, k -> new ArrayList<>()).add(psiElement);
            }
        }
        return psiFileMap;
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
