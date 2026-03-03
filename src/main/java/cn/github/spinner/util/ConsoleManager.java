package cn.github.spinner.util;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import lombok.Data;

@Data
public class ConsoleManager {
    private final ConsoleView consoleView;
    private final ConsolePrinter consolePrinter;
    private String consoleName;
    private VirtualFile consoleFile;

    public ConsoleManager(Project project, String consoleName, VirtualFile consoleFile) {
        this.consoleView = new ConsoleViewImpl(
                project,
                GlobalSearchScope.allScope(project),
                true,  // viewer mode
                false   // ← 关键：false = 禁用循环缓冲区
        );
        this.consolePrinter = new ConsolePrinter(project, consoleView);
        this.consoleName = consoleName;
        this.consoleFile = consoleFile;
    }

    public boolean isPhysicalFile() {
        return consoleFile.isInLocalFileSystem();
    }

    public void print(String message) {
        consolePrinter.print(message);
    }

    public void print(String message, ConsoleViewContentType contentType) {
        consolePrinter.print(message, contentType);
    }

    public void error(String message) {
        consolePrinter.print("[ERROR] " + message, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public void warn(String message) {
        consolePrinter.print("[WARN] " + message, ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }

    public void debug(String message) {
        consolePrinter.print("[DEBUG] " + message, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }

    public void clear() {
        consolePrinter.clear();
    }
}