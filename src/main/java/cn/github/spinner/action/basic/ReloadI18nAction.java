package cn.github.spinner.action.basic;

import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.util.WorkspaceUtil;
import org.jetbrains.annotations.NotNull;

public class ReloadI18nAction extends AbstractReloadCacheAction {
    public ReloadI18nAction() {
        super("notification.title.reload.i18n", "progress.reload.i18n", "message.reload.i18n.success");
    }

    @Override
    protected void reload(@NotNull MatrixConnection connection) throws Exception {
        WorkspaceUtil.reloadProperties(connection);
    }
}
