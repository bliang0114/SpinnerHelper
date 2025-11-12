package cn.github.spinner.components.bean;

public interface TableRowBean {

    String[] headers();

    int[] widths();

    Object[] rowValues();

    Class<?>[] columnTypes();
}
