package cn.github.spinner.editor.ui.dataview;

import cn.github.spinner.components.PaginatedFilterTableComponent;
import cn.github.spinner.editor.ui.dataview.bean.AdminDefinitionRow;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.util.MatrixAdminDefinitionCache;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class AdminDefinitionListComponent extends PaginatedFilterTableComponent<AdminDefinitionRow> {
    private final Project project;
    private final MatrixAdminDefinitionCache.AdminType adminType;

    public AdminDefinitionListComponent(@NotNull Project project,
                                        @NotNull MatrixAdminDefinitionCache.AdminType adminType) {
        super(new AdminDefinitionRow(), adminType.name() + " Definition List");
        this.project = project;
        this.adminType = adminType;
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        loadFromCache();
    }

    private void loadFromCache() {
        List<AdminDefinitionRow> rows = MatrixAdminDefinitionCache.get(project, adminType).stream()
                .map(name -> new AdminDefinitionRow(adminType.displayName(), name))
                .toList();
        setTableData(rows);
        table.getEmptyText().setText(MatrixAdminDefinitionCache.isLoaded(project)
                ? SpinnerBundle.message("message.nothing.to.show")
                : SpinnerBundle.message("message.admin.definition.cache.empty"));
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
