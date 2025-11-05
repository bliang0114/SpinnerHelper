package cn.github.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class ObjectsRow {
    private String type;
    private String name;
    private String revision;
    private String id;
    private boolean path;
    private String physicalId;
    private String description;
    private String originated;
    private String modified;
    private String vault;
    private String policy;
    private String owner;
    private String state;
    private String organization;
    private String collaborativeSpace;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectsRow row = (ObjectsRow) o;
        return id.equals(row.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
