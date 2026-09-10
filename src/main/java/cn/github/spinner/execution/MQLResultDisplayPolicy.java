package cn.github.spinner.execution;

public final class MQLResultDisplayPolicy {
    private MQLResultDisplayPolicy() {
    }

    public static boolean supportsStructuredView(int commandCount) {
        return commandCount == 1;
    }
}
