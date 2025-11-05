package cn.github.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class AttributesRow {
    private String name;
    private String owner;
    private String type;
    private String defaultValue;
    private String range;

    public AttributesRow(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributesRow row = (AttributesRow) o;
        return Objects.equals(owner, row.owner) && name.equals(row.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, owner);
    }
}
