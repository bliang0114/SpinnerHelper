package cn.github.spinner.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.Objects;

@Service(Service.Level.APP)
@State(name="SpinnerSettings", storages = @Storage("matrix-drivers-config.xml"))
public final class MatrixDriversConfig implements PersistentStateComponent<MatrixDriversConfig> {
    @Setter
    private Map<String, DriverInfo> driversMap;
    private static final List<String> DEFAULT_DRIVER = List.of("R2021x", "R2022x", "R2023x", "R2024x", "R2025x");

    public static MatrixDriversConfig getInstance(){
        return ApplicationManager.getApplication().getService(MatrixDriversConfig.class);
    }

    @Override
    public @NotNull MatrixDriversConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MatrixDriversConfig matrixDriversConfig) {
        XmlSerializerUtil.copyBean(matrixDriversConfig, this);
    }

    public Map<String, DriverInfo> getDriversMap() {
        if (driversMap == null){
            driversMap = new LinkedHashMap<>();
        }
        for (String driver : DEFAULT_DRIVER) {
            driversMap.computeIfAbsent(driver, key -> new DriverInfo());
        }
        return driversMap;
    }

    public List<File> getDriverFiles(String driverName) {
        DriverInfo driverInfo = putDriver(driverName);
        List<DriverFile> driverFiles = Optional.ofNullable(driverInfo).map(DriverInfo::getDriverFiles).orElse(new ArrayList<>());
        return driverFiles.stream()
                .map(DriverFile::getPath)
                .filter(Objects::nonNull)
                .map(File::new)
                .toList();
    }

    public void putDriver(String driverName, DriverInfo driverInfo) {
        getDriversMap().put(driverName, driverInfo == null ? new DriverInfo() : driverInfo);
    }

    public DriverInfo putDriver(String driverName) {
        return getDriversMap().computeIfAbsent(driverName, key -> new DriverInfo());
    }

    public void removeDriver(String driverName) {
        getDriversMap().remove(driverName);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverInfo {
        private String driverClass;
        private List<DriverFile> driverFiles;

        public List<DriverFile> getDriverFiles() {
            if  (driverFiles == null) {
                driverFiles = new ArrayList<>();
            }
            return driverFiles;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverFile {
        private String name;
        private String path;
    }
}
