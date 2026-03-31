package cn.github.spinner.action.basic;

import cn.github.spinner.context.UserInput;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.util.ConsoleManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.OptionalInt;

@Slf4j
public class MQLNewConsoleAction extends AnAction {
    private static final String CONSOLE_PREFIX = "MQL Console ";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) return;

        Map<String, ConsoleManager> consoleBeanMap = UserInput.getInstance().mqlConsole.get(project);
        int max = 1;
        if (consoleBeanMap != null) {
            OptionalInt maxOptional = consoleBeanMap.values().stream()
                    .filter(console -> !console.isPhysicalFile())
                    .map(ConsoleManager::getConsoleName)
                    .mapToInt(this::extractConsoleIndex)
                    .filter(index -> index > 0)
                    .max();
            max = maxOptional.isPresent() ? maxOptional.getAsInt() + 1 : max;
        }
        String consoleName = CONSOLE_PREFIX + max;
        LightVirtualFile consoleFile = new LightVirtualFile(consoleName);
        consoleFile.setLanguage(MQLLanguage.INSTANCE);
        consoleFile.setWritable(true);
        consoleFile.setCharset(StandardCharsets.UTF_8);
        ConsoleManager consoleManager = new ConsoleManager(project, consoleName, consoleFile);
        UserInput.getInstance().putConsole(project, consoleManager.getConsoleName(), consoleManager);
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(consoleFile, true);
    }

    private int extractConsoleIndex(String consoleName) {
        if (!consoleName.startsWith(CONSOLE_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(consoleName.substring(CONSOLE_PREFIX.length()).trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
