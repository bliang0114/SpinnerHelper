package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author fzhang
 * @date 2025/11/10
 */
@Getter
@AllArgsConstructor
public class FileOperationContext {
    private final Project project;
    private final MatrixConnection matrixConnection;
}
