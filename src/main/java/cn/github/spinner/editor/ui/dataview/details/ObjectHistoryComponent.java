package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.task.TrackedBackgroundTask;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ObjectHistoryComponent extends AbstractObjectDetailsTableComponent {

    public ObjectHistoryComponent(Project project, String id) {
        super(project, id);
    }

    @Override
    protected String[] headers() {
        return new String[]{"Date", "User", "Action", "State", "Description"};
    }

    @Override
    protected int[] columnWidths() {
        return new int[]{240, 240, 180, 120, 600};
    }

    @Override
    protected String componentId() {
        return ObjectHistoryComponent.class.getSimpleName();
    }

    @Override
    protected void loadData() {
        tableModel.setRowCount(0);
        new TrackedBackgroundTask(project, SpinnerBundle.message("message.loading.data"), true) {
            private final List<String[]> rows = new ArrayList<>();
            private MQLException error;

            @Override
            protected void runTracked(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    String result = MQLUtil.execute(project, "print bus {} select history.time history.user history.event history.state history.description dump |", id);
                    String[] array = result.split("\\|");
                    int groupCount = array.length / 5;
                    for (int i = 0; i < groupCount; i++) {
                        rows.add(new String[]{array[i], array[i + groupCount], array[i + 2 * groupCount],
                                array[i + 3 * groupCount], array[i + 4 * groupCount]});
                    }
                } catch (MQLException e) {
                    error = e;
                }
            }

            @Override
            public void onSuccess() {
                tableModel.setRowCount(0);
                if (error != null) {
                    table.getEmptyText().setText(SpinnerBundle.message("message.error.print", id, error.getMessage()));
                    return;
                }
                rows.forEach(tableModel::addRow);
            }
        }.queue();
    }
}
