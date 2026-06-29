package cn.github.spinner.util;

import cn.github.spinner.config.SpinnerSettings;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import cn.github.spinner.execution.MQLExecutionEntry;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class ConsoleManager implements Disposable {
    private static final int MAX_EXECUTION_ENTRIES = 5_000;
    private static final int EXECUTION_ENTRY_TRIM_COUNT = 500;
    private final ConsoleView consoleView;
    private final ConsolePrinter consolePrinter;
    private final List<MQLExecutionEntry> executionEntries = new CopyOnWriteArrayList<>();
    private String consoleName;
    private VirtualFile consoleFile;
    private volatile boolean disposed;

    public ConsoleManager(Project project, String consoleName, VirtualFile consoleFile) {
        this.consoleView = new ConsoleViewImpl(
                project,
                GlobalSearchScope.allScope(project),
                true,  // viewer mode
                false   // ← 关键：false = 禁用循环缓冲区
        );
        this.consolePrinter = new ConsolePrinter(project, consoleView,
                () -> SpinnerSettings.getInstance(project).getMqlResultMaxSizeMb(),
                this::trimExecutionEntries);
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
        clearExecutionEntries();
        consolePrinter.clear();
    }

    public synchronized void addExecutionEntry(MQLExecutionEntry entry) {
        if (executionEntries.size() >= MAX_EXECUTION_ENTRIES) {
            executionEntries.subList(0, EXECUTION_ENTRY_TRIM_COUNT).clear();
        }
        executionEntries.add(entry);
    }

    public void clearExecutionEntries() {
        executionEntries.clear();
    }
    private synchronized void trimExecutionEntries(int removedChars) {
        if (removedChars <= 0 || executionEntries.isEmpty()) {
            return;
        }
        List<MQLExecutionEntry> adjustedEntries = new ArrayList<>(executionEntries.size());
        for (MQLExecutionEntry entry : executionEntries) {
            int adjustedOffset = entry.consoleStartOffset() - removedChars;
            if (adjustedOffset < 0) {
                continue;
            }
            adjustedEntries.add(new MQLExecutionEntry(
                    entry.lineNumber(),
                    entry.sourceStartOffset(),
                    entry.sourceEndOffset(),
                    adjustedOffset,
                    entry.command(),
                    entry.success(),
                    entry.message()
            ));
        }
        executionEntries.clear();
        executionEntries.addAll(adjustedEntries);
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

    public JComponent createResultComponent() {
        return consolePrinter.createResultComponent();
    }

    public void releaseResultComponent(@Nullable JComponent component) {
        consolePrinter.releaseResultComponent(component);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        clearExecutionEntries();
        consolePrinter.dispose();
        if (consoleView instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }
    }
}
