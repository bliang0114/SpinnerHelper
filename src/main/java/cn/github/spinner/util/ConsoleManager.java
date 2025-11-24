package cn.github.spinner.util;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import lombok.Getter;

public class ConsoleManager {
    @Getter
    private final ConsoleView consoleView;
    private final ConsolePrinter consolePrinter;

    public ConsoleManager(Project project) {
        this.consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        this.consolePrinter = new ConsolePrinter(project, consoleView);
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