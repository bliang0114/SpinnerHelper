package com.bol.spinner.ui.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SpinnerColumnData {
    private String columnName;
    private String columnValue;

    public SpinnerColumnData(String columnName, String columnValue) {
        this.columnName = columnName.trim();
        this.columnValue = columnValue.trim();
    }
}
