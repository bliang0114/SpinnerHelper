package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class PropertiesRow implements TableRowBean {
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

    @Override
    public String[] headers() {
        return new String[]{"Name", "Value"};
    }

    @Override
    public int[] widths() {
        return new int[]{200, 200};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{name, value};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class};
    }
}
