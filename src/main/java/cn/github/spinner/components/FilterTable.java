package cn.github.spinner.components;

import cn.github.spinner.customize.CellCopyTransferHandler;
import cn.hutool.core.util.NumberUtil;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterTable extends JBTable {
    private final TableRowSorter<TableModel> sorter;
    @Getter
    private final FilterComponent filterComponent;

    public FilterTable() {
        this(new DefaultTableModel());
    }

    public FilterTable(TableModel model) {
        super(model);
        sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);
        filterComponent = new FilterComponent("TABLE_FILTER_HISTORY", 10) {
            @Override
            public void filter() {
                applyProfessionalFilter();
            }
        };
        filterComponent.reset();
        filterComponent.setPreferredSize(JBUI.size(300, 30));
        // 设置表头
        initFont();
//        JBFont font = JBUI.Fonts.create("JetBrains Mono", 14);
        JTableHeader header = getTableHeader();
        header.setPreferredSize(JBUI.size(-1, 30));
        header.setReorderingAllowed(false);
        header.setBackground(JBColor.background());
//        header.setFont(font);
        setTransferHandler(new CellCopyTransferHandler(this));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setBackground(JBColor.background());
        setForeground(JBColor.foreground());
        setShowGrid(true);
        setRowHeight(28);
        setGridColor(JBColor.border());
//        setFont(font);
    }

    @Override
    public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
        TableModel model = this.getModel();
        Component c = super.prepareRenderer(renderer, row, column);
        if (column == 0 && model instanceof RowNumberTableModel) { // 行号列特殊处理
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.LEFT);
                if (isRowSelected(row)) {
                    label.setBackground(getSelectionBackground());
                    label.setForeground(getSelectionForeground());
                } else {
                    label.setBackground(row % 2 == 0 ? UIUtil.getTableBackground() : UIUtil.getDecoratedRowColor());
                    label.setForeground(UIUtil.getLabelForeground());
                }
            }
        }
        return c;
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

    private void initFont() {
        // 优先使用 JetBrains Mono 显示英文/数字/符号，中文自动 fallback 到系统字体
        Font codeFont = new Font("JetBrains Mono", Font.PLAIN, 12);
        // 验证中文支持（JetBrains Mono 会返回 false，触发系统字体 fallback）
        if (!codeFont.canDisplay('中')) {
            // 手动指定中文备用字体（适配不同系统）
            String systemFontName = getSystemDefaultChineseFont();
            Font mixedFont = new Font(systemFontName, Font.PLAIN, 12);
            setFont(mixedFont);
            getTableHeader().setFont(mixedFont.deriveFont(Font.BOLD));
        } else {
            setFont(codeFont);
            getTableHeader().setFont(codeFont.deriveFont(Font.BOLD));
        }
    }

    // 获取系统默认中文字体（适配 Windows/macOS/Linux）
    private String getSystemDefaultChineseFont() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "Microsoft YaHei"; // Windows 系统
        } else if (os.contains("mac")) {
            return "PingFang SC"; // macOS 系统
        } else {
            return "Noto Sans CJK SC"; // Linux 系统（需安装思源黑体）
        }
    }
}
