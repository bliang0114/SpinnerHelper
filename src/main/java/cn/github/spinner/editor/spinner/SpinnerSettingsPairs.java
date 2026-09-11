package cn.github.spinner.editor.spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Raw positional Settings tokens. Null represents a missing trailing cell, not an empty value. */
final class SpinnerSettingsPairs {
    private static final Pattern SEPARATOR = Pattern.compile("(?<!\\|)\\|(?!\\|)");
    private final List<String> names;
    private final List<String> values;
    // Token endings before append adds separator padding; null retains a missing trailing slot.
    private final List<String> nameEndings = new ArrayList<>();
    private final List<String> valueEndings = new ArrayList<>();

    SpinnerSettingsPairs(String nameText, String valueText) {
        names = new ArrayList<>();
        values = new ArrayList<>();
        if (nameText.isEmpty() && valueText.isEmpty()) return;
        names.addAll(Arrays.asList(SEPARATOR.split(nameText, -1)));
        values.addAll(Arrays.asList(SEPARATOR.split(valueText, -1)));
        int size = Math.max(names.size(), values.size());
        while (names.size() < size) names.add(null);
        while (values.size() < size) values.add(null);
        names.forEach(value -> nameEndings.add(value == null ? null : trailing(value)));
        values.forEach(value -> valueEndings.add(value == null ? null : trailing(value)));
    }

    SpinnerSettingsPairs(SpinnerSettingsPairs original) {
        names = new ArrayList<>(original.names);
        values = new ArrayList<>(original.values);
        nameEndings.addAll(original.nameEndings);
        valueEndings.addAll(original.valueEndings);
    }

    int size() { return names.size(); }
    String get(int row, int column) {
        String value = (column == 0 ? names : values).get(row);
        return value == null ? "" : value;
    }
    String names() { return join(names); }
    String values() { return join(values); }

    void set(int row, int column, String value) {
        validate(value);
        List<String> target = column == 0 ? names : values;
        if (get(row, column).equals(value) || get(row, column).strip().equals(value)) return;
        target.set(row, SpinnerSettingsComponent.preserveSpacing(target.get(row), value));
        if (!trailing(value).isEmpty()) (column == 0 ? nameEndings : valueEndings).set(row, trailing(value));
        keepSeparatorsDistinct(target);
    }

    void add(String name, String value) {
        validate(name);
        validate(value);
        append(names, name.strip());
        append(values, value.strip());
        nameEndings.add(trailing(names.getLast()));
        valueEndings.add(trailing(values.getLast()));
    }

    void remove(int[] rows) {
        Arrays.stream(rows).distinct().boxed().sorted(java.util.Comparator.reverseOrder()).forEach(row -> {
            names.remove((int) row);
            values.remove((int) row);
            nameEndings.remove((int) row);
            valueEndings.remove((int) row);
        });
        restoreEnding(names, nameEndings);
        restoreEnding(values, valueEndings);
    }

    private static void restoreEnding(List<String> tokens, List<String> endings) {
        for (int row = tokens.size() - 1; row >= 0; row--) {
            String value = tokens.get(row);
            String content = value == null ? "" : value.substring(0, value.length() - trailing(value).length());
            String ending = endings.get(row);
            if (ending == null && content.isEmpty()) {
                tokens.set(row, null);
                continue;
            }
            tokens.set(row, content + (ending == null ? "" : ending));
            break;
        }
    }

