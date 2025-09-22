package com.bol.spinner.util;


import com.intellij.openapi.diagnostic.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MatrixJarLoadManager {
    private static final Logger LOGGER = Logger.getInstance(MatrixJarClassLoader.class);
    private static MatrixJarClassLoader matrixJarClassLoader;

    public static void loadMatrixJars(List<File> jarFiles, ClassLoader parent) {
        // 移除现有的类加载器
        if (matrixJarClassLoader != null) {
            try {
                matrixJarClassLoader.close();
            } catch (IOException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }

        // 创建新的类加载器
        matrixJarClassLoader = new MatrixJarClassLoader(jarFiles, parent);
    }
}
