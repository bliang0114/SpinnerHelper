package cn.github.driver.connection;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MatrixQueryResult {
    @Getter
    private List<Map<String, String>> data;

    public MatrixQueryResult(List<Map<String, String>> data) {
        this.data = Collections.unmodifiableList(new ArrayList<>(data));
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
