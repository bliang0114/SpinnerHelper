package cn.github.spinner.execution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MQLResultParser {
    private static final Pattern FORM_LINE = Pattern.compile("^\\s*([^=:\\t]+?)\\s*(?:=|:)\\s*(.*)$");
    private static final Pattern DUMP_PATTERN = Pattern.compile("(?i)\\bdump(?:\\s+([^\\s;]+))?");
    private static final Pattern RECORD_SEPARATOR_PATTERN = Pattern.compile("(?i)\\brecordsep\\s+([^\\s;]+)");
    private static final Pattern QUERY_BUS_PATTERN = Pattern.compile("(?i)\\b(?:temp\\s+)?query\\s+bus\\b");
    private static final Pattern EXPAND_PATTERN = Pattern.compile("(?i)^\\s*expand\\s+(?:bus|connection)\\b");

    private MQLResultParser() {
    }

    public static @NotNull MQLResultViewData parse(@Nullable String command, @Nullable String result) {
        if (result == null || result.isBlank()) {
            return MQLResultViewData.defaultView();
        }

        MQLResultViewData table = parseTable(command, result);
        if (table != null) {
            return table;
        }

        MQLResultViewData form = parseForm(result);
        return form != null ? form : MQLResultViewData.defaultView();
    }

    private static @Nullable MQLResultViewData parseTable(@Nullable String command, @NotNull String result) {
        if (command == null) {
            return null;
        }
        Matcher dumpMatcher = DUMP_PATTERN.matcher(command);
        if (!dumpMatcher.find()) {
            return null;
        }

        String fieldSeparator = decodeSeparator(dumpMatcher.group(1), ",");
        Matcher recordSeparatorMatcher = RECORD_SEPARATOR_PATTERN.matcher(command);
        String recordSeparator = recordSeparatorMatcher.find()
                ? decodeSeparator(recordSeparatorMatcher.group(1), "\n")
                : "\n";
        if (fieldSeparator.isEmpty() || recordSeparator.isEmpty()) {
            return null;
        }

        List<List<String>> rows = splitRows(result, recordSeparator, fieldSeparator);
        if (rows.isEmpty()) {
            return null;
        }
        int columnCount = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columnCount < 2) {
            return null;
        }
        rows = padRows(rows, columnCount);

        List<String> columns = resolveColumns(command, dumpMatcher.start(), columnCount);
        MQLResultViewData.Style style = EXPAND_PATTERN.matcher(command).find()
                ? MQLResultViewData.Style.TREE_TABLE
                : MQLResultViewData.Style.TABLE;
        return new MQLResultViewData(style, columns, rows);
    }

    private static @NotNull List<List<String>> padRows(@NotNull List<List<String>> rows, int columnCount) {
        List<List<String>> normalizedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            if (row.size() == columnCount) {
                normalizedRows.add(row);
                continue;
            }
            List<String> paddedRow = new ArrayList<>(columnCount);
            paddedRow.addAll(row);
            while (paddedRow.size() < columnCount) {
                paddedRow.add("");
            }
            normalizedRows.add(paddedRow);
        }
        return normalizedRows;
    }

    private static @NotNull List<List<String>> splitRows(@NotNull String result,
                                                          @NotNull String recordSeparator,
                                                          @NotNull String fieldSeparator) {
        String[] records = result.split(Pattern.quote(recordSeparator), -1);
        List<List<String>> rows = new ArrayList<>();
        for (String record : records) {
            String normalized = record.replace("\r", "");
            if (normalized.isBlank()) {
                continue;
            }
            String[] values = normalized.split(Pattern.quote(fieldSeparator), -1);
            List<String> row = new ArrayList<>(values.length);
            for (String value : values) {
                row.add(value.trim());
            }
            rows.add(row);
        }
        return rows;
    }

    private static @NotNull List<String> resolveColumns(@NotNull String command,
                                                         int dumpStart,
                                                         int columnCount) {
        String beforeDump = command.substring(0, dumpStart);
        int selectIndex = lastWordIndex(beforeDump, "select");
        List<String> selectors = selectIndex >= 0
                ? tokenizeSelectors(beforeDump.substring(selectIndex + "select".length()))
                : List.of();

        if (EXPAND_PATTERN.matcher(beforeDump).find()) {
            return resolveExpandColumns(selectors, columnCount);
        }
        if (selectors.size() == columnCount) {
            return selectors;
        }
        if (QUERY_BUS_PATTERN.matcher(beforeDump).find() && selectors.size() + 3 == columnCount) {
            List<String> columns = new ArrayList<>(columnCount);
            columns.add("type");
            columns.add("name");
            columns.add("revision");
            columns.addAll(selectors);
            return columns;
        }
        return List.of();
    }

    private static @NotNull List<String> resolveExpandColumns(@NotNull List<String> selectors, int columnCount) {
        List<String> columns = new ArrayList<>(columnCount);
        List<String> baseColumns = List.of("Level", "Relationship", "Direction", "Type", "Name", "Revision");
        for (int i = 0; i < Math.min(baseColumns.size(), columnCount); i++) {
            columns.add(baseColumns.get(i));
        }

        List<String> dataSelectors = selectors.stream()
                .filter(selector -> !selector.equalsIgnoreCase("bus")
                        && !selector.equalsIgnoreCase("rel")
                        && !selector.equalsIgnoreCase("relationship"))
                .toList();
        int remainingColumns = columnCount - columns.size();
        if (dataSelectors.size() == remainingColumns) {
            columns.addAll(dataSelectors);
        } else if (dataSelectors.size() == 1) {
            String selector = dataSelectors.getFirst();
            for (int i = 0; i < remainingColumns; i++) {
                columns.add(selector + '[' + (i + 1) + ']');
            }
        }
        return columns.size() == columnCount ? columns : List.of();
    }

    private static int lastWordIndex(@NotNull String text, @NotNull String word) {
        Matcher matcher = Pattern.compile("(?i)\\b" + Pattern.quote(word) + "\\b").matcher(text);
        int index = -1;
        while (matcher.find()) {
            index = matcher.start();
        }
        return index;
    }

    private static @NotNull List<String> tokenizeSelectors(@NotNull String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int bracketDepth = 0;
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (quote != 0) {
                token.append(current);
                if (current == quote && (i == 0 || text.charAt(i - 1) != '\\')) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
                token.append(current);
            } else if (current == '[') {
                bracketDepth++;
                token.append(current);
            } else if (current == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
                token.append(current);
            } else if (Character.isWhitespace(current) && bracketDepth == 0) {
                addToken(tokens, token);
            } else {
                token.append(current);
            }
        }
        addToken(tokens, token);
        return tokens;
    }

    private static void addToken(@NotNull List<String> tokens, @NotNull StringBuilder token) {
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private static @Nullable MQLResultViewData parseForm(@NotNull String result) {
        List<List<String>> rows = new ArrayList<>();
        int contentLineCount = 0;
        for (String line : result.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            contentLineCount++;
            Matcher matcher = FORM_LINE.matcher(line);
            if (matcher.matches()) {
                rows.add(List.of(matcher.group(1).trim(), matcher.group(2).trim()));
            }
        }
        if (rows.size() < 2 || rows.size() < contentLineCount - 1) {
            return null;
        }
        return new MQLResultViewData(MQLResultViewData.Style.FORM, List.of(), rows);
    }

    private static @NotNull String decodeSeparator(@Nullable String token, @NotNull String fallback) {
        if (token == null || token.isBlank() || token.toLowerCase(Locale.ROOT).equals("recordsep")) {
            return fallback;
        }
        String value = stripQuotes(token.trim());
        return switch (value) {
            case "\\t" -> "\t";
            case "\\n" -> "\n";
            case "\\r" -> "\r";
            case "\\001", "\\u0001" -> "\u0001";
            case "\\002", "\\u0002" -> "\u0002";
            default -> value;
        };
    }

    private static @NotNull String stripQuotes(@NotNull String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
