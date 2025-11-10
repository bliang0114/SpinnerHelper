package cn.github.spinner.deploy;

import cn.github.driver.connection.MatrixConnection;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
