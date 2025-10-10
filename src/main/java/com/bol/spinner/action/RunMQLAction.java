package com.bol.spinner.action;

import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.editor.ui.MQLConsoleEditor;
import com.bol.spinner.task.MQLCommandExecutor;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class RunMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if  (project == null) return;

        if (SpinnerToken.connection == null) {
            UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
            return;
        }
        MQLConsoleEditor editor = UIUtil.getMQLEditor(project);
        log.info("editor= {}",editor);
        if (editor == null) return ;

        List<String> commandList = editor.getSelectedLines();
        if (commandList.isEmpty()) {
            commandList = editor.getAllLines();
        }
        // 去除注释行和空行
        commandList = commandList.stream().filter(command -> !command.isEmpty() && !command.startsWith("#")).toList();
        log.info("commandList= {}",commandList);
        if (!commandList.isEmpty()) {
            MQLCommandExecutor executor = new MQLCommandExecutor(project, commandList);
            executor.queue();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
