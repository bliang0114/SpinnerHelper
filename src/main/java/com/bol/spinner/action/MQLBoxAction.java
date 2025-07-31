package com.bol.spinner.action;

import com.bol.spinner.auth.SpinnerToken;
import com.bol.spinner.ui.MQL;
import com.bol.spinner.util.SpinnerNotifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import matrix.db.Context;
import org.jetbrains.annotations.NotNull;

public class MQLBoxAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        Context context = SpinnerToken.context;
        if (context == null) {
            SpinnerNotifier.showWarningNotification(project, "Not Login, Please Login First", "");
            return;
        }
        new MQL().main();
    }
}
