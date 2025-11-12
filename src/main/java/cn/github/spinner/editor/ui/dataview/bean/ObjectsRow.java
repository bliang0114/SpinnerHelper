package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;

import java.util.Objects;

@Data
public class ObjectsRow implements TableRowBean {
    private String type;
    private String name;
    private String revision;
    private String id;
    private boolean path;
    private String physicalId;
    private String description;
    private String originated;
    private String modified;
    private String vault;
    private String policy;
    private String owner;
    private String state;
    private String organization;
    private String collaborativeSpace;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectsRow row = (ObjectsRow) o;
        return id.equals(row.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String[] headers() {
        return new String[]{"Type", "Name", "Revision", "ID", "Path", "PhysicalID", "Description", "Originated", "Modified", "Vault", "Policy", "Owner", "State", "Organization", "Collaborative Space"};
    }

    @Override
    public int[] widths() {
        return new int[]{180, 240, 120, 240, 60, 300, 240, 240, 240, 120, 240, 120, 120, 180, 240};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{type, name, revision, id, path, physicalId, description, originated, modified, vault, policy, owner, state, organization, collaborativeSpace};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, Boolean.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class};
    }
}
