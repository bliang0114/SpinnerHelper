package com.bol.spinner.editor.ui.dataview.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProgramsRow {
    private String name;
    private String createTime;
    private String updateTime;

    public ProgramsRow(String name, String createTime, String updateTime) {
        this.name = name;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }
}
