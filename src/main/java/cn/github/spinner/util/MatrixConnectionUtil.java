package cn.github.spinner.util;

import cn.github.driver.connection.MatrixConnection;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public final class MatrixConnectionUtil {

    private MatrixConnectionUtil() {
    }

    public static void closeAsync(@Nullable Project project,
                                  @Nullable MatrixConnection connection,
                                  String actionName,
                                  @Nullable Runnable afterClose) {
        if (connection == null) {
            if (afterClose != null) {
                afterClose.run();
            }
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connection.close();
            } catch (IOException e) {
                UIUtil.showErrorNotification(project, actionName, "Close connection failed: " + e.getLocalizedMessage());
            } finally {
                if (afterClose != null) {
                    afterClose.run();
                }
            }
        });
    }
}
