package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.components.PaginatedFilterTableComponent;
import cn.github.spinner.components.bean.TableRowBean;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.UIUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public abstract class AbstractDataViewTableComponent<T extends TableRowBean> extends PaginatedFilterTableComponent<T> {
    protected final Project project;
    protected final VirtualFile virtualFile;
    @Getter
    @Setter
    protected String name;
    @Setter
    protected boolean loaded = false;
    protected final ScheduledExecutorService executor;

    public AbstractDataViewTableComponent(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull T entity, String componentId) {
        super(entity, componentId);
        this.project = project;
        this.virtualFile = virtualFile;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    @Override
    protected AnAction[] createRightToolbarAction() {
        return new AnAction[]{new RefreshAction()};
    }

    @Override
    protected void setPageData() {
        MatrixConnection connection = UserInput.getInstance().connection.get(project);
        if (connection == null || CharSequenceUtil.isBlank(name)) {
            table.getEmptyText().setText(SpinnerBundle.message("message.connection.closed"));
            return;
        }
        tableModel.setRowCount(0);
        if (totalCount != 0) {
            updatePaginationStatus();
        }
        table.getEmptyText().setText(SpinnerBundle.message("message.loading.data"));
        new TrackedBackgroundTask(project, SpinnerBundle.message("message.loading.data")) {
            private Throwable error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    tableData = new ArrayList<>(loadDataFromMatrix(connection));
                } catch (MQLException e) {
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                if (error != null) {
                    log.error(error.getLocalizedMessage(), error);
                    UIUtil.showErrorNotification(project, SpinnerBundle.message("notification.title.loading.data"), error.getLocalizedMessage());
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    totalCount = totalCount == 0 ? tableData.size() : totalCount;
                    SwingUtilities.invokeLater(() -> {
                        for (T data : tableData) {
                            tableModel.addRow(data.rowValues());
                        }
                    });
                    updatePaginationStatus();
                    table.getEmptyText().setText(SpinnerBundle.message("message.nothing.to.show"));
                });
            }
        }.queue();
    }

    protected void reloadData() {
        if (loaded) return;

        setPageData();
        loaded = true;
    }

    protected abstract List<T> loadDataFromMatrix(MatrixConnection connection) throws MQLException;

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super(SpinnerBundle.message("action.refresh.text"), SpinnerBundle.message("action.refresh.description"), AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            table.getFilterComponent().reset();
            loaded = false;
            reloadData();
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }
}
