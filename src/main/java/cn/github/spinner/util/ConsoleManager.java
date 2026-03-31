package cn.github.spinner.util;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import cn.github.spinner.execution.MQLExecutionEntry;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class ConsoleManager {
    private final ConsoleView consoleView;
    private final ConsolePrinter consolePrinter;
    private final List<MQLExecutionEntry> executionEntries = new CopyOnWriteArrayList<>();
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

    public void addExecutionEntry(MQLExecutionEntry entry) {
        executionEntries.add(entry);
    }

    public void clearExecutionEntries() {
        executionEntries.clear();
    }

    public int getCurrentOutputOffset() {
        return consolePrinter.getCurrentOffset();
    }

    public int printSync(String message) {
        return consolePrinter.printSync(message);
    }

    public int printSync(String message, ConsoleViewContentType contentType) {
        return consolePrinter.printSync(message, contentType);
    }

    public void scrollToExecutionEntry(@NotNull MQLExecutionEntry entry) {
        consolePrinter.scrollToOffset(entry.consoleStartOffset());
    }

    public MQLExecutionEntry findExecutionEntry(int sourceOffset, int lineNumber) {
        MQLExecutionEntry nextEntryOnSameLine = null;
        MQLExecutionEntry previousEntryOnSameLine = null;
        for (MQLExecutionEntry entry : executionEntries) {
            if (sourceOffset >= entry.sourceStartOffset() && sourceOffset < entry.sourceEndOffset()) {
                return entry;
            }
            if (entry.lineNumber() == lineNumber) {
                if (sourceOffset < entry.sourceStartOffset()) {
                    if (nextEntryOnSameLine == null || entry.sourceStartOffset() < nextEntryOnSameLine.sourceStartOffset()) {
                        nextEntryOnSameLine = entry;
                    }
                } else if (sourceOffset >= entry.sourceEndOffset()) {
                    if (previousEntryOnSameLine == null || entry.sourceStartOffset() > previousEntryOnSameLine.sourceStartOffset()) {
                        previousEntryOnSameLine = entry;
                    }
                }
            }
        }
        return nextEntryOnSameLine != null ? nextEntryOnSameLine : previousEntryOnSameLine;
    }

    public boolean isSoftWrapsEnabled() {
        return consolePrinter.isSoftWrapsEnabled();
    }

    public void setSoftWrapsEnabled(boolean enabled) {
        consolePrinter.setSoftWrapsEnabled(enabled);
    }

    public JComponent getResultComponent() {
        return consolePrinter.getResultComponent();
    }
}
