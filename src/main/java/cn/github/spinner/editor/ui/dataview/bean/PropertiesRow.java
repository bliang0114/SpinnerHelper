package cn.github.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class PropertiesRow {
    private String name;
    private String value;

    public PropertiesRow(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertiesRow row = (PropertiesRow) o;
        return name.equals(row.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
