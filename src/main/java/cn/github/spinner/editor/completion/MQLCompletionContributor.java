package cn.github.spinner.editor.completion;

import cn.github.spinner.editor.MQLKeywords;
import cn.github.spinner.editor.MQLLanguage;
import cn.github.spinner.editor.icons.MQLIcons;
import cn.github.spinner.util.MatrixAdminDefinitionCache;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class MQLCompletionContributor extends CompletionContributor {
    private enum CompletionContext {
        DEFAULT,
        TYPE_INSTANCE,
        TYPE_INSTANCE_IN_QUOTES,
        RELATIONSHIP_INSTANCE,
        RELATIONSHIP_INSTANCE_IN_QUOTES,
        POLICY_INSTANCE,
        POLICY_INSTANCE_IN_QUOTES,
        ATTRIBUTE_INSTANCE,
        ATTRIBUTE_INSTANCE_IN_QUOTES,
        INTERFACE_INSTANCE,
        INTERFACE_INSTANCE_IN_QUOTES,
        QUERY_BUS_TYPE,
        QUERY_BUS_TYPE_IN_QUOTES,
        QUERY_CONNECTION_REL_TARGET,
        WHERE_EXPRESSION,
        WHERE_ATTRIBUTE_SELECTOR
    }

    public MQLCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(MQLLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        CompletionResultSet caseInsensitiveResult = result.caseInsensitive();
                        CompletionContext completionContext = resolveCompletionContext(parameters);
                        Project project = parameters.getOriginalFile().getProject();
                        switch (completionContext) {
                            case TYPE_INSTANCE -> {
                                addTypeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, true, false);
                                return;
                            }
                            case TYPE_INSTANCE_IN_QUOTES -> {
                                addTypeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, false, true);
                                return;
                            }
                            case RELATIONSHIP_INSTANCE -> {
                                addRelationshipInstances(cached(project, MatrixAdminDefinitionCache.AdminType.RELATIONSHIP), caseInsensitiveResult, true, false);
                                return;
                            }
                            case RELATIONSHIP_INSTANCE_IN_QUOTES -> {
                                addRelationshipInstances(cached(project, MatrixAdminDefinitionCache.AdminType.RELATIONSHIP), caseInsensitiveResult, false, true);
                                return;
                            }
                            case POLICY_INSTANCE -> {
                                addPolicyInstances(cached(project, MatrixAdminDefinitionCache.AdminType.POLICY), caseInsensitiveResult, true, false);
                                return;
                            }
                            case POLICY_INSTANCE_IN_QUOTES -> {
                                addPolicyInstances(cached(project, MatrixAdminDefinitionCache.AdminType.POLICY), caseInsensitiveResult, false, true);
                                return;
                            }
                            case ATTRIBUTE_INSTANCE -> {
                                addAttributeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.ATTRIBUTE), caseInsensitiveResult, true, false);
                                return;
                            }
                            case ATTRIBUTE_INSTANCE_IN_QUOTES -> {
                                addAttributeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.ATTRIBUTE), caseInsensitiveResult, false, true);
                                return;
                            }
                            case INTERFACE_INSTANCE -> {
                                addInterfaceInstances(cached(project, MatrixAdminDefinitionCache.AdminType.INTERFACE), caseInsensitiveResult, true, false);
                                return;
                            }
                            case INTERFACE_INSTANCE_IN_QUOTES -> {
                                addInterfaceInstances(cached(project, MatrixAdminDefinitionCache.AdminType.INTERFACE), caseInsensitiveResult, false, true);
                                return;
                            }
                            case QUERY_BUS_TYPE -> {
                                caseInsensitiveResult.addElement(LookupElementBuilder.create("*")
                                        .withTypeText("Wildcard")
                                        .withBoldness(true));
                                addQueryBusTypeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, false);
                                return;
                            }
                            case QUERY_BUS_TYPE_IN_QUOTES -> {
                                addQueryBusTypeInstances(cached(project, MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, true);
                                return;
                            }
                            case QUERY_CONNECTION_REL_TARGET -> {
                                MatrixAdminDefinitionCache.MatrixAdminDefinitions definitions =
                                        MatrixAdminDefinitionCache.getCached(project);
                                addTypeInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, false, true);
                                addRelationshipInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.RELATIONSHIP), caseInsensitiveResult, false, true);
                                return;
                            }
                            case WHERE_EXPRESSION -> {
                                MatrixAdminDefinitionCache.MatrixAdminDefinitions definitions =
                                        MatrixAdminDefinitionCache.getCached(project);
                                addTypeInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, false, true);
                                addRelationshipInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.RELATIONSHIP), caseInsensitiveResult, false, true);
                                addPolicyInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.POLICY), caseInsensitiveResult, false, true);
                                addAttributeInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.ATTRIBUTE), caseInsensitiveResult, false, true);
                                addInterfaceInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.INTERFACE), caseInsensitiveResult, false, true);
                                return;
                            }
                            case WHERE_ATTRIBUTE_SELECTOR -> {
                                // The PSI prefix inside a quoted string includes everything
                                // from the opening quote (e.g. "attribute[Partial).  Use only
                                // the bracket content as the prefix so items actually match.
                                Document completionDocument = parameters.getEditor().getDocument();
                                String beforeCaretForPrefix = completionDocument.getText(new TextRange(0, parameters.getOffset()));
                                String stmtPrefixForBracket = getCurrentStatementPrefix(beforeCaretForPrefix);
                                int bracketIdx = stmtPrefixForBracket.lastIndexOf('[');
                                String insideBracket = bracketIdx >= 0 ? stmtPrefixForBracket.substring(bracketIdx + 1) : "";
                                CompletionResultSet unfiltered = caseInsensitiveResult.withPrefixMatcher(insideBracket);
                                for (String instance : cached(project, MatrixAdminDefinitionCache.AdminType.ATTRIBUTE)) {
                                    unfiltered.addElement(buildAttributeSelectorElement(instance));
                                }
                                return;
                            }
                            case DEFAULT -> {
                            }
                        }

                        for (String keyword : MQLKeywords.KEYWORDS) {
                            caseInsensitiveResult.addElement(buildKeywordElement(keyword));
                        }

                        for (String type : MQLKeywords.TYPES) {
                            caseInsensitiveResult.addElement(LookupElementBuilder.create(type)
                                    .withTypeText("Type")
                                    .withIcon(MQLIcons.TYPE));
                        }

                        MatrixAdminDefinitionCache.MatrixAdminDefinitions definitions =
                                MatrixAdminDefinitionCache.getCached(project);
                        addTypeInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.TYPE), caseInsensitiveResult, false, false);
                        addRelationshipInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.RELATIONSHIP), caseInsensitiveResult, false, false);
                        addPolicyInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.POLICY), caseInsensitiveResult, false, false);
                        addAttributeInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.ATTRIBUTE), caseInsensitiveResult, false, false);
                        addInterfaceInstances(definitions.get(MatrixAdminDefinitionCache.AdminType.INTERFACE), caseInsensitiveResult, false, false);
                    }
                });
    }

    private @NotNull LookupElementBuilder buildKeywordElement(@NotNull String keyword) {
        return LookupElementBuilder.create(keyword)
                .withTypeText("Keyword")
                .withBoldness(true)
                .withInsertHandler(new KeywordInsertHandler(keyword));
    }

    private @NotNull List<String> cached(@NotNull Project project,
                                         @NotNull MatrixAdminDefinitionCache.AdminType type) {
        return MatrixAdminDefinitionCache.getCached(project, type);
    }

    private void addTypeInstances(@NotNull List<String> instances,
                                  @NotNull CompletionResultSet result,
                                  boolean quoteIfNeeded,
                                  boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildTypeInstanceElement(instance, quoteIfNeeded, insideQuotedList));
        }
    }

    private void addRelationshipInstances(@NotNull List<String> instances,
                                          @NotNull CompletionResultSet result,
                                          boolean quoteIfNeeded,
                                          boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildRelationshipInstanceElement(instance, quoteIfNeeded, insideQuotedList));
        }
    }

    private void addPolicyInstances(@NotNull List<String> instances,
                                    @NotNull CompletionResultSet result,
                                    boolean quoteIfNeeded,
                                    boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildAdminInstanceElement(instance, "Policy Definition", quoteIfNeeded, insideQuotedList, "policy"));
        }
    }

    private void addAttributeInstances(@NotNull List<String> instances,
                                       @NotNull CompletionResultSet result,
                                       boolean quoteIfNeeded,
                                       boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildAdminInstanceElement(instance, "Attribute Definition",
                    quoteIfNeeded, insideQuotedList, "attribute", "attr"));
        }
    }

    private void addInterfaceInstances(@NotNull List<String> instances,
                                       @NotNull CompletionResultSet result,
                                       boolean quoteIfNeeded,
                                       boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildAdminInstanceElement(instance, "Interface Definition", quoteIfNeeded, insideQuotedList, "interface"));
        }
    }

    private void addQueryBusTypeInstances(@NotNull List<String> instances, @NotNull CompletionResultSet result, boolean insideQuotedList) {
        for (String instance : instances) {
            result.addElement(buildQueryBusTypeElement(instance, insideQuotedList));
        }
    }
    private @NotNull LookupElementBuilder buildNamedInstanceElement(@NotNull String instance,
                                                                    @NotNull String typeText,
                                                                    boolean quoteIfNeeded) {
        String insertValue = quoteIfNeeded(instance, quoteIfNeeded);
        LookupElementBuilder builder = LookupElementBuilder.create(insertValue)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText(typeText)
                .withIcon(MQLIcons.TYPE);
        if (!insertValue.equals(instance)) {
            builder = builder.withLookupString(insertValue);
        }
        return builder;
    }

    private @NotNull LookupElementBuilder buildQueryBusTypeElement(@NotNull String instance, boolean insideQuotedList) {
        String quotedValue = quoteIfNeeded(instance, true);
        LookupElementBuilder builder = LookupElementBuilder.create(instance)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText("Type Definition")
                .withIcon(MQLIcons.TYPE)
                .withInsertHandler(new QueryBusTypeInsertHandler(instance, insideQuotedList));
        if (!quotedValue.equals(instance)) {
            builder = builder.withLookupString(quotedValue);
        }
        return builder;
    }

    private @NotNull LookupElementBuilder buildTypeInstanceElement(@NotNull String instance,
                                                                   boolean quoteIfNeeded,
                                                                   boolean insideQuotedList) {
        String quotedValue = quoteIfNeeded(instance, quoteIfNeeded);
        LookupElementBuilder builder = LookupElementBuilder.create(instance)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText("Type Definition")
                .withIcon(MQLIcons.TYPE)
                .withInsertHandler(new TypeInsertHandler(instance, quoteIfNeeded, insideQuotedList));
        if (!quotedValue.equals(instance)) {
            builder = builder.withLookupString(quotedValue);
        }
        return builder;
    }

    private @NotNull LookupElementBuilder buildRelationshipInstanceElement(@NotNull String instance,
                                                                           boolean quoteIfNeeded,
                                                                           boolean insideQuotedList) {
        String quotedValue = quoteIfNeeded(instance, quoteIfNeeded);
        LookupElementBuilder builder = LookupElementBuilder.create(instance)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText("Relationship Definition")
                .withIcon(MQLIcons.TYPE)
                .withInsertHandler(new RelationshipInsertHandler(instance, quoteIfNeeded, insideQuotedList));
        if (!quotedValue.equals(instance)) {
            builder = builder.withLookupString(quotedValue);
        }
        return builder;
    }

    private @NotNull LookupElementBuilder buildAttributeSelectorElement(@NotNull String instance) {
        return LookupElementBuilder.create(instance)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText("Attribute Definition")
                .withIcon(MQLIcons.TYPE)
                .withInsertHandler(new AttributeSelectorInsertHandler(instance));
    }

    private @NotNull LookupElementBuilder buildAdminInstanceElement(@NotNull String instance,
                                                                    @NotNull String typeText,
                                                                    boolean quoteIfNeeded,
                                                                    boolean insideQuotedList,
                                                                    @NotNull String... keywords) {
        String quotedValue = quoteIfNeeded(instance, quoteIfNeeded);
        LookupElementBuilder builder = LookupElementBuilder.create(instance)
                .withPresentableText(instance)
                .withLookupString(instance)
                .withTypeText(typeText)
                .withIcon(MQLIcons.TYPE)
                .withInsertHandler(new AdminInsertHandler(instance, quoteIfNeeded, insideQuotedList, keywords));
        if (!quotedValue.equals(instance)) {
            builder = builder.withLookupString(quotedValue);
        }
        return builder;
    }

    private @NotNull CompletionContext resolveCompletionContext(@NotNull CompletionParameters parameters) {
        Document document = parameters.getEditor().getDocument();
        int offset = parameters.getOffset();
        String beforeCaret = document.getText(new TextRange(0, offset));
        String statementPrefix = getCurrentStatementPrefix(beforeCaret);

        // Check for attribute[...] / attr[...] before WHERE, so both
        // "where "attribute[" and "select attribute[" share the same path.
        if (isOpenAttributeSelector(statementPrefix)) {
            return CompletionContext.WHERE_ATTRIBUTE_SELECTOR;
        }

        CompletionContext whereExpressionContext = resolveWhereExpressionContext(statementPrefix);
        if (whereExpressionContext != CompletionContext.DEFAULT) {
            return whereExpressionContext;
        }
        if (isTempQueryBusTypeInQuotesContext(statementPrefix)) {
            return CompletionContext.QUERY_BUS_TYPE_IN_QUOTES;
        }
        if (isTempQueryBusTypeContext(statementPrefix)) {
            return CompletionContext.QUERY_BUS_TYPE;
        }
        if (isQueryConnectionRelTargetContext(statementPrefix)) {
            return CompletionContext.QUERY_CONNECTION_REL_TARGET;
        }
        if (isQuotedKeywordValueContext(statementPrefix, "type")) {
            return CompletionContext.TYPE_INSTANCE_IN_QUOTES;
        }
        if (isQuotedKeywordListContinuationContext(statementPrefix, "type")) {
            return CompletionContext.TYPE_INSTANCE;
        }
        if (isQuotedKeywordValueContext(statementPrefix, "rel", "relationship")) {
            return CompletionContext.RELATIONSHIP_INSTANCE_IN_QUOTES;
        }
        if (isQuotedKeywordListContinuationContext(statementPrefix, "rel", "relationship")) {
            return CompletionContext.RELATIONSHIP_INSTANCE;
        }
        if (isQuotedKeywordValueContext(statementPrefix, "policy")) {
            return CompletionContext.POLICY_INSTANCE_IN_QUOTES;
        }
        if (isQuotedKeywordListContinuationContext(statementPrefix, "policy")) {
            return CompletionContext.POLICY_INSTANCE;
        }
        if (isQuotedKeywordValueContext(statementPrefix, "attribute", "attr")) {
            return CompletionContext.ATTRIBUTE_INSTANCE_IN_QUOTES;
        }
        if (isQuotedKeywordListContinuationContext(statementPrefix, "attribute", "attr")) {
            return CompletionContext.ATTRIBUTE_INSTANCE;
        }
        if (isQuotedKeywordValueContext(statementPrefix, "interface")) {
            return CompletionContext.INTERFACE_INSTANCE_IN_QUOTES;
        }
        if (isQuotedKeywordListContinuationContext(statementPrefix, "interface")) {
            return CompletionContext.INTERFACE_INSTANCE;
        }
        String previousWord = getPreviousWord(statementPrefix);
        if ("type".equalsIgnoreCase(previousWord)) {
            return CompletionContext.TYPE_INSTANCE;
        }
        if ("rel".equalsIgnoreCase(previousWord) || "relationship".equalsIgnoreCase(previousWord)) {
            return CompletionContext.RELATIONSHIP_INSTANCE;
        }
        if ("policy".equalsIgnoreCase(previousWord)) {
            return CompletionContext.POLICY_INSTANCE;
        }
        if ("attribute".equalsIgnoreCase(previousWord) || "attr".equalsIgnoreCase(previousWord)) {
            return CompletionContext.ATTRIBUTE_INSTANCE;
        }
        if ("interface".equalsIgnoreCase(previousWord)) {
            return CompletionContext.INTERFACE_INSTANCE;
        }
        return CompletionContext.DEFAULT;
    }

    /**
     * Detects {@code attribute[} or {@code attr[} with no closing {@code ]}
     * anywhere in the statement prefix, including inside a WHERE quoted value.
     */
    private boolean isOpenAttributeSelector(@NotNull String statementPrefix) {
        int bracketIndex = statementPrefix.lastIndexOf('[');
        if (bracketIndex < 0 || statementPrefix.indexOf(']', bracketIndex) >= 0) {
            return false;
        }
        String beforeBracket = statementPrefix.substring(0, bracketIndex).stripTrailing();
        // Walk backwards past quoted content if present (e.g. where "attribute[)
        int end = beforeBracket.length();
        if (end > 0 && (beforeBracket.charAt(end - 1) == '"' || beforeBracket.charAt(end - 1) == '\'')) {
            end--;
        }
        // Find the last keyword-like word before the bracket
        int wordStart = end;
        while (wordStart > 0 && isIdentifierPart(beforeBracket.charAt(wordStart - 1))) {
            wordStart--;
        }
        String word = beforeBracket.substring(wordStart, end);
        return "attribute".equalsIgnoreCase(word) || "attr".equalsIgnoreCase(word);
    }

    private @NotNull CompletionContext resolveWhereExpressionContext(@NotNull String statementPrefix) {
        String whereExpressionText = getOpenWhereExpressionTail(statementPrefix);
        if (whereExpressionText == null) {
            return CompletionContext.DEFAULT;
        }
        return CompletionContext.WHERE_EXPRESSION;
    }

    /**
     * Returns the content inside unclosed quotes after {@code where},
     * or {@code null} if the cursor is not inside a WHERE quoted value.
     */
    private @Nullable String getOpenWhereExpressionTail(@NotNull String statementPrefix) {
        String lower = statementPrefix.toLowerCase(Locale.ROOT);
        int whereIndex = lastIndexOfKeyword(lower, "where");
        if (whereIndex < 0) {
            return null;
        }

        String afterWhere = statementPrefix.substring(whereIndex + "where".length()).stripLeading();
        if (afterWhere.isEmpty()) {
            return null;
        }

        char firstChar = afterWhere.charAt(0);
        if (firstChar != '"' && firstChar != '\'') {
            return null;
        }

        boolean escaped = false;
        for (int i = 1; i < afterWhere.length(); i++) {
            char ch = afterWhere.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == firstChar) {
                return null; // Quote is closed
            }
        }
        return afterWhere.substring(1); // Unclosed quote — inside a WHERE value
    }

    private int lastIndexOfKeyword(@NotNull String lowerText, @NotNull String keyword) {
        int fromIndex = lowerText.length();
        while (fromIndex > 0) {
            int keywordIndex = lowerText.lastIndexOf(keyword, fromIndex - 1);
            if (keywordIndex < 0) {
                return -1;
            }
            int beforeIndex = keywordIndex - 1;
            int afterIndex = keywordIndex + keyword.length();
            boolean beforeBoundary = beforeIndex < 0 || !isIdentifierPart(lowerText.charAt(beforeIndex));
            boolean afterBoundary = afterIndex >= lowerText.length() || !isIdentifierPart(lowerText.charAt(afterIndex));
            if (beforeBoundary && afterBoundary) {
                return keywordIndex;
            }
            fromIndex = keywordIndex;
        }
        return -1;
    }

    private @NotNull String getCurrentStatementPrefix(@NotNull String beforeCaret) {
        int lineBreakIndex = Math.max(beforeCaret.lastIndexOf('\n'), beforeCaret.lastIndexOf('\r'));
        int statementIndex = beforeCaret.lastIndexOf(';');
        int startIndex = Math.max(lineBreakIndex, statementIndex);
        return beforeCaret.substring(startIndex + 1);
    }

    private @NotNull String getPreviousWord(@NotNull String beforeCaret) {
        int index = beforeCaret.length() - 1;

        // Skip identifier-part chars under the cursor
        while (index >= 0 && isIdentifierPart(beforeCaret.charAt(index))) {
            index--;
        }
        // Skip whitespace
        while (index >= 0 && Character.isWhitespace(beforeCaret.charAt(index))) {
            index--;
        }
        // Skip non-identifier, non-whitespace chars such as [ ] " '
        while (index >= 0 && !isIdentifierPart(beforeCaret.charAt(index))
                && !Character.isWhitespace(beforeCaret.charAt(index))) {
            index--;
        }
        if (index < 0) {
            return "";
        }

        int end = index;
        while (index >= 0 && isIdentifierPart(beforeCaret.charAt(index))) {
            index--;
        }
        return beforeCaret.substring(index + 1, end + 1);
    }

    private boolean isTempQueryBusTypeContext(@NotNull String statementPrefix) {
        String normalized = statementPrefix.replace('\t', ' ');
        String lower = normalized.toLowerCase();
        int queryBusIndex = lower.lastIndexOf("temp query bus");
        if (queryBusIndex < 0) {
            return false;
        }

        String tail = normalized.substring(queryBusIndex + "temp query bus".length());
        if (tail.isBlank()) {
            return true;
        }

        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean segmentHasContent = false;
        for (int i = 0; i < tail.length(); i++) {
            char ch = tail.charAt(i);
            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                segmentHasContent = true;
                continue;
            }
            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                segmentHasContent = true;
                continue;
            }
            if (inDoubleQuotes || inSingleQuotes) {
                segmentHasContent = true;
                continue;
            }
            if (ch == ',') {
                segmentHasContent = false;
                continue;
            }
            if (Character.isWhitespace(ch)) {
                if (!segmentHasContent) {
                    continue;
                }
                return tail.substring(i).trim().isEmpty();
            }
            if (isQueryBusTypeChar(ch)) {
                segmentHasContent = true;
                continue;
            }
            if (ch == '*') {
                segmentHasContent = true;
                continue;
            }
            if (ch == '.') {
                segmentHasContent = true;
                continue;
            }
            if (ch == '-') {
                segmentHasContent = true;
                continue;
            }
            if (ch == '_') {
                segmentHasContent = true;
                continue;
            }
            if (!Character.isLetterOrDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTempQueryBusTypeInQuotesContext(@NotNull String statementPrefix) {
        String normalized = statementPrefix.replace('\t', ' ');
        String lower = normalized.toLowerCase();
        int queryBusIndex = lower.lastIndexOf("temp query bus");
        if (queryBusIndex < 0) {
            return false;
        }

        String tail = normalized.substring(queryBusIndex + "temp query bus".length());
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        for (int i = 0; i < tail.length(); i++) {
            char ch = tail.charAt(i);
            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            }
        }
        return inDoubleQuotes || inSingleQuotes;
    }

    private boolean isQueryConnectionRelTargetContext(@NotNull String statementPrefix) {
        String lower = statementPrefix.toLowerCase();
        int relIndex = lower.lastIndexOf("query connection rel");
        if (relIndex < 0) {
            return false;
        }

        String tail = statementPrefix.substring(relIndex + "query connection rel".length()).stripLeading();
        if (tail.isEmpty()) {
            return false;
        }

        char quoteChar = tail.charAt(0);
        if (quoteChar != '"' && quoteChar != '\'') {
            return false;
        }

        boolean escaped = false;
        StringBuilder currentToken = new StringBuilder();
        boolean hasComma = false;
        for (int i = 1; i < tail.length(); i++) {
            char ch = tail.charAt(i);
            if (escaped) {
                currentToken.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                currentToken.append(ch);
                continue;
            }
            if (ch == quoteChar) {
                return false;
            }
            if (ch == ',') {
                hasComma = true;
                currentToken.setLength(0);
                continue;
            }
            currentToken.append(ch);
        }

        if (!hasComma) {
            return false;
        }
        return true;
    }

    private boolean isQuotedKeywordValueContext(@NotNull String statementPrefix, @NotNull String... keywords) {
        String lower = statementPrefix.toLowerCase();
        for (String keyword : keywords) {
            int keywordIndex = lower.lastIndexOf(keyword.toLowerCase());
            if (keywordIndex < 0) {
                continue;
            }
            int beforeIndex = keywordIndex - 1;
            if (beforeIndex >= 0 && isIdentifierPart(lower.charAt(beforeIndex))) {
                continue;
            }
            String tail = statementPrefix.substring(keywordIndex + keyword.length()).stripLeading();
            if (tail.isEmpty()) {
                continue;
            }
            char quoteChar = tail.charAt(0);
            if (quoteChar != '"' && quoteChar != '\'') {
                continue;
            }
            if (tail.indexOf(quoteChar, 1) < 0) {
                return true;
            }
        }
        return false;
    }

    private @NotNull String quoteIfNeeded(@NotNull String instance, boolean quoteIfNeeded) {
        if (!quoteIfNeeded || !instance.chars().anyMatch(Character::isWhitespace)) {
            return instance;
        }
        if ((instance.startsWith("\"") && instance.endsWith("\""))
                || (instance.startsWith("'") && instance.endsWith("'"))) {
            return instance;
        }
        return "\"" + instance.replace("\"", "\\\"") + "\"";
    }
    private boolean isQueryBusTypeChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.' || ch == '*';
    }

    private boolean isIdentifierPart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private @Nullable QuotedQueryBusContinuation findQuotedQueryBusContinuation(@NotNull Document document, int startOffset) {
        String beforeCaret = document.getText(new TextRange(0, startOffset));
        String statementPrefix = getCurrentStatementPrefix(beforeCaret);
        if (isTempQueryBusTypeInQuotesContext(statementPrefix)) {
            return null;
        }

        String normalized = statementPrefix.replace('\t', ' ');
        String lower = normalized.toLowerCase();
        int queryBusIndex = lower.lastIndexOf("temp query bus");
        if (queryBusIndex < 0) {
            return null;
        }

        String tail = normalized.substring(queryBusIndex + "temp query bus".length());
        String trimmedTail = tail.stripLeading();
        if (trimmedTail.isEmpty()) {
            return null;
        }

        char quoteChar = trimmedTail.charAt(0);
        if (quoteChar != '"' && quoteChar != '\'') {
            return null;
        }

        int lastCommaIndex = tail.lastIndexOf(',');
        if (lastCommaIndex < 0) {
            return null;
        }

        int closingQuoteInTail = previousNonWhitespaceIndex(tail, lastCommaIndex - 1);
        if (closingQuoteInTail < 0 || tail.charAt(closingQuoteInTail) != quoteChar) {
            return null;
        }

        int statementStartOffset = startOffset - statementPrefix.length();
        int tailStartOffset = statementStartOffset + queryBusIndex + "temp query bus".length();
        return new QuotedQueryBusContinuation(tailStartOffset + closingQuoteInTail, quoteChar);
    }

    private int previousNonWhitespaceIndex(@NotNull String text, int fromIndex) {
        for (int i = fromIndex; i >= 0; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasCharAt(@NotNull Document document, int offset, char expected) {
        return offset >= 0
                && offset < document.getTextLength()
                && document.getCharsSequence().charAt(offset) == expected;
    }

    private @NotNull String escapeForQuotedList(@NotNull String instance, char quoteChar) {
        return instance.replace(String.valueOf(quoteChar), "\\" + quoteChar);
    }
    private @Nullable Character findWhereOpenQuote(@NotNull Document document, int beforeOffset) {
        String beforeCaret = document.getText(new TextRange(0, beforeOffset));
        String statementPrefix = getCurrentStatementPrefix(beforeCaret);
        String lower = statementPrefix.toLowerCase(Locale.ROOT);
        int whereIndex = lastIndexOfKeyword(lower, "where");
        if (whereIndex < 0) {
            return null;
        }
        String afterWhere = statementPrefix.substring(whereIndex + "where".length()).stripLeading();
        if (afterWhere.isEmpty()) {
            return null;
        }
        if (afterWhere.charAt(0) == '"' || afterWhere.charAt(0) == '\'') {
            char quoteChar = afterWhere.charAt(0);
            boolean escaped = false;
            for (int i = 1; i < afterWhere.length(); i++) {
                char ch = afterWhere.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == quoteChar) {
                    return null;
                }
            }
            return quoteChar;
        }
        return null;
    }

    private static final class KeywordInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String keyword;

        private KeywordInsertHandler(@NotNull String keyword) {
            this.keyword = keyword;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int tailOffset = context.getTailOffset();
            String lowerKeyword = keyword.toLowerCase();
            String suffix = switch (lowerKeyword) {
                case "temp" -> " query bus ";
                case "where" -> " \"\"";
                default -> " ";
            };
            if (!matchesSuffix(document, tailOffset, suffix)) {
                document.insertString(tailOffset, suffix);
            }
            int caretOffset = switch (lowerKeyword) {
                case "where" -> tailOffset + 2;
                default -> tailOffset + suffix.length();
            };
            context.getEditor().getCaretModel().moveToOffset(caretOffset);
        }

        private boolean matchesSuffix(@NotNull Document document, int offset, @NotNull String suffix) {
            int availableLength = document.getTextLength() - offset;
            if (availableLength < suffix.length()) {
                return false;
            }
            return suffix.contentEquals(document.getCharsSequence().subSequence(offset, offset + suffix.length()));
        }
    }

    private boolean isQuotedKeywordListContinuationContext(@NotNull String statementPrefix, @NotNull String... keywords) {
        return findQuotedKeywordContinuation(statementPrefix, keywords) != null;
    }

    private @Nullable QuotedQueryBusContinuation findQuotedKeywordContinuation(@NotNull String statementPrefix,
                                                                               @NotNull String... keywords) {
        String normalized = statementPrefix.replace('\t', ' ');
        String lower = normalized.toLowerCase();
        int keywordIndex = -1;
        String matchedKeyword = null;
        for (String keyword : keywords) {
            int candidateIndex = lower.lastIndexOf(keyword.toLowerCase());
            if (candidateIndex < 0) {
                continue;
            }
            int beforeIndex = candidateIndex - 1;
            if (beforeIndex >= 0 && isIdentifierPart(lower.charAt(beforeIndex))) {
                continue;
            }
            if (candidateIndex > keywordIndex) {
                keywordIndex = candidateIndex;
                matchedKeyword = keyword;
            }
        }
        if (keywordIndex < 0 || matchedKeyword == null) {
            return null;
        }

        String tail = normalized.substring(keywordIndex + matchedKeyword.length());
        String trimmedTail = tail.stripLeading();
        if (trimmedTail.isEmpty()) {
            return null;
        }

        char quoteChar = trimmedTail.charAt(0);
        if (quoteChar != '"' && quoteChar != '\'') {
            return null;
        }

        int lastCommaIndex = tail.lastIndexOf(',');
        if (lastCommaIndex < 0) {
            return null;
        }

        int closingQuoteInTail = previousNonWhitespaceIndex(tail, lastCommaIndex - 1);
        if (closingQuoteInTail < 0 || tail.charAt(closingQuoteInTail) != quoteChar) {
            return null;
        }
        return new QuotedQueryBusContinuation(closingQuoteInTail, quoteChar);
    }

    private @Nullable QuotedQueryBusContinuation findQuotedKeywordContinuation(@NotNull Document document,
                                                                               int startOffset,
                                                                               @NotNull String... keywords) {
        String beforeCaret = document.getText(new TextRange(0, startOffset));
        String statementPrefix = getCurrentStatementPrefix(beforeCaret);
        QuotedQueryBusContinuation continuation = findQuotedKeywordContinuation(statementPrefix, keywords);
        if (continuation == null) {
            return null;
        }

        int statementStartOffset = startOffset - statementPrefix.length();
        int keywordIndex = -1;
        String matchedKeyword = null;
        String lower = statementPrefix.toLowerCase();
        for (String keyword : keywords) {
            int candidateIndex = lower.lastIndexOf(keyword.toLowerCase());
            if (candidateIndex < 0) {
                continue;
            }
            int beforeIndex = candidateIndex - 1;
            if (beforeIndex >= 0 && isIdentifierPart(lower.charAt(beforeIndex))) {
                continue;
            }
            if (candidateIndex > keywordIndex) {
                keywordIndex = candidateIndex;
                matchedKeyword = keyword;
            }
        }
        if (keywordIndex < 0 || matchedKeyword == null) {
            return null;
        }

        int tailStartOffset = statementStartOffset + keywordIndex + matchedKeyword.length();
        return new QuotedQueryBusContinuation(tailStartOffset + continuation.closingQuoteOffset(), continuation.quoteChar());
    }

    private final class QueryBusTypeInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String instance;
        private final boolean insideQuotedList;

        private QueryBusTypeInsertHandler(@NotNull String instance, boolean insideQuotedList) {
            this.instance = instance;
            this.insideQuotedList = insideQuotedList;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            QuotedQueryBusContinuation continuation = insideQuotedList ? null : findQuotedQueryBusContinuation(document, startOffset);

            String insertText;
            if (continuation != null) {
                document.deleteString(continuation.closingQuoteOffset(), continuation.closingQuoteOffset() + 1);
                startOffset--;
                tailOffset--;
                insertText = escapeForQuotedList(instance, continuation.quoteChar());
            } else if (insideQuotedList) {
                insertText = escapeForQuotedList(instance, '"');
            } else {
                insertText = quoteIfNeeded(instance, true);
            }

            document.replaceString(startOffset, tailOffset, insertText);
            int newTailOffset = startOffset + insertText.length();

            if (continuation != null && !hasCharAt(document, newTailOffset, continuation.quoteChar())) {
                document.insertString(newTailOffset, String.valueOf(continuation.quoteChar()));
                newTailOffset++;
            }

            context.setTailOffset(newTailOffset);
            context.getEditor().getCaretModel().moveToOffset(newTailOffset);
        }
    }

    private final class RelationshipInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String instance;
        private final boolean quoteIfNeeded;
        private final boolean insideQuotedList;

        private RelationshipInsertHandler(@NotNull String instance, boolean quoteIfNeeded, boolean insideQuotedList) {
            this.instance = instance;
            this.quoteIfNeeded = quoteIfNeeded;
            this.insideQuotedList = insideQuotedList;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            QuotedQueryBusContinuation continuation = insideQuotedList
                    ? null
                    : findQuotedKeywordContinuation(document, startOffset, "rel", "relationship");

            String insertText;
            if (continuation != null) {
                document.deleteString(continuation.closingQuoteOffset(), continuation.closingQuoteOffset() + 1);
                startOffset--;
                tailOffset--;
                insertText = escapeForQuotedList(instance, continuation.quoteChar());
            } else if (insideQuotedList) {
                insertText = escapeForQuotedList(instance, '"');
            } else {
                insertText = quoteIfNeeded(instance, quoteIfNeeded);
            }

            document.replaceString(startOffset, tailOffset, insertText);
            int newTailOffset = startOffset + insertText.length();

            if (continuation != null && !hasCharAt(document, newTailOffset, continuation.quoteChar())) {
                document.insertString(newTailOffset, String.valueOf(continuation.quoteChar()));
                newTailOffset++;
            }

            context.setTailOffset(newTailOffset);
            context.getEditor().getCaretModel().moveToOffset(newTailOffset);
        }
    }

    private final class AdminInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String instance;
        private final boolean quoteIfNeeded;
        private final boolean insideQuotedList;
        private final String[] keywords;

        private AdminInsertHandler(@NotNull String instance,
                                   boolean quoteIfNeeded,
                                   boolean insideQuotedList,
                                   @NotNull String... keywords) {
            this.instance = instance;
            this.quoteIfNeeded = quoteIfNeeded;
            this.insideQuotedList = insideQuotedList;
            this.keywords = keywords;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            QuotedQueryBusContinuation continuation = insideQuotedList
                    ? null
                    : findQuotedKeywordContinuation(document, startOffset, keywords);

            String insertText;
            if (continuation != null) {
                document.deleteString(continuation.closingQuoteOffset(), continuation.closingQuoteOffset() + 1);
                startOffset--;
                tailOffset--;
                insertText = escapeForQuotedList(instance, continuation.quoteChar());
            } else if (insideQuotedList) {
                insertText = escapeForQuotedList(instance, '"');
            } else {
                insertText = quoteIfNeeded(instance, quoteIfNeeded);
            }

            document.replaceString(startOffset, tailOffset, insertText);
            int newTailOffset = startOffset + insertText.length();

            if (continuation != null && !hasCharAt(document, newTailOffset, continuation.quoteChar())) {
                document.insertString(newTailOffset, String.valueOf(continuation.quoteChar()));
                newTailOffset++;
            }

            context.setTailOffset(newTailOffset);
            context.getEditor().getCaretModel().moveToOffset(newTailOffset);
        }
    }

    private final class TypeInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String instance;
        private final boolean quoteIfNeeded;
        private final boolean insideQuotedList;

        private TypeInsertHandler(@NotNull String instance, boolean quoteIfNeeded, boolean insideQuotedList) {
            this.instance = instance;
            this.quoteIfNeeded = quoteIfNeeded;
            this.insideQuotedList = insideQuotedList;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            QuotedQueryBusContinuation continuation = insideQuotedList
                    ? null
                    : findQuotedKeywordContinuation(document, startOffset, "type");

            String insertText;
            if (continuation != null) {
                document.deleteString(continuation.closingQuoteOffset(), continuation.closingQuoteOffset() + 1);
                startOffset--;
                tailOffset--;
                insertText = escapeForQuotedList(instance, continuation.quoteChar());
            } else if (insideQuotedList) {
                insertText = escapeForQuotedList(instance, '"');
            } else {
                insertText = quoteIfNeeded(instance, quoteIfNeeded);
            }

            document.replaceString(startOffset, tailOffset, insertText);
            int newTailOffset = startOffset + insertText.length();

            if (continuation != null && !hasCharAt(document, newTailOffset, continuation.quoteChar())) {
                document.insertString(newTailOffset, String.valueOf(continuation.quoteChar()));
                newTailOffset++;
            }

            context.setTailOffset(newTailOffset);
            context.getEditor().getCaretModel().moveToOffset(newTailOffset);
        }
    }

    private final class AttributeSelectorInsertHandler implements InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
        private final String instance;

        private AttributeSelectorInsertHandler(@NotNull String instance) {
            this.instance = instance;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context,
                                 @NotNull com.intellij.codeInsight.lookup.LookupElement item) {
            Document document = context.getDocument();
            int startOffset = context.getStartOffset();
            int tailOffset = context.getTailOffset();
            Character quoteChar = findWhereOpenQuote(document, startOffset);
            String insertText = quoteChar != null ? escapeForQuotedList(instance, quoteChar) : instance;
            document.replaceString(startOffset, tailOffset, insertText);
            int newTailOffset = startOffset + insertText.length();
            if (!hasCharAt(document, newTailOffset, ']')) {
                document.insertString(newTailOffset, "]");
                newTailOffset++;
            }
            if (quoteChar != null && !hasCharAt(document, newTailOffset, quoteChar)) {
                document.insertString(newTailOffset, String.valueOf(quoteChar));
                newTailOffset++;
            }
            context.setTailOffset(newTailOffset);
            context.getEditor().getCaretModel().moveToOffset(newTailOffset);
        }
    }

    private record QuotedQueryBusContinuation(int closingQuoteOffset, char quoteChar) {
    }
}
