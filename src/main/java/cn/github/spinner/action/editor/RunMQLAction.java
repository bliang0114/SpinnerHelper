package cn.github.spinner.action.editor;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.SpinnerSettings;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.execution.MQLExecutorToolWindow;
import cn.github.spinner.task.ExecuteMQLCommand;
import cn.github.spinner.util.ConsoleManager;
import cn.github.spinner.util.EditorUtil;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.util.StrUtil;
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
        if (project == null) return;

        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null) {
            UIUtil.showWarningNotification(project, UserInput.NOTIFICATION_TITLE_MQL_EXECUTE, "Please connect to a matrix server first.");
            return;
        }
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        String consoleName = editor.getVirtualFile().getName();
        String selectedText = EditorUtil.getSelectedText(editor);
        log.info("Selected Text: {}", selectedText);
        List<String> commandList;
        SpinnerSettings spinnerSettings = SpinnerSettings.getInstance(project);
        if (StrUtil.isNotEmpty(selectedText)) {
            commandList = List.of(selectedText.split(spinnerSettings.getLineDelimiter()));
        } else {
            commandList = List.of(EditorUtil.getLineContent(editor).split(spinnerSettings.getLineDelimiter()));
        }
        // 去除注释行和空行
        commandList = commandList.stream()
                .filter(command -> !command.isEmpty() && !command.startsWith("#"))
                .map(command -> command.replaceAll("\n", " ").trim()).toList();
        log.info("commandList: {}", commandList);
        if (!commandList.isEmpty()) {
            ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
            if (consoleManager == null) {
                consoleManager = new ConsoleManager(project, consoleName, editor.getVirtualFile());
                UserInput.getInstance().putConsole(project, consoleName, consoleManager);
            }
            MQLExecutorToolWindow toolWindow = UIUtil.getMQLExecutorToolWindow(project);
            if (toolWindow != null) {
                toolWindow.addNodeToTree(consoleName);
            }
            ExecuteMQLCommand executeMQLCommand = new ExecuteMQLCommand(project, consoleName, commandList);
            executeMQLCommand.queue();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        e.getPresentation().setEnabled(connection != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