    /** Validate the entire operation before changing even a copy of the target. */
    SpinnerSettingsPairs merged(List<List<String>> source) {
        java.util.Map<String, Integer> incoming = new java.util.LinkedHashMap<>();
        for (int row = 0; row < source.size(); row++) {
            List<String> pair = source.get(row);
            if (pair.size() != 2) throw new MergeConflict("spinner.settings.merge.columns", row + 1);
            try { validate(pair.get(0)); validate(pair.get(1)); }
            catch (IllegalArgumentException ex) { throw new MergeConflict("spinner.settings.merge.invalid", row + 1); }
            String key = pair.get(0).strip();
            if (key.isEmpty()) throw new MergeConflict("spinner.settings.merge.empty.name", row + 1);
            Integer first = incoming.putIfAbsent(key, row);
            if (first != null) throw new MergeConflict("spinner.settings.merge.source.duplicate", key, first + 1, row + 1);
        }
        java.util.Map<String, List<Integer>> positions = new java.util.HashMap<>();
        for (int row = 0; row < size(); row++) positions.computeIfAbsent(get(row, 0).strip(), key -> new ArrayList<>()).add(row);
        for (String key : incoming.keySet()) {
            List<Integer> matches = positions.get(key);
            if (matches != null && matches.size() > 1) throw new MergeConflict("spinner.settings.merge.target.duplicate", key,
                    String.join(", ", matches.stream().map(row -> String.valueOf(row + 1)).toList()));
        }
        SpinnerSettingsPairs result = new SpinnerSettingsPairs(this);
        incoming.forEach((key, row) -> {
            String value = source.get(row).get(1); // Values are never trimmed for matching or merging.
            List<Integer> matches = positions.get(key);
            if (matches != null) result.set(matches.getFirst(), 1, value);
            else {
                // Use the target's original local style, not whitespace carried by pasted values.
                append(result.names, key, names);
                append(result.values, value, values);
                result.nameEndings.add(trailing(result.names.getLast()));
                result.valueEndings.add(trailing(result.values.getLast()));
            }
        });
        return result;
    }

    static final class MergeConflict extends IllegalArgumentException {
        final String key;
        final Object[] arguments;
        MergeConflict(String key, Object... arguments) {
            super(key);
            this.key = key;
            this.arguments = arguments;
        }
    }

    private static void validate(String value) {
        if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || SEPARATOR.matcher(value).find())
            throw new IllegalArgumentException("spinner.settings.invalid");
    }

    private static void append(List<String> tokens, String value) {
        append(tokens, value, tokens);
    }

    private static void append(List<String> tokens, String value, List<String> reference) {
        if (tokens.isEmpty()) { tokens.add(value); return; }
        String left = " ", right = " ";
        if (reference.size() > 1) {
            left = trailing(reference.get(reference.size() - 2));
            right = leading(reference.getLast());
        }
        String last = tokens.getLast() == null ? "" : tokens.getLast();
        if (trailing(last).isEmpty()) last += left;
        // Avoid manufacturing a || operator when appending after a truly empty token.
        if (last.isEmpty()) last = " ";
        tokens.set(tokens.size() - 1, last);
        tokens.add(right + value);
        keepSeparatorsDistinct(tokens);
    }

    private static void keepSeparatorsDistinct(List<String> tokens) {
        int end = tokens.size();
        while (end > 0 && tokens.get(end - 1) == null) end--;
        for (int row = 0; row < end; row++) {
            String token = tokens.get(row) == null ? "" : tokens.get(row);
            // An empty middle slot or a pipe at a token edge must not swallow a separator into ||.
            if (row > 0 && (token.startsWith("|") || token.isEmpty() && row < end - 1)) token = " " + token;
            if (row < end - 1 && token.endsWith("|")) token += " ";
            if (!token.isEmpty() || tokens.get(row) != null) tokens.set(row, token);
        }
    }

    private static String leading(String text) {
        if (text == null) return "";
        int end = 0;
        while (end < text.length() && text.charAt(end) == ' ') end++;
        return text.substring(0, end);
    }

    private static String trailing(String text) {
        if (text == null) return "";
        int start = text.length();
        while (start > 0 && text.charAt(start - 1) == ' ') start--;
        return text.substring(start);
    }

    private static String join(List<String> tokens) {
        int end = tokens.size();
        while (end > 0 && tokens.get(end - 1) == null) end--;
        return String.join("|", tokens.subList(0, end).stream().map(value -> value == null ? "" : value).toList());
    }
}
