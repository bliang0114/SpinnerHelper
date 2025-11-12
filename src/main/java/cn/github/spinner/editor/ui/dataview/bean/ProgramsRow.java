package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProgramsRow implements TableRowBean {
    private String name;
    private String createTime;
    private String updateTime;

    public ProgramsRow(String name, String createTime, String updateTime) {
        this.name = name;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    @Override
    public String[] headers() {
        return new String[]{"Name", "CreateTime", "UpdateTime"};
    }

    @Override
    public int[] widths() {
        return new int[]{300, 180, 180};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{name, createTime, updateTime};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class};
    }
}
