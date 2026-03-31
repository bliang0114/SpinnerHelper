package cn.github.spinner.task;

import org.jetbrains.annotations.NotNull;

public record MQLCommandEntry(int lineNumber,
                              int sourceStartOffset,
                              int sourceEndOffset,
                              @NotNull String command) {
}
