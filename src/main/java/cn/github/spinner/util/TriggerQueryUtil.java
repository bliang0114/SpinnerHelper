package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TriggerQueryUtil {
    private static final String TRIGGER_PARAMETER_TYPE = "eService Trigger Program Parameters";
    private static final Pattern PROGRAM_TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<String> SKIPPED_CLASS_SEARCH_DIRS = Set.of(".git", ".gradle", ".idea", "node_modules");

    private TriggerQueryUtil() {
    }

    public static @NotNull List<TriggerQueryResult> query(@NotNull Project project,
                                                          @NotNull String name,
                                                          @Nullable String stateFilter) throws MQLException {
        return query(project, SchemaType.ALL, name, stateFilter);
    }

    public static @NotNull List<TriggerQueryResult> query(@NotNull Project project,
                                                          @NotNull SchemaType schemaType,
                                                          @NotNull String name,
                                                          @Nullable String stateFilter) throws MQLException {
        List<TriggerQueryResult> results = new ArrayList<>();
        if (schemaType.includes(SchemaType.POLICY) && isPolicy(project, name)) {
            results.addAll(queryPolicyTrigger(project, name, stateFilter));
        }
        if (schemaType.includes(SchemaType.ATTRIBUTE) && isAttribute(project, name)) {
            results.addAll(queryAdminTrigger(project, "attribute", name, null, "print attribute " + quote(name) + " select trigger dump"));
        }
        if (schemaType.includes(SchemaType.RELATIONSHIP) && isRelationship(project, name)) {
            results.addAll(queryAdminTrigger(project, "relationship", name, null, "print relationship " + quote(name) + " select trigger dump"));
        }
        if (schemaType.includes(SchemaType.TYPE) && isType(project, name)) {
            results.addAll(queryTypeTrigger(project, name, stateFilter));
        }
        return results.stream().distinct().sorted(Comparator
                .comparing(TriggerQueryResult::schemaType, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TriggerQueryResult::schemaName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TriggerQueryResult::state, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(TriggerQueryResult::sequence)).toList();
    }

    public static @NotNull List<AdminObjectCandidate> listAdminObjects(@NotNull Project project,
                                                                       @NotNull SchemaType schemaType) throws MQLException {
        List<AdminObjectCandidate> results = new ArrayList<>();
        for (SchemaType targetType : schemaType.targetTypes()) {
            String listResult = MQLUtil.execute(project, "list " + targetType.mqlName());
            for (String name : parseListResult(listResult)) {
                results.add(new AdminObjectCandidate(name, targetType));
            }
        }
        return results.stream().distinct().sorted(Comparator
                .comparing(AdminObjectCandidate::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.schemaType().mqlName())).toList();
    }

    private static @NotNull List<TriggerQueryResult> queryTypeTrigger(@NotNull Project project,
                                                                      @NotNull String typeName,
                                                                      @Nullable String stateFilter) throws MQLException {
        List<TriggerQueryResult> results = new ArrayList<>();
        String triggerResult = MQLUtil.execute(project, "print type " + quote(typeName) + " select trigger dump");
        results.addAll(queryTriggerPrograms(project, "type", typeName, null, triggerResult));

        for (String policyName : queryTypePolicies(project, typeName)) {
            results.addAll(queryPolicyTrigger(project, policyName, stateFilter));
        }
        return results;
    }

    private static @NotNull List<String> queryTypePolicies(@NotNull Project project,
                                                           @NotNull String typeName) throws MQLException {
        String policyResult = MQLUtil.execute(project, "print type " + quote(typeName) + " select policy dump");
        return parseDumpValues(policyResult);
    }

    private static @NotNull List<TriggerQueryResult> queryPolicyTrigger(@NotNull Project project,
                                                                        @NotNull String policyName,
                                                                        @Nullable String stateFilter) throws MQLException {
        String stateResult = MQLUtil.execute(project, "print policy " + quote(policyName) + " select state dump");
        if (CharSequenceUtil.isBlank(stateResult)) {
            return List.of();
        }

        List<String> targetStates = parseStateFilter(stateFilter);
        List<TriggerQueryResult> results = new ArrayList<>();
        for (String state : stateResult.split(",")) {
            String normalizedState = state.trim();
            if (normalizedState.isEmpty() || (!targetStates.isEmpty() && !targetStates.contains(normalizedState))) {
                continue;
            }
            String triggerResult = MQLUtil.execute(project,
                    "print policy " + quote(policyName) + " select state[" + normalizedState + "].trigger dump");
            results.addAll(queryTriggerPrograms(project, "policy", policyName, normalizedState, triggerResult));
        }
        return results;
    }

    private static @NotNull List<TriggerQueryResult> queryAdminTrigger(@NotNull Project project,
                                                                       @NotNull String schemaType,
                                                                       @NotNull String schemaName,
                                                                       @Nullable String state,
                                                                       @NotNull String mql) throws MQLException {
        String triggerResult = MQLUtil.execute(project, mql);
        return queryTriggerPrograms(project, schemaType, schemaName, state, triggerResult);
    }

    private static @NotNull List<TriggerQueryResult> queryTriggerPrograms(@NotNull Project project,
                                                                          @NotNull String schemaType,
                                                                          @NotNull String schemaName,
                                                                          @Nullable String state,
                                                                          @Nullable String triggerResult) throws MQLException {
        if (CharSequenceUtil.isBlank(triggerResult)) {
            return List.of();
        }

        List<TriggerQueryResult> results = new ArrayList<>();
        for (TriggerSpec triggerSpec : parseTriggerSpecs(triggerResult)) {
            String triggerProgramResult;
            try {
                triggerProgramResult = MQLUtil.execute(project,
                        "temp query bus " + quote(TRIGGER_PARAMETER_TYPE) + " " + quote(triggerSpec.triggerName()) +
                                " * select attribute[*Name] current attribute[eService Sequence Number] dump |");
            } catch (MQLException ignored) {
                continue;
            }
            if (CharSequenceUtil.isBlank(triggerProgramResult)) {
                continue;
            }
            for (String line : triggerProgramResult.split("\\R")) {
                String[] values = line.split("\\|", -1);
                if (values.length < 4) {
                    continue;
                }
                String program = values[values.length - 4].trim();
                String method = values[values.length - 3].trim();
                String current = values[values.length - 2].trim();
                String sequence = values[values.length - 1].trim();
                if (!"Active".equals(current) || CharSequenceUtil.isBlank(program)) {
                    continue;
                }

                SourceLocation sourceLocation = findSourceLocation(project, program + "_mxJPO", method);
                results.add(new TriggerQueryResult(
                        schemaType,
                        schemaName,
                        state == null ? "" : state,
                        triggerSpec.eventType(),
                        parseSequence(sequence),
                        triggerSpec.triggerName(),
                        program + "_mxJPO",
                        method,
                        sourceLocation.path(),
                        sourceLocation.line()
                ));
            }
        }
        return results;
    }

    private static @NotNull List<TriggerSpec> parseTriggerSpecs(@NotNull String triggerResult) {
        List<TriggerSpec> specs = new ArrayList<>();
        for (String trigger : triggerResult.split(",")) {
            String triggerText = trigger.trim();
            int colonIndex = triggerText.indexOf(':');
            int openIndex = triggerText.indexOf('(');
            int closeIndex = triggerText.indexOf(')', openIndex + 1);
            if (colonIndex <= 0 || openIndex < 0 || closeIndex <= openIndex) {
                continue;
            }
            String eventType = triggerText.substring(0, colonIndex).trim();
            String triggerNames = triggerText.substring(openIndex + 1, closeIndex);
            for (String triggerName : triggerNames.split("\\s+")) {
                String normalizedName = triggerName.trim();
                if (normalizedName.isEmpty() || (normalizedName.contains("<<") && normalizedName.contains(">>"))) {
                    continue;
                }
                specs.add(new TriggerSpec(eventType, normalizedName));
            }
        }
        return specs;
    }

    private static @NotNull SourceLocation findSourceLocation(@NotNull Project project,
                                                              @NotNull String className,
                                                              @NotNull String methodName) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return SourceLocation.EMPTY;
        }

        Path projectPath = Path.of(basePath);
        Path sourceRoot = projectPath.resolve("spinner").resolve("Business").resolve("SourceFiles");
        Path sourceFile = findJavaSourceFile(sourceRoot, className);
        if (sourceFile == null) {
            sourceFile = findJavaSourceFile(projectPath, className);
        }
        if (sourceFile == null) {
            return SourceLocation.EMPTY;
        }
        try {
            return new SourceLocation(sourceFile.toString(), findMethodLine(Files.readAllLines(sourceFile), methodName));
        } catch (IOException ignored) {
            return new SourceLocation(sourceFile.toString(), -1);
        }
    }

    private static @Nullable Path findJavaSourceFile(@NotNull Path root, @NotNull String className) {
        if (!Files.isDirectory(root)) {
            return null;
        }
        String fileName = className + ".java";
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> fileName.equals(path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static @NotNull String queryProgramCode(@NotNull Project project, @NotNull String jpoClassName) throws MQLException {
        String programName = toProgramName(jpoClassName);
        String sourceCode = MQLUtil.execute(project, "print prog " + quote(programName) + " select code dump");
        return restoreProgramTemplateValues(sourceCode, programName);
    }

    public static @NotNull String toProgramName(@NotNull String jpoClassName) {
        return jpoClassName.endsWith("_mxJPO")
                ? jpoClassName.substring(0, jpoClassName.length() - "_mxJPO".length())
                : jpoClassName;
    }

    public static int findMethodLine(@NotNull String sourceCode, @NotNull String methodName) {
        return findMethodLine(sourceCode.lines().toList(), methodName);
    }

    public static @NotNull ClassLookupResult findClassTarget(@NotNull Project project,
                                                             @NotNull String jpoClassName,
                                                             @NotNull String methodName) {
        SmartPsiElementPointer<PsiElement> pointer = findIndexedClassTarget(project, jpoClassName, methodName);
        if (pointer != null) {
            return new ClassLookupResult(pointer, "");
        }

        Path classFile = findClassFile(project, jpoClassName);
        return new ClassLookupResult(null, classFile == null ? "" : classFile.toString());
    }

    @SuppressWarnings("deprecation")
    private static @Nullable SmartPsiElementPointer<PsiElement> findIndexedClassTarget(@NotNull Project project,
                                                                                       @NotNull String jpoClassName,
                                                                                       @NotNull String methodName) {
        if (project.isDisposed()) {
            return null;
        }
        return DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            PsiClass psiClass = findPsiClass(project, jpoClassName);
            if (psiClass == null) {
                return null;
            }
            PsiElement target = psiClass;
            if (CharSequenceUtil.isNotBlank(methodName)) {
                PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
                if (methods.length > 0) {
                    target = methods[0];
                }
            }
            return SmartPointerManager.getInstance(project).createSmartPsiElementPointer(target);
        });
    }

    private static @Nullable PsiClass findPsiClass(@NotNull Project project, @NotNull String jpoClassName) {
        String shortName = toShortClassName(jpoClassName);
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        for (PsiClass psiClass : PsiShortNamesCache.getInstance(project).getClassesByName(shortName, scope)) {
            String qualifiedName = psiClass.getQualifiedName();
            if (jpoClassName.equals(qualifiedName) || shortName.equals(psiClass.getName())) {
                return psiClass;
            }
        }
        return null;
    }

    private static @Nullable Path findClassFile(@NotNull Project project, @NotNull String jpoClassName) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return null;
        }
        return findFileByName(Path.of(basePath), toShortClassName(jpoClassName) + ".class");
    }

    private static @Nullable Path findFileByName(@NotNull Path root, @NotNull String fileName) {
        if (!Files.isDirectory(root)) {
            return null;
        }
        AtomicReference<Path> result = new AtomicReference<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir,
                                                                   @NotNull BasicFileAttributes attrs) {
                    Path directoryName = dir.getFileName();
                    if (!root.equals(dir) && directoryName != null &&
                            SKIPPED_CLASS_SEARCH_DIRS.contains(directoryName.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file,
                                                          @NotNull BasicFileAttributes attrs) {
                    if (fileName.equals(file.getFileName().toString())) {
                        result.set(file);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            return null;
        }
        return result.get();
    }

    private static @NotNull String toShortClassName(@NotNull String className) {
        int packageIndex = className.lastIndexOf('.');
        return packageIndex >= 0 ? className.substring(packageIndex + 1) : className;
    }

    private static @NotNull String restoreProgramTemplateValues(@NotNull String sourceCode, @NotNull String programName) {
        Matcher matcher = PROGRAM_TEMPLATE_PATTERN.matcher(sourceCode);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String templateName = matcher.group(1).trim();
            String replacement = "CLASSNAME".equalsIgnoreCase(templateName) ? programName : templateName;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static int findMethodLine(@NotNull List<String> lines, @NotNull String methodName) {
        Pattern pattern = Pattern.compile(".*\\b" + Pattern.quote(methodName) + "\\s*\\([^)]*\\).*");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.startsWith("//") && pattern.matcher(line).matches()) {
                return i + 1;
            }
        }
        return -1;
    }

    private static boolean isAttribute(@NotNull Project project, @NotNull String name) {
        return exists(project, "print attribute " + quote(name) + " dump");
    }

    private static boolean isPolicy(@NotNull Project project, @NotNull String name) {
        return exists(project, "print policy " + quote(name) + " dump");
    }

    private static boolean isRelationship(@NotNull Project project, @NotNull String name) {
        return exists(project, "print relationship " + quote(name) + " dump");
    }

    private static boolean isType(@NotNull Project project, @NotNull String name) {
        return exists(project, "print type " + quote(name) + " dump");
    }

    private static boolean exists(@NotNull Project project, @NotNull String mql) {
        try {
            return CharSequenceUtil.isNotBlank(MQLUtil.execute(project, mql));
        } catch (MQLException ignored) {
            return false;
        }
    }

    private static @NotNull List<String> parseStateFilter(@Nullable String stateFilter) {
        if (CharSequenceUtil.isBlank(stateFilter)) {
            return List.of();
        }
        return Stream.of(stateFilter.split(","))
                .map(String::trim)
                .filter(state -> !state.isEmpty())
                .toList();
    }

    private static @NotNull List<String> parseListResult(@Nullable String listResult) {
        if (CharSequenceUtil.isBlank(listResult)) {
            return List.of();
        }
        return listResult.lines()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }

    private static @NotNull List<String> parseDumpValues(@Nullable String dumpResult) {
        if (CharSequenceUtil.isBlank(dumpResult)) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String line : dumpResult.split("\\R")) {
            for (String value : line.split(",")) {
                String normalizedValue = value.trim();
                if (!normalizedValue.isEmpty()) {
                    values.add(normalizedValue);
                }
            }
        }
        return List.copyOf(values);
    }

    private static int parseSequence(@NotNull String sequence) {
        try {
            return Integer.parseInt(sequence);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static @NotNull String quote(@NotNull String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record TriggerSpec(@NotNull String eventType, @NotNull String triggerName) {
    }

    private record SourceLocation(@NotNull String path, int line) {
        private static final SourceLocation EMPTY = new SourceLocation("", -1);
    }

    public enum SchemaType {
        ALL(""),
        TYPE("type"),
        POLICY("policy"),
        RELATIONSHIP("relationship"),
        ATTRIBUTE("attribute");

        private static final List<SchemaType> QUERY_TYPES = List.of(TYPE, POLICY, RELATIONSHIP, ATTRIBUTE);
        private final String mqlName;

        SchemaType(@NotNull String mqlName) {
            this.mqlName = mqlName;
        }

        public @NotNull String mqlName() {
            return mqlName;
        }

        public boolean includes(@NotNull SchemaType schemaType) {
            return this == ALL || this == schemaType;
        }

        private @NotNull List<SchemaType> targetTypes() {
            return this == ALL ? QUERY_TYPES : List.of(this);
        }
    }

    public record AdminObjectCandidate(@NotNull String name, @NotNull SchemaType schemaType) {
    }

    public record ClassLookupResult(@Nullable SmartPsiElementPointer<PsiElement> elementPointer,
                                    @NotNull String classPath) {
        private static final ClassLookupResult EMPTY = new ClassLookupResult(null, "");

        public boolean isEmpty() {
            return elementPointer == null && classPath.isBlank();
        }
    }

    public record TriggerQueryResult(@NotNull String schemaType,
                                     @NotNull String schemaName,
                                     @NotNull String state,
                                     @NotNull String eventType,
                                     int sequence,
                                     @NotNull String triggerName,
                                     @NotNull String program,
                                     @NotNull String method,
                                     @NotNull String sourcePath,
                                     int sourceLine) {
        public @NotNull String sourceDisplay() {
            if (sourcePath.isBlank()) {
                return "";
            }
            return sourceLine > 0 ? sourcePath + ":" + sourceLine : sourcePath;
        }
    }
}
