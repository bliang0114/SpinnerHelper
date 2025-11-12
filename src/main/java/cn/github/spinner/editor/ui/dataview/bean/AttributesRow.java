package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class AttributesRow implements TableRowBean {
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

    @Override
    public String[] headers() {
        return new String[]{"Name", "Owner", "Type", "Default", "Range"};
    }

    @Override
    public int[] widths() {
        return new int[]{360, 360, 180, 180, 600};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{name, owner, type, defaultValue, range};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class};
    }
}
