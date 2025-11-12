package cn.github.spinner.editor.ui.dataview.bean;

import cn.github.spinner.components.bean.TableRowBean;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class RelationsRow implements TableRowBean {
    private String relationship;
    private String direction;
    private String typeOrRelationship;

    public RelationsRow(String relationship, String direction, String typeOrRelationship) {
        this.relationship = relationship;
        this.direction = direction;
        this.typeOrRelationship = typeOrRelationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationsRow row = (RelationsRow) o;
        return relationship.equals(row.relationship) && Objects.equals(direction, row.direction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationship, direction);
    }

    @Override
    public String[] headers() {
        return new String[]{"Relationship", "Direction", "Type / Relationship"};
    }

    @Override
    public int[] widths() {
        return new int[]{200, 50, 400};
    }

    @Override
    public Object[] rowValues() {
        return new Object[]{relationship, direction, typeOrRelationship};
    }

    @Override
    public Class<?>[] columnTypes() {
        return new Class[]{String.class, String.class, String.class};
    }
}
