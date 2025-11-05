package cn.github.spinner.util;

import com.intellij.openapi.diagnostic.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class MatrixJarClassLoader extends URLClassLoader {
    private static final Logger LOGGER = Logger.getInstance(MatrixJarClassLoader.class);

    public MatrixJarClassLoader(List<File> jarFiles, ClassLoader parent) {
        super(new URL[0], parent);
        for (File jarFile : jarFiles) {
            try {
                addURL(jarFile.toURI().toURL());
            } catch (Exception e) {
                LOGGER.error("Error adding jar file " + jarFile.getAbsolutePath(), e);
            }
        }
    }
}
