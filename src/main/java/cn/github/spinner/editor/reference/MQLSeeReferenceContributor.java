package cn.github.spinner.editor.reference;

import cn.github.spinner.editor.highlights.MQLTokenTypes;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 为 MQL 文档注释中的 {@code @see} 引用提供 Java 源码导航。
 *
 * <p>支持 {@code Foo.java}、{@code Foo.java#method} 和
 * {@code com.example.Foo#method} 三种常用写法。</p>
 */
public final class MQLSeeReferenceContributor extends PsiReferenceContributor {
    private static final Pattern SEE_REFERENCE = Pattern.compile(
            "@see\\s+(?:<([^>]+)>|([^\\s*]+))");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(MQLTokenTypes.COMMENT),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element,
                            @NotNull ProcessingContext context) {
                        // 一个文档注释可能包含多个 @see，每个目标分别创建一个可导航引用。
                        List<PsiReference> references = new ArrayList<>();
                        Matcher matcher = SEE_REFERENCE.matcher(element.getText());
                        while (matcher.find()) {
                            String target = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                            int targetStart = matcher.start(1) >= 0 ? matcher.start(1) : matcher.start(2);
                            references.add(new MQLSeeReference(element, targetStart,
                                    targetStart + target.length(), target));
                        }
                        return references.toArray(PsiReference.EMPTY_ARRAY);
                    }
                });
    }

    private static final class MQLSeeReference extends PsiReferenceBase<PsiElement> {
        private final String target;

        private MQLSeeReference(@NotNull PsiElement element, int start, int end, @NotNull String target) {
            super(element, new com.intellij.openapi.util.TextRange(start, end));
            this.target = target;
        }

        @Override
        public @Nullable PsiElement resolve() {
            // 先按“文件/类”定位，再按 # 后面的名称定位方法。
            Project project = getElement().getProject();
            String[] parts = target.split("#", 2);
            String owner = parts[0];
            String methodName = parts.length == 2 ? parts[1] : null;

            PsiClass psiClass = owner.endsWith(".java")
                    ? findClassInJavaFile(project, owner)
                    : findClass(project, owner);
            if (psiClass == null) {
                return null;
            }
            if (methodName == null || methodName.isBlank()) {
                return psiClass.getContainingFile();
            }
            PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
            return methods.length == 0 ? null : methods[0];
        }

        private static @Nullable PsiClass findClassInJavaFile(@NotNull Project project, @NotNull String owner) {
            // 文件名引用允许携带相对路径，用路径后缀过滤同名 Java 文件。
            String fileName = owner.substring(owner.lastIndexOf('/') + 1);
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName(project, fileName, scope)) {
                if (!matchesPath(virtualFile, owner)) {
                    continue;
                }
                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if (file instanceof PsiJavaFile javaFile) {
                    String className = fileName.substring(0, fileName.length() - ".java".length());
                    for (PsiClass psiClass : javaFile.getClasses()) {
                        if (className.equals(psiClass.getName())) {
                            return psiClass;
                        }
                    }
                    return javaFile.getClasses().length == 0 ? null : javaFile.getClasses()[0];
                }
            }
            return null;
        }

        private static boolean matchesPath(@NotNull VirtualFile file, @NotNull String owner) {
            String normalizedOwner = owner.replace('\\', '/');
            String normalizedPath = file.getPath().replace('\\', '/');
            return normalizedPath.endsWith("/" + normalizedOwner)
                    || file.getName().equals(owner);
        }

        private static @Nullable PsiClass findClass(@NotNull Project project, @NotNull String owner) {
            // JavaPsiFacade 负责处理项目索引中的全限定类名。
            String qualifiedName = owner.replace('/', '.');
            if (qualifiedName.endsWith(".java")) {
                qualifiedName = qualifiedName.substring(0, qualifiedName.length() - ".java".length());
            }
            return com.intellij.psi.JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedName, GlobalSearchScope.projectScope(project));
        }
    }
}
