package cn.github.spinner.execution;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MQLResultViewData(@NotNull Style style,
                                @NotNull List<String> columns,
                                @NotNull List<List<String>> rows) {
    public MQLResultViewData {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public static @NotNull MQLResultViewData defaultView() {
        return new MQLResultViewData(Style.DEFAULT, List.of(), List.of());
    }

    public enum Style {
        DEFAULT,
        FORM,
        TABLE,
        TREE_TABLE
    }
}
