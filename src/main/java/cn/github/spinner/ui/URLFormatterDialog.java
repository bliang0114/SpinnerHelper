package cn.github.spinner.ui;

import cn.github.spinner.customize.CellCopyTransferHandler;
import cn.hutool.core.util.NumberUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class URLFormatterDialog extends DialogWrapper {
    @Getter
    @Setter
    private JBTextField textField;
    private JBTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<TableModel> sorter;
    private FilterComponent filterComponent;

    public URLFormatterDialog() {
        super(true);
        setTitle("URL Parameter Parse Tool");
        setOKActionEnabled(false);
        setSize(800, 600);
        initComponents();
        setupListener();
        init();
    }

    private void initComponents() {
        textField = new JBTextField();
        tableModel = new DefaultTableModel(new String[]{"Parameter Name", "Parameter Value"}, 0);
        sorter = new TableRowSorter<>(tableModel);
        table = new JBTable(tableModel);
        table.setRowSorter(sorter);
        filterComponent = new FilterComponent("TABLE_FILTER_HISTORY", 10) {
            @Override
            public void filter() {
                applyProfessionalFilter();
            }
        };
        filterComponent.reset();
    }

    private void setupListener() {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                parseUrl(textField.getText().trim());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                parseUrl(textField.getText().trim());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                parseUrl(textField.getText().trim());
            }
        });
    }

    protected JComponent getToolbarComponent() {
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        filterComponent.setPreferredSize(JBUI.size(260, filterComponent.getHeight()));
        toolbarPanel.add(filterComponent, BorderLayout.WEST);
        return toolbarPanel;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        // 设置表头
        JBFont font = JBUI.Fonts.create("JetBrains Mono", 14);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
        header.setFont(font);
        table.setTransferHandler(new CellCopyTransferHandler(table));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setBackground(JBColor.background());
        table.setForeground(JBColor.foreground());
        table.setShowGrid(true);
        table.setRowHeight(28);
        table.setGridColor(JBColor.border());
        table.setFont(font);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(FormBuilder.createFormBuilder().addLabeledComponent("URL", textField).addComponent(filterComponent).getPanel(), BorderLayout.NORTH);
//        JPanel tablePanel = new JPanel();
//        tablePanel.setLayout(new BorderLayout());
//        JComponent toolbarPanel = getToolbarComponent();
//        tablePanel.add(toolbarPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
//        tablePanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void applyProfessionalFilter() {
        String filterText = filterComponent.getFilter();
        if (filterText == null || filterText.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        try {
            // 支持高级筛选语法
            if (filterText.contains(":")) {
                // 按列筛选 例如: "name:test type:file"
                applyColumnSpecificFilter(filterText);
            } else {
                // 全局筛选
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
            }
        } catch (Exception e) {
            // 筛选语法错误时使用全局筛选
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
            } catch (PatternSyntaxException ex) {
                sorter.setRowFilter(null);
            }
        }
    }

    private void applyColumnSpecificFilter(String filterText) {
        String[] conditions = filterText.split("\\s+");
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        for (String condition : conditions) {
            String[] parts = condition.split(":", 2);
            if (parts.length == 2) {
                String columnIndexStr = parts[0].trim();
                String value = parts[1].trim();
                // 查找列索引
                int columnIndex = NumberUtil.isInteger(columnIndexStr) ? Integer.parseInt(columnIndexStr) : 1;
                if (columnIndex >= 0 && !value.isEmpty()) {
                    RowFilter<Object, Object> filter = RowFilter.regexFilter("(?i)" + Pattern.quote(value), columnIndex);
                    filters.add(filter);
                }
            }
        }
        if (!filters.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        } else {
            sorter.setRowFilter(null);
        }
    }

    private void parseUrl(String url) {
        tableModel.setRowCount(0);
        // 1. 提取主URL和后续内容
        int questionMarkIndex = url.indexOf('?');
        String mainUrl = questionMarkIndex > 0 ? url.substring(0, questionMarkIndex) : url;
        String queryString = questionMarkIndex > 0 ? url.substring(questionMarkIndex + 1) : "";
        // 2. 识别并提取包含?的参数
        List<String> nestedUrlParams = new ArrayList<>();
        String cleanedQueryString = extractNestedUrls(queryString, nestedUrlParams);
        // 3. 提取清理后查询字符串的参数
        Map<String, String> mainParams = parseQueryString(cleanedQueryString);
        // 4. 解析嵌套URL参数
        Map<String, Map<String, String>> nestedParams = new LinkedHashMap<>();
        for (String nestedParam : nestedUrlParams) {
            parseNestedUrlParam(nestedParam, nestedParams);
        }
        // 5. 汇总所有结果
//        printSummary(mainUrl, mainParams, nestedParams);
        tableModel.addRow(new Object[]{"URL", mainUrl});
        for (Map.Entry<String, String> entry : mainParams.entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        for (Map.Entry<String, Map<String, String>> entry : nestedParams.entrySet()) {
            String paramName = entry.getKey();
            Map<String, String> subParams = entry.getValue();
            tableModel.addRow(new Object[]{paramName, subParams.get("_url")});
            for (Map.Entry<String, String> subEntry : subParams.entrySet()) {
                if (!subEntry.getKey().equals("_url")) {
                    tableModel.addRow(new Object[]{paramName + "." + subEntry.getKey(), subEntry.getValue()});
                }
            }
        }
    }

    /**
     * 嵌套URL提取方法
     *
     * @param queryString URL参数
     * @param nestedUrls  参数中的URL
     * @return {@link String}
     * @author xlwang
     */
    private String extractNestedUrls(String queryString, List<String> nestedUrls) {
        // 使用正则表达式匹配包含?的参数
        Pattern pattern = Pattern.compile("([^&=]+)=([^&]*\\?[^&]+)");
        Matcher matcher = pattern.matcher(queryString);
        List<String> toRemove = new ArrayList<>();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            // 查找这个参数的所有部分（可能被&分隔）
            String fullParam = findCompleteParam(queryString, paramName);
            if (fullParam != null) {
                nestedUrls.add(fullParam);
                toRemove.add(fullParam);
            }
        }
        // 从原始字符串中移除嵌套URL参数
        String result = queryString;
        for (String removeParam : toRemove) {
            result = result.replace(removeParam, "");
        }
        // 清理多余的&
        result = result.replace("&&", "&");
        result = result.replaceAll("^&|&$", "");
        return result;
    }

    /**
     * 查找完整的参数（包括后续的&参数，直到遇到下一个包含?的参数或结束）
     *
     * @param queryString    URL参数
     * @param startParamName 起始参数名
     * @return {@link String}
     * @author xlwang
     */
    private String findCompleteParam(String queryString, String startParamName) {
        String[] params = queryString.split("&");
        StringBuilder completeParam = new StringBuilder();
        boolean foundStart = false;
        for (String param : params) {
            if (param.startsWith(startParamName + "=")) {
                foundStart = true;
                if (!completeParam.isEmpty()) {
                    completeParam.append("&");
                }
                completeParam.append(param);
            } else if (foundStart) {
                // 如果已经找到起始参数，检查当前参数是否包含?
                if (param.contains("?")) {
                    // 遇到下一个嵌套URL参数，停止
                    break;
                } else {
                    // 继续添加普通参数
                    completeParam.append("&").append(param);
                }
            }
        }
        return foundStart ? completeParam.toString() : null;
    }

    /**
     * 解析查询字符串为键值对
     *
     * @param queryString URL参数
     * @return {@link Map<String,String>}
     * @author xlwang
     */
    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new LinkedHashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * 嵌套URL参数解析
     *
     * @param nestedParam 嵌套URL
     * @param result      结果
     * @author xlwang
     */
    private void parseNestedUrlParam(String nestedParam, Map<String, Map<String, String>> result) {
        // 首先分离出参数名和完整值
        int firstEqual = nestedParam.indexOf('=');
        if (firstEqual == -1) return;

        String paramName = nestedParam.substring(0, firstEqual);
        String remaining = nestedParam.substring(firstEqual + 1);

        // 查找第一个?的位置
        int questionMarkIndex = remaining.indexOf('?');
        if (questionMarkIndex == -1) {
            // 没有?，直接作为URL
            Map<String, String> subParams = new LinkedHashMap<>();
            subParams.put("_url", remaining);
            result.put(paramName, subParams);
            return;
        }
        // 提取主URL部分
        result.put(paramName, extractNestedParams(remaining, questionMarkIndex));
    }

    /**
     * 提取嵌套URL参数
     *
     * @param nestedQueryString 嵌套URL参数
     * @param questionMarkIndex 起始?索引
     * @return {@link Map<String,String>}
     * @author xlwang
     */
    private @NotNull Map<String, String> extractNestedParams(String nestedQueryString, int questionMarkIndex) {
        String mainUrl = nestedQueryString.substring(0, questionMarkIndex);
        String queryPart = nestedQueryString.substring(questionMarkIndex + 1);
        Map<String, String> subParams = new LinkedHashMap<>();
        subParams.put("_url", mainUrl);
        // 解析查询参数
        String[] queryParams = queryPart.split("&");
        for (String queryParam : queryParams) {
            int eqIndex = queryParam.indexOf('=');
            if (eqIndex > 0) {
                String key = queryParam.substring(0, eqIndex);
                String value = queryParam.substring(eqIndex + 1);
                subParams.put(key, value);
            }
        }
        return subParams;
    }

    // 打印汇总结果
    private static void printSummary(String mainUrl, Map<String, String> mainParams,
                                     Map<String, Map<String, String>> nestedParams) {
        System.out.println("URL: " + mainUrl);

        // 输出主参数
        for (Map.Entry<String, String> entry : mainParams.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + ",");
        }

        // 输出嵌套URL参数
        for (Map.Entry<String, Map<String, String>> entry : nestedParams.entrySet()) {
            String paramName = entry.getKey();
            Map<String, String> subParams = entry.getValue();

            System.out.println(paramName + ": " + subParams.get("_url") + ",");

            for (Map.Entry<String, String> subEntry : subParams.entrySet()) {
                if (!subEntry.getKey().equals("_url")) {
                    System.out.println(paramName + "." + subEntry.getKey() + ": " + subEntry.getValue() + ",");
                }
            }
        }
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getCancelAction()};
    }
}
