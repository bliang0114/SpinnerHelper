package cn.github.driver.connection;

import lombok.Data;

@Data
public class MatrixConnectionQuery {
    private String type = "*";
    private String whereExpression;
    private String vault = "*";
    private short limit;
}
