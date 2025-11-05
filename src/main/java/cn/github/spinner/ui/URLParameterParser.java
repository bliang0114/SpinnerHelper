package cn.github.spinner.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

public class URLParameterParser extends JFrame {
    private JTextField urlField;
    private JTable paramTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private java.util.List<Integer> searchResults;
    private int currentSearchIndex = -1;
    String title = "URL Parameter Parse Tool";
    public URLParameterParser() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(title);
        setSize(700, 500);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        // 创建输入框和按钮
        urlField = new JTextField(30);
        urlField.setPreferredSize(new Dimension(400, 30));

        // 创建搜索相关组件
        searchField = new JTextField(15);
        searchField.setPreferredSize(new Dimension(150, 30));

        // 创建表格模型和表格
        String[] columnNames = {"ParameterName", "ParameterValue"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 设置表格为只读
            }
        };
        paramTable = new JTable(tableModel);
        paramTable.setFillsViewportHeight(true);

        // 设置表格选择模式为单个单元格
        paramTable.setColumnSelectionAllowed(true);
        paramTable.setRowSelectionAllowed(false);
        paramTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paramTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        paramTable.getColumnModel().getColumn(0).setMaxWidth(250);
        paramTable.getColumnModel().getColumn(0).setMinWidth(200);

        // 设置默认选中第1列（参数值列）
        paramTable.setCellSelectionEnabled(true);

        // 设置自定义渲染器以支持高亮
        paramTable.setDefaultRenderer(Object.class, new HighlightRenderer());

        // 添加键盘监听器实现Ctrl+C复制功能
        addTableKeyBindings();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 顶部面板：URL输入框和按钮
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("URL:"));
        topPanel.add(urlField);

        JButton parseButton = new JButton("Parse");
        topPanel.add(parseButton);

        // 添加搜索组件到同一行
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        /*JButton searchButton = new JButton("搜索");
        topPanel.add(searchButton);*/

        add(topPanel, BorderLayout.NORTH);

        // 中部面板：表格
        JScrollPane scrollPane = new JScrollPane(paramTable);
        add(scrollPane, BorderLayout.CENTER);

        // 设置按钮事件
        parseButton.setActionCommand("PARSE");
        parseButton.addActionListener(new ButtonClickListener());

//        searchButton.setActionCommand("SEARCH");
//        searchButton.addActionListener(new ButtonClickListener());

        // 添加回车键监听
        searchField.addActionListener(e -> performSearch());
    }

    private void setupEventHandlers() {
        // URL输入框回车键监听
        urlField.addActionListener(e -> parseURL());
    }

    private void addTableKeyBindings() {
        // 为表格添加键盘监听器
        paramTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 检查是否按下了Ctrl+C
                if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
                    copySelectedCellValue();
                }
            }
        });

        // 为表格添加双击复制功能
        paramTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    int row = paramTable.rowAtPoint(e.getPoint());
                    int col = paramTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        // 只复制参数值列（第1列）
                        copyCellValue(row, 1);
                    }
                }
            }
        });
    }

    // 复制选中的参数值单元格内容
    private void copySelectedCellValue() {
        int selectedRow = paramTable.getSelectedRow();
        // 始终复制参数值列（第1列）
        int selectedColumn = 1;

        if (selectedRow >= 0) {
            Object value = paramTable.getValueAt(selectedRow, selectedColumn);
            if (value != null) {
                copyToClipboard(value.toString());
                showMessage("已复制到剪贴板: " + value.toString());
            }
        }
    }

    // 复制指定单元格内容（默认复制参数值列）
    private void copyCellValue(int row, int column) {
        Object value = paramTable.getValueAt(row, column);
        if (value != null) {
            copyToClipboard(value.toString());
            showMessage("已复制到剪贴板: " + value.toString());
        }
    }

    // 复制文本到剪贴板
    private void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    // 显示消息
    private void showMessage(String message) {
        /*SwingUtilities.invokeLater(() ->
                getRootPane().putClientProperty("window.message", message)
        );*/
    }

    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
                case "PARSE":
                    parseURL();
                    break;
                case "SEARCH":
                    performSearch();
                    break;
            }
        }
    }

    private void parseURL() {
        String urlText = urlField.getText().trim();
        if (urlText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please Input URL First", "Tips", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 清空表格
            tableModel.setRowCount(0);

            // 清除搜索结果
            clearSearchResults();

            // 解析URL参数
            Map<String, String> parameters = parseURLParameters(urlText);

            // 将参数添加到表格中
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "URL Format: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, String> parseURLParameters(String url) throws URISyntaxException {
        Map<String, String> parameters = new LinkedHashMap<>();

        URI uri = new URI(url);
        String query = uri.getQuery();

        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : "";

                // URL解码
                try {
                    key = java.net.URLDecoder.decode(key, "UTF-8");
                    value = java.net.URLDecoder.decode(value, "UTF-8");
                } catch (Exception e) {
                    // 如果解码失败，保持原值
                }

                parameters.put(key, value);
            }
        }

        return parameters;
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            clearHighlights();
            return;
        }

        searchResults = new java.util.ArrayList<>();

        // 在表格中搜索匹配的参数名或参数值
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String paramName = (String) tableModel.getValueAt(i, 0);
            String paramValue = (String) tableModel.getValueAt(i, 1);

            if (paramName.contains(searchText) || paramValue.contains(searchText)) {
                searchResults.add(i);
            }
        }

        if (searchResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No Match Parameter", "Search Result", JOptionPane.INFORMATION_MESSAGE);
            clearHighlights();
        } else {
            // 自动导航到下一个匹配项
            navigateToNextResult();
        }
    }

    private void navigateToNextResult() {
        if (searchResults == null || searchResults.isEmpty()) return;

        // 如果是第一次搜索或已经到最后一个，回到第一个
        if (currentSearchIndex == -1 || currentSearchIndex >= searchResults.size() - 1) {
            currentSearchIndex = 0;
        } else {
            currentSearchIndex++;
        }

        highlightCurrentResult();
    }

    private void highlightCurrentResult() {
        if (searchResults == null || searchResults.isEmpty() || currentSearchIndex < 0) return;

        int row = searchResults.get(currentSearchIndex);
        paramTable.setRowSelectionInterval(row, row);
        paramTable.scrollRectToVisible(paramTable.getCellRect(row, 0, true));
        paramTable.repaint();

        // 显示状态信息
        String searchText = searchField.getText().trim();
        setTitle(String.format(title + " (find %d matches, current %d)",
                searchResults.size(), currentSearchIndex + 1));
    }

    private void clearHighlights() {
        paramTable.clearSelection();
        clearSearchResults();
        setTitle(title);
    }

    private void clearSearchResults() {
        searchResults = null;
        currentSearchIndex = -1;
        paramTable.repaint();
    }

    // 自定义表格单元格渲染器，用于高亮显示
    private class HighlightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (searchResults != null && searchResults.contains(row) && !isSelected) {
                // 高亮显示搜索结果行
                c.setBackground(Color.YELLOW);
                c.setForeground(Color.BLACK);
            } else if (isSelected) {
                // 保持选中行的默认样式
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                // 普通行使用默认样式
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }

            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new URLParameterParser().setVisible(true);
        });
    }
}
