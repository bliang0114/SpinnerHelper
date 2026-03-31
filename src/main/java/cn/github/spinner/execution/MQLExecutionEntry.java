package cn.github.spinner.execution;

import org.jetbrains.annotations.NotNull;

public record MQLExecutionEntry(int lineNumber,
                                int sourceStartOffset,
                                int sourceEndOffset,
                                int consoleStartOffset,
                                @NotNull String command,
                                boolean success,
                                @NotNull String message) {
}
