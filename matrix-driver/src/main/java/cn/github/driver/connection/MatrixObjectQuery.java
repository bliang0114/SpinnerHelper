package cn.github.driver.connection;

import lombok.Data;

@Data
public class MatrixObjectQuery {
    private String type = "*";
    private String name = "*";
    private String revision = "*";
    private String whereExpression = "";
    private String owner = "*";
    private String vault = "*";
    private short limit;
    private boolean expandType;
}
