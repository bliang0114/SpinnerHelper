package com.bol.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.hutool.core.util.StrUtil;
import com.bol.spinner.config.SpinnerSettings;
import com.bol.spinner.config.SpinnerToken;
import com.bol.spinner.task.MQLCommandExecutor;
import com.bol.spinner.util.EditorUtil;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
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

        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, "Not Login, Please Login First", "");
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return ;

        String selectedText = EditorUtil.getSelectedText(editor);
        log.info("Selected Text: {}", selectedText);
        List<String> commandList;
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        if (StrUtil.isNotEmpty(selectedText)) {
            commandList = List.of(selectedText.split(spinnerSettings.getLineDelimiter()));
        }else{
            commandList = List.of(EditorUtil.getLineContent(editor).split(spinnerSettings.getLineDelimiter()));
        }
        // 去除注释行和空行
        commandList = commandList.stream()
                .filter(command -> !command.isEmpty() && !command.startsWith("#"))
                .map(command -> command.replaceAll("\n", " ").trim()).toList();
        log.info("commandList: {}", commandList);
        if (!commandList.isEmpty()) {
            MQLCommandExecutor executor = new MQLCommandExecutor(project, commandList);
            executor.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return ;
        }
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
