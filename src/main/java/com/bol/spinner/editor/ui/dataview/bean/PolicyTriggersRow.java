package com.bol.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class PolicyTriggersRow {
    private String policy;
    private String state;
    private String triggerName;
    private String check;
    private String override;
    private String action;

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
}
