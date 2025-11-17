package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;

import java.util.Objects;

@Data
public class ConnectionsRow implements TableRowBean {
    private String type;
    private String id;
    private boolean path;
    private String physicalId;
    private String fromType;
    private String fromName;
    private String fromRevision;
    private String fromId;
    private String toType;
    private String toName;
    private String toRevision;
    private String toId;

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

    @Override
    public String[] headers() {
        return new String[]{"Type", "ID", "Path", "PhysicalID", "From Type", "From Name", "From Revision", "From Id", "To Type", "To Name", "To Revision", "To Id"};
    }

    @Override
    public int[] widths() {
        return new int[]{200, 200, 100, 200, 200, 200, 200, 200, 200, 200, 200, 200};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{type, id, path, physicalId, fromType, fromName, fromRevision, fromId, toType, toName, toRevision, toId};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, Boolean.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class};
    }
}
