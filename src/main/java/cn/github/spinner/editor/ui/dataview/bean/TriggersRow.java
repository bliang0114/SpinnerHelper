package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;

import java.util.Objects;

@Data
public class TriggersRow implements TableRowBean {
    private String triggerName;
    private boolean inherited;
    private String check;
    private String override;
    private String action;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggersRow row = (TriggersRow) o;
        return triggerName.equals(row.triggerName) && Objects.equals(check, row.check) && Objects.equals(override, row.override) && Objects.equals(action, row.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggerName, check, override, action);
    }

    @Override
    public String[] headers() {
        return new String[]{"Trigger Name", "Inherited", "Check", "Override", "Action"};
    }

    @Override
    public int[] widths() {
        return new int[]{200, 100, 300, 300, 300};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{triggerName, inherited, check, override, action};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, Boolean.class, String.class, String.class, String.class};
    }
}
