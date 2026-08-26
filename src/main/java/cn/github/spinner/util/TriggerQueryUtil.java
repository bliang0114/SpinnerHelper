package cn.github.spinner.util;

import cn.github.driver.MQLException;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TriggerQueryUtil {
    private static final Logger LOG = Logger.getInstance(TriggerQueryUtil.class);
    private static final String TRIGGER_PARAMETER_TYPE = "eService Trigger Program Parameters";
    private static final int TRIGGER_PROGRAM_BATCH_SIZE = 75;
    private static final Pattern PROGRAM_TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Set<String> SKIPPED_CLASS_SEARCH_DIRS = Set.of(
            ".git", ".gradle", ".idea", "node_modules", "build", "out", "target");

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
        return query(project, schemaType, name, stateFilter, true, true);
    }

    public static @NotNull List<TriggerQueryResult> query(@NotNull Project project,
                                                          @NotNull SchemaType schemaType,
                                                          @NotNull String name,
                                                          @Nullable String stateFilter,
                                                          boolean includeRelatedPolicies,
                                                          boolean useCache) throws MQLException {
        long queryStartedAt = System.nanoTime();
        String queryContext = "schemaType=" + schemaType.name() +
                ", name=" + logValue(name) +
                ", stateFilter=" + logValue(stateFilter) +
                ", includeRelatedPolicies=" + includeRelatedPolicies +
                ", useCache=" + useCache;
        LOG.info("[TriggerQuery] query started: " + queryContext);

        if (useCache) {
            long cacheStartedAt = System.nanoTime();
            TriggerQueryCache.CachedTriggerQueryResults cachedResults =
                    TriggerQueryCache.get(project, schemaType, name, stateFilter, includeRelatedPolicies);
            long cacheElapsedMs = elapsedMillis(cacheStartedAt);
            if (cachedResults.loaded()) {
                LOG.info("[TriggerQuery] cache hit: " + queryContext +
                        ", rows=" + cachedResults.results().size() +
                        ", cacheElapsedMs=" + cacheElapsedMs +
                        ", totalElapsedMs=" + elapsedMillis(queryStartedAt));
                return cachedResults.results();
            }
            LOG.info("[TriggerQuery] cache miss: " + queryContext +
                    ", cacheElapsedMs=" + cacheElapsedMs);
        } else {
            LOG.info("[TriggerQuery] cache read bypassed: " + queryContext);
        }

        List<TriggerQueryResult> results;
        long remoteStartedAt = System.nanoTime();
        try {
            results = queryRemote(project, schemaType, name, stateFilter, includeRelatedPolicies);
        } catch (MQLException e) {
            LOG.warn("[TriggerQuery] remote query failed: " + queryContext +
                    ", remoteElapsedMs=" + elapsedMillis(remoteStartedAt) +
                    ", totalElapsedMs=" + elapsedMillis(queryStartedAt), e);
            throw e;
        }
        long remoteElapsedMs = elapsedMillis(remoteStartedAt);

        long cacheWriteStartedAt = System.nanoTime();
        try {
            TriggerQueryCache.put(project, schemaType, name, stateFilter, includeRelatedPolicies, results);
            LOG.info("[TriggerQuery] cache write completed: " + queryContext +
                    ", rows=" + results.size() +
                    ", elapsedMs=" + elapsedMillis(cacheWriteStartedAt));
        } catch (MQLException e) {
            LOG.warn("[TriggerQuery] cache write failed: " + queryContext +
                    ", elapsedMs=" + elapsedMillis(cacheWriteStartedAt), e);
        }
        LOG.info("[TriggerQuery] query completed: " + queryContext +
                ", rows=" + results.size() +
                ", remoteElapsedMs=" + remoteElapsedMs +
                ", totalElapsedMs=" + elapsedMillis(queryStartedAt));
        return results;
    }

    private static @NotNull List<TriggerQueryResult> queryRemote(@NotNull Project project,
                                                                 @NotNull SchemaType schemaType,
                                                                 @NotNull String name,
                                                                 @Nullable String stateFilter,
                                                                 boolean includeRelatedPolicies) throws MQLException {
        List<TriggerQueryResult> results = new ArrayList<>();
        Map<String, String> triggerProgramCache = new HashMap<>();
        Map<ProgramMethodKey, SourceLocation> sourceLocationCache = new HashMap<>();
        Set<String> queriedPolicyKeys = new HashSet<>();

        long phaseStartedAt = System.nanoTime();
        int rowsBeforePhase = results.size();
        boolean matched = schemaType.includes(SchemaType.POLICY) && isPolicy(project, name);
        if (matched) {
            results.addAll(queryPolicyTriggerOnce(project, name, stateFilter, triggerProgramCache,
                    sourceLocationCache, queriedPolicyKeys));
        }
        logSchemaPhase(SchemaType.POLICY, name, matched, results.size() - rowsBeforePhase, phaseStartedAt);

        phaseStartedAt = System.nanoTime();
        rowsBeforePhase = results.size();
        matched = schemaType.includes(SchemaType.ATTRIBUTE) && isAttribute(project, name);
        if (matched) {
            results.addAll(queryAdminTrigger(project, "attribute", name, null,
                    "print attribute " + quote(name) + " select trigger dump", triggerProgramCache, sourceLocationCache));
        }
        logSchemaPhase(SchemaType.ATTRIBUTE, name, matched, results.size() - rowsBeforePhase, phaseStartedAt);

        phaseStartedAt = System.nanoTime();
        rowsBeforePhase = results.size();
        matched = schemaType.includes(SchemaType.RELATIONSHIP) && isRelationship(project, name);
        if (matched) {
            results.addAll(queryAdminTrigger(project, "relationship", name, null,
                    "print relationship " + quote(name) + " select trigger dump", triggerProgramCache, sourceLocationCache));
        }
        logSchemaPhase(SchemaType.RELATIONSHIP, name, matched, results.size() - rowsBeforePhase, phaseStartedAt);

        phaseStartedAt = System.nanoTime();
        rowsBeforePhase = results.size();
        matched = schemaType.includes(SchemaType.TYPE) && isType(project, name);
        if (matched) {
            results.addAll(queryTypeTrigger(project, name, stateFilter, triggerProgramCache,
                    sourceLocationCache, queriedPolicyKeys, includeRelatedPolicies));
        }
        logSchemaPhase(SchemaType.TYPE, name, matched, results.size() - rowsBeforePhase, phaseStartedAt);

        return sortResults(results);
    }

    private static @NotNull List<TriggerQueryResult> sortResults(@NotNull List<TriggerQueryResult> results) {
        List<TriggerQueryResult> distinctResults = results.stream().distinct().toList();
        Map<ResultGroupKey, Map<String, Integer>> stateOrders = new HashMap<>();
        for (TriggerQueryResult result : distinctResults) {
            ResultGroupKey groupKey = new ResultGroupKey(result.schemaType(), result.schemaName());
            Map<String, Integer> groupStateOrders = stateOrders.computeIfAbsent(groupKey, key -> new HashMap<>());
            groupStateOrders.computeIfAbsent(result.state(), state -> groupStateOrders.size());
        }

        return distinctResults.stream().sorted(Comparator
                .comparing(TriggerQueryResult::schemaType, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TriggerQueryResult::schemaName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(result -> stateOrders
                        .get(new ResultGroupKey(result.schemaType(), result.schemaName()))
                        .get(result.state()))
                .thenComparing(TriggerQueryResult::eventName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(result -> result.eventKind().order())
                .thenComparingInt(TriggerQueryResult::sequence)).toList();
    }

    public static @NotNull List<AdminObjectCandidate> listAdminObjects(@NotNull Project project,
                                                                       @NotNull SchemaType schemaType) throws MQLException {
        long startedAt = System.nanoTime();
        List<AdminObjectCandidate> results = new ArrayList<>();
        for (SchemaType targetType : schemaType.targetTypes()) {
            MatrixAdminDefinitionCache.AdminType adminType = targetType.adminType();
            if (adminType == null) {
                continue;
            }
            for (String name : MatrixAdminDefinitionCache.get(project, adminType)) {
                results.add(new AdminObjectCandidate(name, targetType));
            }
        }
        List<AdminObjectCandidate> candidates = results.stream().distinct().sorted(Comparator
                .comparing(AdminObjectCandidate::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.schemaType().mqlName())).toList();
        LOG.info("[TriggerQuery] suggestions loaded: schemaType=" + schemaType.name() +
                ", rows=" + candidates.size() +
                ", elapsedMs=" + elapsedMillis(startedAt));
        return candidates;
    }

    public static @NotNull List<String> listPolicyStates(@NotNull Project project,
                                                          @NotNull String policyName) throws MQLException {
        String stateResult = executeMql(project, "policy-states name=" + policyName,
                "print policy " + quote(policyName) + " select state dump");
        return parseDumpValues(stateResult);
    }

    private static @NotNull List<TriggerQueryResult> queryTypeTrigger(@NotNull Project project,
                                                                      @NotNull String typeName,
                                                                      @Nullable String stateFilter,
                                                                      @NotNull Map<String, String> triggerProgramCache,
                                                                      @NotNull Map<ProgramMethodKey, SourceLocation> sourceLocationCache,
                                                                      @NotNull Set<String> queriedPolicyKeys,
                                                                      boolean includeRelatedPolicies) throws MQLException {
        List<TriggerQueryResult> results = new ArrayList<>();
        String triggerResult = executeMql(project, "type-trigger name=" + typeName,
                "print type " + quote(typeName) + " select trigger dump");
        results.addAll(queryTriggerPrograms(project, "type", typeName, null, triggerResult, triggerProgramCache, sourceLocationCache));

        if (!includeRelatedPolicies) {
            LOG.info("[TriggerQuery] related policy query skipped: type=" + logValue(typeName));
            return results;
        }
        for (String policyName : queryTypePolicies(project, typeName)) {
            results.addAll(queryPolicyTriggerOnce(project, policyName, stateFilter, triggerProgramCache,
                    sourceLocationCache, queriedPolicyKeys));
        }
        return results;
    }

    private static @NotNull List<TriggerQueryResult> queryPolicyTriggerOnce(
            @NotNull Project project,
            @NotNull String policyName,
            @Nullable String stateFilter,
            @NotNull Map<String, String> triggerProgramCache,
            @NotNull Map<ProgramMethodKey, SourceLocation> sourceLocationCache,
            @NotNull Set<String> queriedPolicyKeys) throws MQLException {
        String policyKey = policyName.trim().toLowerCase(Locale.ROOT) + "\u001F" +
                (stateFilter == null ? "" : stateFilter.trim().toLowerCase(Locale.ROOT));
        if (!queriedPolicyKeys.add(policyKey)) {
            LOG.info("[TriggerQuery] duplicate policy skipped: name=" + logValue(policyName) +
                    ", stateFilter=" + logValue(stateFilter));
            return List.of();
        }
        return queryPolicyTrigger(project, policyName, stateFilter, triggerProgramCache, sourceLocationCache);
    }

    private static @NotNull List<String> queryTypePolicies(@NotNull Project project,
                                                           @NotNull String typeName) throws MQLException {
        String policyResult = executeMql(project, "type-policy name=" + typeName,
                "print type " + quote(typeName) + " select policy dump");
        return parseDumpValues(policyResult);
    }

    private static @NotNull List<TriggerQueryResult> queryPolicyTrigger(@NotNull Project project,
                                                                        @NotNull String policyName,
                                                                        @Nullable String stateFilter,
                                                                        @NotNull Map<String, String> triggerProgramCache,
                                                                        @NotNull Map<ProgramMethodKey, SourceLocation> sourceLocationCache) throws MQLException {
        List<String> policyStates = listPolicyStates(project, policyName);
        if (policyStates.isEmpty()) {
            return List.of();
        }

        List<String> targetStates = parseStateFilter(stateFilter);
        List<TriggerQueryResult> results = new ArrayList<>();
        for (String state : policyStates) {
            if (!targetStates.isEmpty() && !targetStates.contains(state)) {
                continue;
            }
            String triggerResult = executeMql(project,
                    "policy-state-trigger name=" + policyName + ", state=" + state,
                    "print policy " + quote(policyName) + " select state[" + state + "].trigger dump");
            results.addAll(queryTriggerPrograms(project, "policy", policyName, state,
                    triggerResult, triggerProgramCache, sourceLocationCache));
        }
        return results;
    }

    private static @NotNull List<TriggerQueryResult> queryAdminTrigger(@NotNull Project project,
                                                                       @NotNull String schemaType,
                                                                       @NotNull String schemaName,
                                                                       @Nullable String state,
                                                                       @NotNull String mql,
                                                                       @NotNull Map<String, String> triggerProgramCache,
                                                                       @NotNull Map<ProgramMethodKey, SourceLocation> sourceLocationCache) throws MQLException {
        String triggerResult = executeMql(project,
                schemaType + "-trigger name=" + schemaName, mql);
        return queryTriggerPrograms(project, schemaType, schemaName, state,
                triggerResult, triggerProgramCache, sourceLocationCache);
    }

    private static @NotNull List<TriggerQueryResult> queryTriggerPrograms(@NotNull Project project,
                                                                          @NotNull String schemaType,
                                                                          @NotNull String schemaName,
                                                                          @Nullable String state,
                                                                          @Nullable String triggerResult,
                                                                          @NotNull Map<String, String> triggerProgramCache,
                                                                          @NotNull Map<ProgramMethodKey, SourceLocation> sourceLocationCache) throws MQLException {
        if (CharSequenceUtil.isBlank(triggerResult)) {
            return List.of();
        }

        List<TriggerSpec> triggerSpecs = parseTriggerSpecs(triggerResult);
        preloadTriggerPrograms(project, triggerSpecs, triggerProgramCache);
        List<TriggerQueryResult> results = new ArrayList<>();
        for (TriggerSpec triggerSpec : triggerSpecs) {
            String triggerProgramResult = queryTriggerProgram(project, triggerSpec.triggerName(), triggerProgramCache);
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

                String className = program + "_mxJPO";
                SourceLocation sourceLocation = findSourceLocation(project, className, method, sourceLocationCache);
                results.add(new TriggerQueryResult(
                        schemaType,
                        schemaName,
                        state == null ? "" : state,
                        triggerSpec.eventType(),
                        parseSequence(sequence),
                        triggerSpec.triggerName(),
                        className,
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
            int openIndex = triggerText.indexOf('(');
            int closeIndex = triggerText.indexOf(')', openIndex + 1);
            if (openIndex <= 0 || closeIndex <= openIndex) {
                continue;
            }
            String eventType = parseTriggerPrefixEventType(triggerText.substring(0, openIndex));
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

    private static @NotNull String parseTriggerPrefixEventType(@NotNull String triggerPrefix) {
        String normalizedPrefix = triggerPrefix.trim();
        if (normalizedPrefix.isEmpty()) {
            return "";
        }

        String[] parts = normalizedPrefix.split(":");
        if (parts.length >= 2 && TriggerEventKind.fromText(parts[1].trim()) != null) {
            return parts[0].trim() + ":" + parts[1].trim();
        }
        return parts[0].trim();
    }

    private static void preloadTriggerPrograms(@NotNull Project project,
                                               @NotNull List<TriggerSpec> triggerSpecs,
                                               @NotNull Map<String, String> triggerProgramCache) {
        List<String> triggerNames = triggerSpecs.stream()
                .map(TriggerSpec::triggerName)
                .filter(name -> !triggerProgramCache.containsKey(name))
                .filter(TriggerQueryUtil::isBatchSafeTriggerName)
                .distinct()
                .toList();
        if (triggerNames.size() < 2) {
            return;
        }

        long startedAt = System.nanoTime();
        int loadedCount = 0;
        for (int start = 0; start < triggerNames.size(); start += TRIGGER_PROGRAM_BATCH_SIZE) {
            List<String> batch = triggerNames.subList(start,
                    Math.min(start + TRIGGER_PROGRAM_BATCH_SIZE, triggerNames.size()));
            if (loadTriggerProgramBatch(project, batch, triggerProgramCache)) {
                loadedCount += batch.size();
            }
        }
        LOG.info("[TriggerQuery] trigger programs preloaded: requested=" + triggerNames.size() +
                ", loaded=" + loadedCount +
                ", elapsedMs=" + elapsedMillis(startedAt));
    }

    private static boolean loadTriggerProgramBatch(@NotNull Project project,
                                                   @NotNull List<String> triggerNames,
                                                   @NotNull Map<String, String> triggerProgramCache) {
        String matchList = String.join(",", triggerNames);
        String whereExpression = "name matchlist '" + matchList + "' ','";
        String mql = "temp query bus " + quote(TRIGGER_PARAMETER_TYPE) + " * * where " +
                quote(whereExpression) +
                " select attribute[*Name] current attribute[eService Sequence Number] dump |";

        String batchResult;
        try {
            batchResult = executeMql(project,
                    "trigger-program-batch count=" + triggerNames.size(), mql);
        } catch (MQLException e) {
            LOG.info("[TriggerQuery] trigger program batch unavailable; falling back to individual queries: count=" +
                    triggerNames.size());
            return false;
        }

        Map<String, StringBuilder> rowsByTrigger = new HashMap<>();
        Set<String> requestedNames = new HashSet<>(triggerNames);
        if (CharSequenceUtil.isNotBlank(batchResult)) {
            for (String line : batchResult.split("\\R")) {
                String triggerName = findTriggerNameInBatchRow(line, requestedNames);
                if (triggerName == null) {
                    continue;
                }
                StringBuilder rows = rowsByTrigger.computeIfAbsent(triggerName, key -> new StringBuilder());
                if (!rows.isEmpty()) {
                    rows.append('\n');
                }
                rows.append(line);
            }
            if (rowsByTrigger.isEmpty()) {
                LOG.warn("[TriggerQuery] trigger program batch returned an unrecognized result; " +
                        "falling back to individual queries: count=" + triggerNames.size());
                return false;
            }
        }

        for (String triggerName : triggerNames) {
            StringBuilder rows = rowsByTrigger.get(triggerName);
            triggerProgramCache.put(triggerName, rows == null ? "" : rows.toString());
        }
        return true;
    }

    private static @Nullable String findTriggerNameInBatchRow(@NotNull String line,
                                                               @NotNull Set<String> requestedNames) {
        String[] values = line.split("\\|", -1);
        int metadataValueCount = Math.max(values.length - 4, 0);
        for (int i = 0; i < metadataValueCount; i++) {
            String value = values[i].trim();
            if (requestedNames.contains(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBatchSafeTriggerName(@NotNull String triggerName) {
        return triggerName.indexOf(',') < 0 && triggerName.indexOf('\'') < 0 &&
                triggerName.indexOf('\r') < 0 && triggerName.indexOf('\n') < 0;
    }

    private static @NotNull String queryTriggerProgram(@NotNull Project project,
                                                       @NotNull String triggerName,
                                                       @NotNull Map<String, String> triggerProgramCache) throws MQLException {
        String cachedResult = triggerProgramCache.get(triggerName);
        if (cachedResult != null) {
            return cachedResult;
        }

        String triggerProgramResult;
        try {
            triggerProgramResult = executeMql(project, "trigger-program name=" + triggerName,
                    "temp query bus " + quote(TRIGGER_PARAMETER_TYPE) + " " + quote(triggerName) +
                            " * select attribute[*Name] current attribute[eService Sequence Number] dump |");
        } catch (MQLException ignored) {
            triggerProgramResult = "";
        }
        triggerProgramCache.put(triggerName, triggerProgramResult);
        return triggerProgramResult;
    }

    private static @NotNull SourceLocation findSourceLocation(@NotNull Project project,
                                                              @NotNull String className,
                                                              @NotNull String methodName,
                                                              @NotNull Map<ProgramMethodKey, SourceLocation> cache) {
        ProgramMethodKey key = new ProgramMethodKey(className, methodName);
        SourceLocation cachedLocation = cache.get(key);
        if (cachedLocation != null) {
            LOG.debug("[TriggerQuery] source lookup cache hit: class=" + logValue(className) +
                    ", method=" + logValue(methodName) +
                    ", found=" + !cachedLocation.path().isBlank());
            return cachedLocation;
        }

        long startedAt = System.nanoTime();
        SourceLocation sourceLocation = findSourceLocation(project, className, methodName);
        cache.put(key, sourceLocation);
        LOG.info("[TriggerQuery] source lookup completed: class=" + logValue(className) +
                ", method=" + logValue(methodName) +
                ", found=" + !sourceLocation.path().isBlank() +
                ", elapsedMs=" + elapsedMillis(startedAt));
        return sourceLocation;
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
        Set<String> fileNames = javaSourceFileNames(className);
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
                    if (fileNames.contains(file.getFileName().toString())) {
                        result.set(file);
                        return FileVisitResult.TERMINATE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
        return result.get();
    }

    private static @NotNull Set<String> javaSourceFileNames(@NotNull String className) {
        String shortName = toShortClassName(className);
        LinkedHashSet<String> fileNames = new LinkedHashSet<>();
        fileNames.add(shortName + ".java");
        String programName = toProgramName(shortName);
        fileNames.add(programName + ".java");
        if (!shortName.endsWith("_mxJPO")) {
            fileNames.add(shortName + "_mxJPO.java");
        }
        return fileNames;
    }

    public static @NotNull String queryProgramCode(@NotNull Project project, @NotNull String jpoClassName) throws MQLException {
        String programName = toProgramName(jpoClassName);
        String sourceCode = executeMql(project, "program-code name=" + programName,
                "print prog " + quote(programName) + " select code dump");
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
        ClassLookupResult indexedTarget = findIndexedClassTarget(project, jpoClassName, methodName);
        if (indexedTarget != null) {
            return indexedTarget;
        }

        Path classFile = findClassFile(project, jpoClassName);
        if (classFile == null) {
            return ClassLookupResult.EMPTY;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(classFile);
        return virtualFile == null ? ClassLookupResult.EMPTY : new ClassLookupResult(virtualFile, 0);
    }

    public static @NotNull SourceLookupResult findProjectSourceTarget(@NotNull Project project,
                                                                      @NotNull String jpoClassName,
                                                                      @NotNull String methodName) {
        SourceLocation sourceLocation = findSourceLocation(project, jpoClassName, methodName);
        return new SourceLookupResult(sourceLocation.path(), sourceLocation.line());
    }

    @SuppressWarnings("deprecation")
    private static @Nullable ClassLookupResult findIndexedClassTarget(@NotNull Project project,
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
            PsiElement navigationElement = target.getNavigationElement();
            if (!navigationElement.isValid()) {
                return null;
            }
            PsiFile containingFile = navigationElement.getContainingFile();
            VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
            if (virtualFile == null) {
                return null;
            }
            return new ClassLookupResult(virtualFile, Math.max(navigationElement.getTextOffset(), 0));
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
        return MatrixAdminDefinitionCache.contains(project, MatrixAdminDefinitionCache.AdminType.ATTRIBUTE, name);
    }

    private static boolean isPolicy(@NotNull Project project, @NotNull String name) {
        return MatrixAdminDefinitionCache.contains(project, MatrixAdminDefinitionCache.AdminType.POLICY, name);
    }

    private static boolean isRelationship(@NotNull Project project, @NotNull String name) {
        return MatrixAdminDefinitionCache.contains(project, MatrixAdminDefinitionCache.AdminType.RELATIONSHIP, name);
    }

    private static boolean isType(@NotNull Project project, @NotNull String name) {
        return MatrixAdminDefinitionCache.contains(project, MatrixAdminDefinitionCache.AdminType.TYPE, name);
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

    private static @NotNull TriggerEventType parseEventType(@NotNull String eventType) {
        String normalizedEventType = eventType.trim();
        if (normalizedEventType.isEmpty()) {
            return new TriggerEventType("", TriggerEventKind.UNKNOWN);
        }

        int colonIndex = normalizedEventType.indexOf(':');
        if (colonIndex >= 0) {
            String eventName = normalizedEventType.substring(0, colonIndex).trim();
            String eventKindText = normalizedEventType.substring(colonIndex + 1).trim();
            TriggerEventKind kind = TriggerEventKind.fromText(eventKindText);
            return new TriggerEventType(eventName, kind == null ? TriggerEventKind.UNKNOWN : kind);
        }

        String[] parts = normalizedEventType.split("\\s+");
        TriggerEventKind kind = TriggerEventKind.fromText(parts[parts.length - 1]);
        if (kind != null) {
            String eventName = normalizedEventType.substring(0,
                    normalizedEventType.length() - parts[parts.length - 1].length()).trim();
            return new TriggerEventType(eventName, kind);
        }

        for (TriggerEventKind eventKind : TriggerEventKind.values()) {
            if (eventKind == TriggerEventKind.UNKNOWN) {
                continue;
            }
            String displayName = eventKind.displayName();
            if (normalizedEventType.length() > displayName.length() &&
                    normalizedEventType.regionMatches(true,
                            normalizedEventType.length() - displayName.length(),
                            displayName, 0, displayName.length())) {
                String eventName = normalizedEventType.substring(0,
                        normalizedEventType.length() - displayName.length()).trim();
                return new TriggerEventType(eventName, eventKind);
            }
        }

        return new TriggerEventType(normalizedEventType, TriggerEventKind.UNKNOWN);
    }

    private static @NotNull String quote(@NotNull String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static @NotNull String executeMql(@NotNull Project project,
                                              @NotNull String operation,
                                              @NotNull String mql) throws MQLException {
        long startedAt = System.nanoTime();
        LOG.debug("[TriggerQuery] MQL started: operation=" + logValue(operation) +
                ", command=" + logValue(mql));
        try {
            String result = MQLUtil.execute(project, mql);
            LOG.info("[TriggerQuery] MQL completed: operation=" + logValue(operation) +
                    ", resultChars=" + (result == null ? 0 : result.length()) +
                    ", elapsedMs=" + elapsedMillis(startedAt));
            return result;
        } catch (MQLException e) {
            LOG.warn("[TriggerQuery] MQL failed: operation=" + logValue(operation) +
                    ", elapsedMs=" + elapsedMillis(startedAt), e);
            throw e;
        }
    }

    private static void logSchemaPhase(@NotNull SchemaType schemaType,
                                       @NotNull String name,
                                       boolean matched,
                                       int rows,
                                       long startedAt) {
        LOG.info("[TriggerQuery] schema phase completed: schemaType=" + schemaType.name() +
                ", name=" + logValue(name) +
                ", matched=" + matched +
                ", rows=" + rows +
                ", elapsedMs=" + elapsedMillis(startedAt));
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static @NotNull String logValue(@Nullable String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
    }

    private record TriggerSpec(@NotNull String eventType, @NotNull String triggerName) {
    }

    private record ProgramMethodKey(@NotNull String className, @NotNull String methodName) {
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

        private @Nullable MatrixAdminDefinitionCache.AdminType adminType() {
            return switch (this) {
                case TYPE -> MatrixAdminDefinitionCache.AdminType.TYPE;
                case POLICY -> MatrixAdminDefinitionCache.AdminType.POLICY;
                case RELATIONSHIP -> MatrixAdminDefinitionCache.AdminType.RELATIONSHIP;
                case ATTRIBUTE -> MatrixAdminDefinitionCache.AdminType.ATTRIBUTE;
                case ALL -> null;
            };
        }

        private @NotNull List<SchemaType> targetTypes() {
            return this == ALL ? QUERY_TYPES : List.of(this);
        }
    }

    public record AdminObjectCandidate(@NotNull String name, @NotNull SchemaType schemaType) {
    }

    public enum TriggerEventKind {
        CHECK("Check", 0),
        OVERRIDE("Override", 1),
        ACTION("Action", 2),
        UNKNOWN("", 3);

        private final String displayName;
        private final int order;

        TriggerEventKind(@NotNull String displayName, int order) {
            this.displayName = displayName;
            this.order = order;
        }

        public @NotNull String displayName() {
            return displayName;
        }

        private int order() {
            return order;
        }

        private static @Nullable TriggerEventKind fromText(@NotNull String value) {
            for (TriggerEventKind kind : values()) {
                if (kind.displayName.equalsIgnoreCase(value)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private record TriggerEventType(@NotNull String eventName, @NotNull TriggerEventKind eventKind) {
    }

    private record ResultGroupKey(@NotNull String schemaType, @NotNull String schemaName) {
    }

    public record ClassLookupResult(@Nullable VirtualFile virtualFile, int offset) {
        private static final ClassLookupResult EMPTY = new ClassLookupResult(null, 0);

        public boolean isEmpty() {
            return virtualFile == null;
        }
    }

    public record SourceLookupResult(@NotNull String sourcePath, int sourceLine) {
        public boolean isEmpty() {
            return sourcePath.isBlank();
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
        public @NotNull String eventName() {
            return parseEventType(eventType).eventName();
        }

        public @NotNull TriggerEventKind eventKind() {
            return parseEventType(eventType).eventKind();
        }

        public @NotNull String sourceDisplay() {
            if (sourcePath.isBlank()) {
                return "";
            }
            return sourceLine > 0 ? sourcePath + ":" + sourceLine : sourcePath;
        }
    }
}
