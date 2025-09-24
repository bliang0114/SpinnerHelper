package com.bol.spinner.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@State(name="SpinnerSettings", storages = @Storage("spinnerSettingsConfig.xml"))
public final class SpinnerSettings implements PersistentStateComponent<SpinnerSettings> {
    @Setter
    private List<EnvironmentConfig> environments;

    public static SpinnerSettings getInstance(Project project){
        return project.getService(SpinnerSettings.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dependency {
        private String name;
        private String path;
    }

    @Override
    public @NotNull SpinnerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SpinnerSettings spinnerSettings) {
        XmlSerializerUtil.copyBean(spinnerSettings, this);
    }

    public List<EnvironmentConfig> getEnvironments() {
        if  (environments == null) {
            environments = new ArrayList<>();
        }
        return environments;
    }

    public Optional<EnvironmentConfig> getEnvironment(String name) {
        return getEnvironments().stream().filter(env -> env.getName().equals(name)).findFirst();
    }
}
