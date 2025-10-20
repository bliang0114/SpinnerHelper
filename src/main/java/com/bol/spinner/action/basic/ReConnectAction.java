package com.bol.spinner.action.basic;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ReConnectAction extends EnvironmentTbActionAdapter {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        connect(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        try {
            e.getPresentation().setEnabled(enableAction(e));
        } catch (Exception ex) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
