package com.bol.spinner.action;

import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.editor.ui.MQLConsoleEditor;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Slf4j
public class RunMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if  (project == null) return;
//        if (SpinnerToken.connection == null) {
//            UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
//            return;
//        }
        MQLConsoleEditor editor = UIUtil.getMQLEditor(project);
        if (editor == null) return ;

        log.info("getSelectedText: {}", editor.getSelectedText());
        log.info("getSelectedLines: {}", editor.getSelectedLines());
        log.info("getCurrentLine: {}", editor.getCurrentLine());
    }
}
