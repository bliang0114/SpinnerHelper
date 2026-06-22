package cn.github.spinner.util;

import lombok.extern.slf4j.Slf4j;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MatrixJarLoadManager {
    private static final ConcurrentHashMap<Project, ConcurrentHashMap<String, MatrixJarClassLoader>> projectClassLoaders =
            new ConcurrentHashMap<>();

    /**
     * 加载驱动包
     *
     * @param environment 环境名称
     * @param jarFiles    驱动包
     * @param parent      父类加载器
     * @return {@link ClassLoader}
     * @author zaydenwang
     */
    public static ClassLoader loadMatrixJars(@NotNull Project project,
                                             String environment,
                                             List<File> jarFiles,
                                             ClassLoader parent) {
        ConcurrentHashMap<String, MatrixJarClassLoader> environmentClassLoaders =
                projectClassLoaders.computeIfAbsent(project, ignored -> new ConcurrentHashMap<>());
        MatrixJarClassLoader classLoader = environmentClassLoaders.get(environment);
        // 移除现有的类加载器
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        // 创建新的类加载器
        classLoader = new MatrixJarClassLoader(jarFiles, parent);
        environmentClassLoaders.put(environment, classLoader);
        return classLoader;
    }

    public static void closeEnvironment(@NotNull Project project, String environment) {
        ConcurrentHashMap<String, MatrixJarClassLoader> environmentClassLoaders = projectClassLoaders.get(project);
        if (environmentClassLoaders == null) {
            return;
        }
        closeClassLoader(environment, environmentClassLoaders.remove(environment));
        if (environmentClassLoaders.isEmpty()) {
            projectClassLoaders.remove(project, environmentClassLoaders);
        }
    }

    public static void closeProject(@NotNull Project project) {
        ConcurrentHashMap<String, MatrixJarClassLoader> environmentClassLoaders = projectClassLoaders.remove(project);
        if (environmentClassLoaders == null) {
            return;
        }
        environmentClassLoaders.forEach(MatrixJarLoadManager::closeClassLoader);
        environmentClassLoaders.clear();
    }

    public static void closeAll() {
        projectClassLoaders.forEach((project, environmentClassLoaders) ->
                environmentClassLoaders.forEach(MatrixJarLoadManager::closeClassLoader));
        projectClassLoaders.clear();
    }

    private static void closeClassLoader(String environment, MatrixJarClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException e) {
            log.error("Close matrix jar classloader failed: {}", environment, e);
        }
    }
}
