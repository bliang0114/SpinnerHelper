package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.components.PaginatedFilterTableComponent;
import cn.github.spinner.components.bean.TableRowBean;
import cn.github.spinner.config.SpinnerToken;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if (connection == null || CharSequenceUtil.isBlank(name)) {
            table.getEmptyText().setText("Connection is closed");
            return;
        }
        executor.schedule(() -> {
            try {
                tableModel.setRowCount(0);
                boolean flag = false;
                if (totalCount != 0) {
                    updatePaginationStatus();
                    flag = true;
                }
                table.getEmptyText().setText("Loading Data...");
                tableData = new ArrayList<>(loadDataFromMatrix(connection));
                totalCount = totalCount == 0 ? tableData.size() : totalCount;
                for (T data : tableData) {
                    tableModel.addRow(data.rowValues());
                }
                if (!flag) {
                    updatePaginationStatus();
                }
                table.getEmptyText().setText("Nothing to show");
            } catch (MQLException e) {
                log.error(e.getLocalizedMessage(), e);
                table.getEmptyText().setText(e.getLocalizedMessage());
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    protected void reloadData() {
        if (loaded) return;

        setPageData();
        loaded = true;
    }

    protected abstract List<T> loadDataFromMatrix(MatrixConnection connection) throws MQLException;

    public class RefreshAction extends AnAction {
        public RefreshAction() {
            super("Refresh", "Refresh", AllIcons.Actions.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            table.getFilterComponent().reset();
            loaded = false;
            reloadData();
        }
    }
}
