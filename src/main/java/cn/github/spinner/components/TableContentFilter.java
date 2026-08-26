package cn.github.spinner.components;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds case-insensitive, literal-content filters for {@link FilterTable}.
 */
public final class TableContentFilter {
    private static final Pattern COLUMN_CONDITION = Pattern.compile(
            "(?:\\\"([^\\\"]+)\\\"|\\[([^]]+)]|([^\\s:]+))\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(\\S+))"
    );

    private TableContentFilter() {
    }

    public static void apply(TableRowSorter<TableModel> sorter, String filterText) {
        apply(sorter, filterText, Map.of());
    }

    public static void apply(TableRowSorter<TableModel> sorter, String filterText,
                             Map<Integer, Set<String>> columnFilters) {
        String text = filterText == null ? "" : filterText.trim();
        if (text.isEmpty() && columnFilters.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        List<RowFilter<TableModel, Integer>> filters = new ArrayList<>();
        if (!text.isEmpty()) {
            List<ColumnCondition> conditions = parseColumnConditions(sorter.getModel(), text);
            if (conditions == null) {
                filters.add(new ContentRowFilter(null, text));
            } else {
                for (ColumnCondition condition : conditions) {
                    filters.add(new ContentRowFilter(condition.columnIndex(), condition.value()));
                }
            }
        }

        for (Map.Entry<Integer, Set<String>> entry : columnFilters.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < sorter.getModel().getColumnCount()) {
                filters.add(new SelectedValuesRowFilter(entry.getKey(), entry.getValue()));
            }
        }
        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private static List<ColumnCondition> parseColumnConditions(TableModel model, String text) {
        Matcher matcher = COLUMN_CONDITION.matcher(text);
        List<ColumnCondition> conditions = new ArrayList<>();
        int end = 0;
        while (matcher.find()) {
            if (!text.substring(end, matcher.start()).isBlank()) {
                return null;
            }

            String column = firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3));
            String value = firstNonNull(matcher.group(4), matcher.group(5));
            int columnIndex = findColumn(model, column.trim());
            if (columnIndex < 0 || value.isEmpty()) {
                return null;
            }
            conditions.add(new ColumnCondition(columnIndex, value));
            end = matcher.end();
        }

        if (conditions.isEmpty() || !text.substring(end).isBlank()) {
            return null;
        }
        return conditions;
    }

    private static int findColumn(TableModel model, String column) {
        try {
            int index = Integer.parseInt(column);
            return index >= 0 && index < model.getColumnCount() ? index : -1;
        } catch (NumberFormatException ignored) {
            for (int i = 0; i < model.getColumnCount(); i++) {
                if (column.equalsIgnoreCase(model.getColumnName(i).trim())) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    private record ColumnCondition(int columnIndex, String value) {
    }

    private static final class ContentRowFilter extends RowFilter<TableModel, Integer> {
        private final Integer columnIndex;
        private final String value;

        private ContentRowFilter(Integer columnIndex, String value) {
            this.columnIndex = columnIndex;
            this.value = value.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
            if (columnIndex != null) {
                return contains(entry.getStringValue(columnIndex));
            }
            for (int i = 0; i < entry.getValueCount(); i++) {
                if (contains(entry.getStringValue(i))) {
                    return true;
                }
            }
            return false;
        }

        private boolean contains(String cellValue) {
            return cellValue != null && cellValue.toLowerCase(Locale.ROOT).contains(value);
        }
    }

    private static final class SelectedValuesRowFilter extends RowFilter<TableModel, Integer> {
        private final int columnIndex;
        private final Set<String> selectedValues;

        private SelectedValuesRowFilter(int columnIndex, Set<String> selectedValues) {
            this.columnIndex = columnIndex;
            this.selectedValues = Set.copyOf(selectedValues);
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
            return selectedValues.contains(entry.getStringValue(columnIndex));
        }
    }
}
