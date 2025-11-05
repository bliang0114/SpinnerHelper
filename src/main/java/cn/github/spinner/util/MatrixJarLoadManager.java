package cn.github.spinner.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MatrixJarLoadManager {
    private static final ConcurrentHashMap<String, MatrixJarClassLoader> environmentClassLoaders = new ConcurrentHashMap<>();

    /**
     * 加载驱动包
     *
     * @param environment 环境名称
     * @param jarFiles    驱动包
     * @param parent      父类加载器
     * @return {@link ClassLoader}
     * @author zaydenwang
     */
    public static ClassLoader loadMatrixJars(String environment, List<File> jarFiles, ClassLoader parent) {
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
}
