package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class InterfacesRow implements TableRowBean {
    private String interfaceName;
    private String attributeName;
    private String attributeOwner;
    private String type;
    private String defaultValue;
    private String range;

    public InterfacesRow(String interfaceName, String attributeName, String attributeOwner) {
        this.interfaceName = interfaceName;
        this.attributeName = attributeName;
        this.attributeOwner = attributeOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfacesRow row = (InterfacesRow) o;
        return Objects.equals(attributeName, row.attributeName) && Objects.equals(attributeOwner, row.attributeOwner) && interfaceName.equals(row.interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, attributeName, attributeOwner);
    }

    @Override
    public String[] headers() {
        return new String[]{"Interface Name", "Attribute Name", "Attribute Owner", "Type", "Default", "Range"};
    }

    @Override
    public int[] widths() {
        return new int[]{260, 260, 260, 150, 150, 500};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{interfaceName, attributeName, attributeOwner, type, defaultValue, range};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class, String.class};
    }
}
