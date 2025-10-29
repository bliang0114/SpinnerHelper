package com.bol.spinner.editor.completion;

import com.bol.spinner.editor.MQLLanguage;
import com.bol.spinner.editor.icons.MQLIcons;
import com.bol.spinner.editor.MQLKeywords;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class MQLCompletionContributor extends CompletionContributor {
    public MQLCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(MQLLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        // 创建大小写不敏感的结果集
                        CompletionResultSet caseInsensitiveResult = result.caseInsensitive();
                        // 添加关键字补全
                        for (String keyword : MQLKeywords.KEYWORDS) {
                            caseInsensitiveResult.addElement(LookupElementBuilder.create(keyword)
                                    .withTypeText("Keyword")
                                    .withBoldness(true));
                        }

                        // 添加类型补全
                        for (String type : MQLKeywords.TYPES) {
                            caseInsensitiveResult.addElement(LookupElementBuilder.create(type)
                                    .withTypeText("Type")
                                    .withIcon(MQLIcons.TYPE));
                        }

                        // 添加类型实例数据补全
                        for (String instance : MQLKeywords.TYPE_INSTANCES) {
                            caseInsensitiveResult.addElement(LookupElementBuilder.create(instance)
                                    .withTypeText("Type Definition")
                                    .withIcon(MQLIcons.TYPE));
                        }

                        // 添加关系实例数据补全
                        for (String instance : MQLKeywords.RELATIONSHIP_INSTANCES) {
                            caseInsensitiveResult.addElement(LookupElementBuilder.create(instance)
                                    .withTypeText("Relationship Definition")
                                    .withIcon(MQLIcons.TYPE));
                        }
                    }
                });
    }
}
