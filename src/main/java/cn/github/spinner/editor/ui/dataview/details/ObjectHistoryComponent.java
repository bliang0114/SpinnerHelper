package cn.github.spinner.editor.ui.dataview.details;

import cn.github.driver.MQLException;
import cn.github.spinner.util.MQLUtil;
import com.intellij.openapi.project.Project;

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
        try {
            String result = MQLUtil.execute(project, "print bus {} select history.time history.user history.event history.state history.description dump |", id);
            String[] array = result.split("\\|");
            int groupCount = array.length / 5;
            for (int i = 0; i < groupCount; i++) {
                String time = array[i];
                String user = array[i + groupCount];
                String action = array[i + 2 * groupCount];
                String state = array[i + 3 * groupCount];
                String description = array[i + 4 * groupCount];
                tableModel.addRow(new String[]{time, user, action, state, description});
            }
        } catch (MQLException e) {
            table.getEmptyText().setText("Error: print " + id + " error. " + e.getMessage());
        }
    }
}
