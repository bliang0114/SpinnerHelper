package cn.github.spinner.task;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将编辑器中的 MQL 源码解析成真正需要发送给 Matrix 服务端的命令。
 * 注释会在分隔命令之前被遮罩，因此注释内容及其中的分隔符都不会参与执行。
 */
public final class MQLCommandParser {
    private MQLCommandParser() {
    }

    /**
     * 解析指定源码范围，同时使用完整源码判断范围开始位置是否位于块注释中。
     *
     * @param source        完整编辑器源码
     * @param rangeStart    执行范围起始偏移（包含）
     * @param rangeEnd      执行范围结束偏移（不包含）
     * @param lineDelimiter MQL 命令分隔符正则表达式
     * @return 保留原始行号和源码偏移的可执行命令
     */
    public static @NotNull List<MQLCommandEntry> parse(@NotNull CharSequence source,
                                                       int rangeStart,
                                                       int rangeEnd,
                                                       @NotNull String lineDelimiter) {
        if (rangeStart < 0 || rangeEnd < rangeStart || rangeEnd > source.length()) {
            throw new IllegalArgumentException("Invalid MQL source range");
        }

        // 必须先遮罩完整源码再截取范围，否则选择可能从多行块注释的中间开始。
        String executableText = maskComments(source).substring(rangeStart, rangeEnd);
        List<MQLCommandEntry> entries = new ArrayList<>();
        Matcher matcher = Pattern.compile(lineDelimiter).matcher(executableText);
        int segmentStart = 0;
        while (matcher.find()) {
            addEntry(entries, source, rangeStart, executableText, segmentStart, matcher.start());
            segmentStart = matcher.end();
        }
        addEntry(entries, source, rangeStart, executableText, segmentStart, executableText.length());
        return entries;
    }

    private static void addEntry(@NotNull List<MQLCommandEntry> entries,
                                 @NotNull CharSequence source,
                                 int baseOffset,
                                 @NotNull String executableText,
                                 int segmentStart,
                                 int segmentEnd) {
        String command = executableText.substring(segmentStart, segmentEnd);
        int leadingWhitespace = countLeadingWhitespace(command);
        int trailingWhitespace = countTrailingWhitespace(command);
        if (leadingWhitespace == command.length()) {
            return;
        }

        int sourceStartOffset = baseOffset + segmentStart + leadingWhitespace;
        int sourceEndOffset = baseOffset + segmentEnd - trailingWhitespace;
        String normalized = command.substring(leadingWhitespace, command.length() - trailingWhitespace)
                .replace('\n', ' ')
                .replace('\r', ' ');
        entries.add(new MQLCommandEntry(
                lineNumber(source, sourceStartOffset),
                sourceStartOffset,
                sourceEndOffset,
                normalized
        ));
    }

    private static int countLeadingWhitespace(@NotNull String text) {
        int count = 0;
        while (count < text.length() && Character.isWhitespace(text.charAt(count))) {
            count++;
        }
        return count;
    }

    private static int countTrailingWhitespace(@NotNull String text) {
        int count = 0;
        while (count < text.length() && Character.isWhitespace(text.charAt(text.length() - 1 - count))) {
            count++;
        }
        return count;
    }

    private static int lineNumber(@NotNull CharSequence source, int offset) {
        int lineNumber = 0;
        for (int index = 0; index < offset; index++) {
            if (source.charAt(index) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    /**
     * 使用空格替换 #、// 和块注释字符，同时保留换行及字符串中的注释符号。
     * 返回文本与原文严格等长，确保执行结果仍能映射到正确的源码位置。
     */
    private static @NotNull String maskComments(@NotNull CharSequence source) {
        char[] masked = source.toString().toCharArray();
        ScanState state = ScanState.CODE;
        for (int index = 0; index < masked.length; index++) {
            char current = masked[index];
            char next = index + 1 < masked.length ? masked[index + 1] : '\0';
            switch (state) {
                case CODE -> {
                    if (current == '"') {
                        state = ScanState.DOUBLE_QUOTED;
                    } else if (current == '\'') {
                        state = ScanState.SINGLE_QUOTED;
                    } else if (current == '#') {
                        masked[index] = ' ';
                        state = ScanState.LINE_COMMENT;
                    } else if (current == '/' && next == '/') {
                        masked[index] = ' ';
                        masked[++index] = ' ';
                        state = ScanState.LINE_COMMENT;
                    } else if (current == '/' && next == '*') {
                        masked[index] = ' ';
                        masked[++index] = ' ';
                        state = ScanState.BLOCK_COMMENT;
                    }
                }
                case DOUBLE_QUOTED -> {
                    if (current == '\\') {
                        index++;
                    } else if (current == '"') {
                        state = ScanState.CODE;
                    }
                }
                case SINGLE_QUOTED -> {
                    if (current == '\\') {
                        index++;
                    } else if (current == '\'') {
                        state = ScanState.CODE;
                    }
                }
                case LINE_COMMENT -> {
                    if (current == '\n' || current == '\r') {
                        state = ScanState.CODE;
                    } else {
                        masked[index] = ' ';
                    }
                }
                case BLOCK_COMMENT -> {
                    if (current == '*' && next == '/') {
                        masked[index] = ' ';
                        masked[++index] = ' ';
                        state = ScanState.CODE;
                    } else if (current != '\n' && current != '\r') {
                        masked[index] = ' ';
                    }
                }
            }
        }
        return new String(masked);
    }

    private enum ScanState {
        CODE,
        DOUBLE_QUOTED,
        SINGLE_QUOTED,
        LINE_COMMENT,
        BLOCK_COMMENT
    }
}
