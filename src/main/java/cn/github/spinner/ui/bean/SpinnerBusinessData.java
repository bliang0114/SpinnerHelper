package cn.github.spinner.ui.bean;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SpinnerBusinessData {

    private List<SpinnerColumnData> columnDataList;
    private Map<String, List<SpinnerColumnData>> columnDataMap = new HashMap<>();
    private String settingName;
    private String settingValue;

    public SpinnerBusinessData(String headerStr, String rowDataStr) {
        if(StrUtil.isEmpty(headerStr) || StrUtil.isEmpty(rowDataStr)){
            return;
        }
        String[] headers = headerStr.split("\t", -1);
        String[] rowData = rowDataStr.split("\t", -1);
        SpinnerColumnData columnData;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            String value;
            if(i < rowData.length){
                value = rowData[i];
            }else{
                value = "";
            }
            if(header.contains("Setting Name")){
                this.settingName = value;
                continue;
            }
            if(header.contains("Setting Value")){
                this.settingValue = value;
                continue;
            }
            columnData = new SpinnerColumnData(header, value);
            if(this.columnDataList == null){
                this.columnDataList = new ArrayList<>();
            }
            this.columnDataList.add(columnData);
        }
        convertSettings();
    }

    private void convertSettings() {
        if(StrUtil.isNotEmpty(settingName) && StrUtil.isNotEmpty(settingValue)){
            String[] settingNames = settingName.split("\\|");
            String[] settingValues = settingValue.split("\\|");
            SpinnerColumnData columnData;
            for (int i = 0; i < settingNames.length; i++) {
                String settingName = settingNames[i];
                String settingValue = settingValues[i];
                columnData = new SpinnerColumnData(settingName, settingValue);
                if(this.columnDataMap.containsKey("Settings")){
                    this.columnDataMap.get("Settings").add(columnData);
                }else{
                    List<SpinnerColumnData> dataList = new ArrayList<>();
                    dataList.add(columnData);
                    this.columnDataMap.put("Settings", dataList);
                }
            }
        }
    }
}



