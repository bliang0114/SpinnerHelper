package com.bol.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class TriggersRow {
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
}
