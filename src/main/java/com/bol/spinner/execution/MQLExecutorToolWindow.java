package com.bol.spinner.execution;

import com.bol.spinner.util.ConsoleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class MQLExecutorToolWindow extends SimpleToolWindowPanel {
    @Getter
    private final ConsoleManager consoleManager;

    public MQLExecutorToolWindow(@NotNull Project project) {
        super(true, true);
        this.consoleManager = ConsoleManager.getInstance(project);
        setContent(consoleManager.getConsoleView().getComponent());
    }
}
