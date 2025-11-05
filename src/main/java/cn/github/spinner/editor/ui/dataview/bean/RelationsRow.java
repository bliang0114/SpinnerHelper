package cn.github.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class RelationsRow {
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
}
