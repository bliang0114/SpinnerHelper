package cn.github.driver;

import cn.github.driver.connection.MatrixConnection;
import lombok.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 3DExperience Matrix 驱动管理
 */
public class MatrixDriverManager {
    /**
     * 已注册的驱动
     */
    private static final CopyOnWriteArrayList<MatrixDriver> registeredDrivers = new CopyOnWriteArrayList<>();
    private static final Object lockForInitDrivers = new Object();
    private static volatile boolean driversInitialized;
    private static final String MATRIX_DRIVERS_PROPERTY = "matrix.drivers";

    /**
     * 注册 Matrix 驱动
     *
     * @param driver Matrix 驱动
     * @author zaydenwang
     */
    public static void registerDriver(MatrixDriver driver) {
        if (driver != null) {
            registeredDrivers.addIfAbsent(driver);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * 确保驱动已经初始化
     *
     * @param classLoader 驱动所在的类加载器
     * @author zaydenwang
     */
    private static void ensureDriversInitialized(@NonNull ClassLoader classLoader) {
        if (driversInitialized) {
            return;
        }
        synchronized (lockForInitDrivers) {
            if (driversInitialized) {
                return;
            }
            String drivers = System.getProperty(MATRIX_DRIVERS_PROPERTY);
            if (drivers != null && !drivers.isEmpty()) {
                String[] driversList = drivers.split(":");
                for (String aDriver : driversList) {
                    try {
                        Class.forName(aDriver, true, classLoader);
                    } catch (Exception ignored) {
                    }
                }
            }
            driversInitialized = true;
        }
    }

    /**
     * 是否允许加载的Matrix 驱动
     *
     * @param driver      Matrix 驱动
     * @param classLoader 驱动所在的类加载器
     * @return {@link boolean}
     * @author zaydenwang
     */
    private static boolean isDriverAllowed(MatrixDriver driver, ClassLoader classLoader) {
        boolean result = false;
        if (driver != null) {
            Class<?> aClass = null;
            try {
                aClass = Class.forName(driver.getClass().getName(), true, classLoader);
            } catch (Exception ignored) {
            }
            result = aClass == driver.getClass();
        }
        return result;
    }

    /**
     * 获取 Matrix 连接
     *
     * @param url         数据源URL
     * @param username    用户名
     * @param password    密码
     * @param vault       Vault
     * @param classLoader 类加载器
     * @return {@link MatrixConnection}
     * @author zaydenwang
     */
    public static MatrixConnection getConnection(String url, String username, String password, String vault, ClassLoader classLoader) throws MQLException {
        return getConnection(url, username, password, vault, null, classLoader);
    }

    /**
     * 获取 Matrix 连接
     *
     * @param url         数据源URL
     * @param username    用户名
     * @param password    密码
     * @param vault       Vault
     * @param role        安全上下文
     * @param classLoader 类加载器
     * @return {@link MatrixConnection}
     * @author zaydenwang
     */
    public static MatrixConnection getConnection(String url, String username, String password, String vault, String role, ClassLoader classLoader) throws MQLException {
        if (url == null) {
            throw new MQLException("The url cannot be null", "08001");
        }

        ensureDriversInitialized(classLoader);

        MQLException reason = null;
        MatrixDriverProperty matrixDriverProperty = new MatrixDriverProperty(url, username, password, vault, role);
        for (MatrixDriver driver : registeredDrivers) {
            if (isDriverAllowed(driver, classLoader)) {
                try {
                    MatrixConnection con = driver.connect(matrixDriverProperty);
                    if (con != null) {
                        return con;
                    }
                } catch (MQLException ex) {
                    if (reason == null) {
                        reason = ex;
                    }
                }
            }
        }
        if (reason != null) {
            throw reason;
        }
        throw new MQLException("No suitable driver found for " + url, "08001");
    }
}
