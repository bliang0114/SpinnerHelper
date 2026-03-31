package cn.github.spinner.task;

import org.jetbrains.annotations.NotNull;

public record MQLCommandEntry(int lineNumber, @NotNull String command) {
}
