package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdminDefinitionRow implements TableRowBean {
    private String type;
    private String name;

    public AdminDefinitionRow(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String[] headers() {
        return new String[]{"Type", "Name"};
    }

    @Override
    public int[] widths() {
        return new int[]{160, 520};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{type, name};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class};
    }
}
