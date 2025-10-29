package com.bol.spinner.ui.bean;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.bol.spinner.util.MQLUtil;
import com.bol.spinner.util.UIUtil;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Slf4j
public class MenuCommandNode {
    private String name;
    private String type;

    private String description;
    private String label;
    private String href;
    private String alt;
    private Map<String, String> setting=new HashMap<>();

    public MenuCommandNode(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return CharSequenceUtil.format("{} ({})", name, type);
    }

    public boolean isMenu() {
        return type.equals("Menu");
    }



    public void setInfo(Project project) {
        try {
            String[] commandInfoArray = MQLUtil.execute(project, "print {} '{}' select description label  alt setting.name setting.value dump", this.getType().toLowerCase(), this.getName()).split(StrPool.COMMA);
            if (commandInfoArray.length >= 3) {
                this.setDescription(commandInfoArray[0]);
                this.setLabel(commandInfoArray[1]);
                this.setAlt(commandInfoArray[2]);
                this.setHref(MQLUtil.execute(project, "print {} '{}' select href dump", this.getType().toLowerCase(), this.getName()).replaceAll("&=","\n&="));
                int settingCount = commandInfoArray.length - 3;
                if (settingCount % 2 == 0) {
                    int offset = settingCount / 2;
                    this.setting = new HashMap<>(offset);
                    for (int i = 0; i < offset; i++) {
                        this.setting.put(commandInfoArray[3 + i], commandInfoArray[3 + offset + i]);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 Menu 或 Command 信息失败");
        }
    }
}
