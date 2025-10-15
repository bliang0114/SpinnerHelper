package com.bol.spinner.editor.ui.dataview.bean;

import lombok.Data;

import java.util.Objects;

@Data
public class InterfacesRow {
    private String interfaceName;
    private String attributeName;
    private String attributeOwner;
    private String type;
    private String defaultValue;
    private String range;

    public InterfacesRow(String interfaceName, String attributeName, String attributeOwner) {
        this.interfaceName = interfaceName;
        this.attributeName = attributeName;
        this.attributeOwner = attributeOwner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterfacesRow row = (InterfacesRow) o;
        return Objects.equals(attributeName, row.attributeName) && Objects.equals(attributeOwner, row.attributeOwner) && interfaceName.equals(row.interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaceName, attributeName, attributeOwner);
    }
}
