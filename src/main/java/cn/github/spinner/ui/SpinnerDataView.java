package cn.github.spinner.ui;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.github.spinner.ui.bean.SpinnerBusinessData;
import cn.github.spinner.ui.bean.SpinnerColumnData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class SpinnerDataView extends SimpleToolWindowPanel {

    private Project project;
    private JPanel contentPanel;

    public SpinnerDataView(@NotNull Project project) {
        super(true, true);
        this.project = project;
    }

    public void display(SpinnerBusinessData businessData){
        if(businessData == null){
            return;
        }
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        List<SpinnerColumnData> columnDataList = businessData.getColumnDataList();
        Map<String, List<SpinnerColumnData>> columnDataMap = businessData.getColumnDataMap();
        JComponent formPanel = createFormPanel(columnDataList);
        contentPanel.add(formPanel, BorderLayout.CENTER);
        JComponent tabbedPane = createTabbedPane(columnDataMap);
        contentPanel.add(tabbedPane, BorderLayout.SOUTH);
        setContent(contentPanel);
    }
    private JComponent createTabbedPane(Map<String, List<SpinnerColumnData>> columnDataMap) {
        JBPanel container = new JBPanel(new BorderLayout());
        if(MapUtil.isEmpty(columnDataMap)){
            return container;
        }
        JTabbedPane tabbedPane = new JBTabbedPane();
        for (String tabName : columnDataMap.keySet()) {
            List<SpinnerColumnData> settings = columnDataMap.get(tabName);
            JComponent tabPanel = createFormPanel(settings);
            tabbedPane.addTab(tabName, tabPanel);
        }
        container.add(tabbedPane, BorderLayout.CENTER);
        return container;
    }

    private JComponent createFormPanel(List<SpinnerColumnData> columnDataList){
        JBPanel formPanel = new JBPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(JBUI.Borders.empty(10));
        if(CollUtil.isEmpty(columnDataList)){
            return formPanel;
        }
        for (SpinnerColumnData spinnerColumnData : columnDataList) {
            formPanel.add(createFormItem(spinnerColumnData.getColumnName(),spinnerColumnData.getColumnValue()));
            formPanel.add(Box.createVerticalStrut(5));
        }
        return formPanel;
    }
    private JComponent createFormItem(String labelText, String filedValue) {
        JBPanel rowPanel = new JBPanel(new BorderLayout(10, 0));
        JBLabel label = new JBLabel(labelText);
        label.setPreferredSize(new Dimension(100, 30));
        label.setToolTipText(labelText);
        JBTextField textField = new JBTextField(filedValue, 6);
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
//        textField.setPreferredSize(new Dimension(100, 30));
//        textField.setText(filedValue);
        rowPanel.add(label, BorderLayout.WEST);
        rowPanel.add(textField, BorderLayout.CENTER);
        return rowPanel;
    }
}
