package cn.github.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class ConnectionsRow {
    private String type;
    private String id;
    private boolean path;
    private String physicalId;
    private String fromType;
    private String fromName;
    private String fromRevision;
    private String fromId;
    private String fromRelType;
    private String fromRelId;
    private String toType;
    private String toName;
    private String toRevision;
    private String toId;
    private String toRelType;
    private String toRelId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionsRow row = (ConnectionsRow) o;
        return id.equals(row.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
