package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class PolicyTriggersRow implements TableRowBean {
    private String policy;
    private String state;
    private String triggerName;
    private String check;
    private String override;
    private String action;

    public PolicyTriggersRow(String policy, String state) {
        this.policy = policy;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyTriggersRow row = (PolicyTriggersRow) o;
        return Objects.equals(policy, row.policy)
                && Objects.equals(state, row.state)
                && Objects.equals(triggerName, row.triggerName)
                && Objects.equals(check, row.check)
                && Objects.equals(override, row.override)
                && Objects.equals(action, row.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policy, state, triggerName, check, override, action);
    }

    @Override
    public String[] headers() {
        return new String[]{"Policy", "State", "Trigger Name", "Check", "Override", "Action"};
    }

    @Override
    public int[] widths() {
        return new int[]{200, 100, 100, 300, 300, 300};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{policy, state, triggerName, check, override, action};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class, String.class};
    }
}
