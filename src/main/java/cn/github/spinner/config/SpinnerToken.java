package cn.github.spinner.config;

import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SpinnerToken {
    public static final Map<String, ObjectWhereExpression> OBJECT_WHERE_EXPRESSION_MAP = new ConcurrentHashMap<>();

    public static ObjectWhereExpression getObjectWhereExpression(@NotNull Project project) {
        return OBJECT_WHERE_EXPRESSION_MAP.get(project.getLocationHash());
    }

    public static void putObjectWhereExpression(@NotNull Project project, @NotNull ObjectWhereExpression objectWhereExpression) {
        OBJECT_WHERE_EXPRESSION_MAP.put(project.getLocationHash(), objectWhereExpression);
    }
}
