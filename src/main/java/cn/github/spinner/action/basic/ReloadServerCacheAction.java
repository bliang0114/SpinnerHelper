package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.util.WorkspaceUtil;
import org.jetbrains.annotations.NotNull;

public class ReloadServerCacheAction extends AbstractReloadCacheAction {
    public ReloadServerCacheAction() {
        super("notification.title.reload.cache", "progress.reload.server.cache", "message.reload.server.cache.success");
    }

    @Override
    protected void reload(@NotNull MatrixConnection connection) throws Exception {
        WorkspaceUtil.reloadCache(connection);
    }
}
