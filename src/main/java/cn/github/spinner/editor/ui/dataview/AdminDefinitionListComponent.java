package cn.github.spinner.editor.ui.dataview;

import cn.github.spinner.components.PaginatedFilterTableComponent;
import cn.github.spinner.editor.ui.dataview.bean.AdminDefinitionRow;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MatrixAdminDefinitionCache;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class AdminDefinitionListComponent extends PaginatedFilterTableComponent<AdminDefinitionRow> {
    private final Project project;
    private final MatrixAdminDefinitionCache.AdminType adminType;
    private boolean loading;

    public AdminDefinitionListComponent(@NotNull Project project,
                                        @NotNull MatrixAdminDefinitionCache.AdminType adminType) {
        super(new AdminDefinitionRow(), adminType.name() + " Definition List");
        this.project = project;
        this.adminType = adminType;
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        loadFromCache();
    }

    private void loadFromCache() {
        if (loading) {
            return;
        }
        loading = true;
        table.getEmptyText().setText(SpinnerBundle.message("message.loading.data"));
        new TrackedBackgroundTask(project, SpinnerBundle.message("message.loading.data"), true) {
            private List<AdminDefinitionRow> rows = List.of();
            private boolean cacheLoaded;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                rows = MatrixAdminDefinitionCache.get(project, adminType).stream()
                        .map(name -> new AdminDefinitionRow(adminType.displayName(), name))
                        .toList();
                cacheLoaded = MatrixAdminDefinitionCache.isLoaded(project);
            }

            @Override
            public void onSuccess() {
                loading = false;
                setTableData(rows);
                table.getEmptyText().setText(cacheLoaded
                        ? SpinnerBundle.message("message.nothing.to.show")
                        : SpinnerBundle.message("message.admin.definition.cache.empty"));
            }

            @Override
            public void onCancel() {
                loading = false;
            }
        }.queue();
    }

    @Override
    protected AnAction[] createRightToolbarAction() {
        return new AnAction[]{new RefreshAction()};
    }

    private class RefreshAction extends AnAction {
        private RefreshAction() {
            super(SpinnerBundle.message("action.refresh.text"),
                    SpinnerBundle.message("action.refresh.description"),
                    AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            table.getFilterComponent().reset();
            loadFromCache();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }
}
