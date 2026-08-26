package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.util.WorkspaceUtil;
import org.jetbrains.annotations.NotNull;

public class ReloadPropertiesAction extends AbstractReloadCacheAction {
    public ReloadPropertiesAction() {
        super("notification.title.reload.properties", "progress.reload.properties", "message.reload.properties.success");
    }

    @Override
    protected void reload(@NotNull MatrixConnection connection) throws Exception {
        WorkspaceUtil.reloadProperties(connection);
    }
}
