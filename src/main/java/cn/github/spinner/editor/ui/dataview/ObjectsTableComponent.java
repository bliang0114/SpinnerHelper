package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.config.ObjectWhereExpression;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.editor.ui.dataview.bean.ObjectsRow;
import cn.github.spinner.editor.ui.dataview.details.ObjectDetailsWindow;
import cn.github.spinner.ui.ObjectWhereExpressionBuilderDialog;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ObjectsTableComponent extends AbstractDataViewTableComponent<ObjectsRow> {
    private JButton whereBtn;

    public ObjectsTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new ObjectsRow(), "Objects Table");
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        whereBtn = new JButton("Where");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    @Override
    protected void setupListener() {
        super.setupListener();
        whereBtn.addActionListener(e -> {
            ObjectWhereExpressionBuilderDialog dialog = new ObjectWhereExpressionBuilderDialog(project);
            if (dialog.showAndGet()) {
                ObjectWhereExpression whereExpression = dialog.getWhereExpression();
                SpinnerToken.putObjectWhereExpression(project, whereExpression);
                String tooltip = CharSequenceUtil.format("""
                                <table>
                                    <tr>
                                        <td><strong>Name</strong></td>
                                        <td>{}</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Revision</strong></td>
                                        <td>{}</td>
                                    </tr>
                                    <tr>
                                        <td><strong>Where</strong></td>
                                        <td>{}</td>
                                    </tr>
                                </table>
                                """,
                        whereExpression.getName(),
                        whereExpression.getRevision(),
                        whereExpression.build());
                whereBtn.setToolTipText(tooltip);
                currentPage = 1;
                setPageData();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    int rowIndex = table.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        int modelRowIndex = table.convertRowIndexToModel(rowIndex);
                        if (modelRowIndex < 0) return;

                        String id = String.valueOf(tableModel.getValueAt(modelRowIndex, 4));
                        ObjectDetailsWindow.showWindow(project, id);
                    }
                }
            }
        });
    }

    @Override
    protected Component[] createToolbarComponent() {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = JBUI.emptyInsets();
        gbc.gridx = 0;
        panel.add(whereBtn);
        container.add(panel);
        return new Component[]{table.getFilterComponent(), container};
    }

    @Override
    protected List<ObjectsRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        ObjectWhereExpression whereExpression = SpinnerToken.getObjectWhereExpression(project);
        if (whereExpression == null) {
            whereExpression = new ObjectWhereExpression();
        }
        String condition = whereExpression.build();
        String countQuery = CharSequenceUtil.format("eval expr 'count TRUE' on temp query bus '{}' '{}' '{}'", name, whereExpression.getName(), whereExpression.getRevision());
        if (CharSequenceUtil.isNotBlank(condition)) {
            countQuery += " where \"" + condition + "\"";
        }
        String result = MQLUtil.execute(project, countQuery);
        if (NumberUtil.isInteger(result)) {
            totalCount = Integer.parseInt(result);
        }
        List<ObjectsRow> dataList = new ArrayList<>();
        var array = new String[0];
        if (pageSize > 0) {
            array = loadPageObject(whereExpression, condition);
        } else {
            array = loadAllObject(whereExpression);
        }
        for (String str : array) {
            String[] arrayInfo = str.split("\001");
            ObjectsRow row = new ObjectsRow();
            row.setType(arrayInfo[0]);
            row.setName(arrayInfo[1]);
            row.setRevision(arrayInfo[2]);
            row.setId(arrayInfo[3]);
            String path = arrayInfo[4];
            row.setPath(CharSequenceUtil.isNotBlank(path) && !path.equalsIgnoreCase("FALSE"));
            row.setPhysicalId(arrayInfo[5]);
            row.setDescription(arrayInfo[6]);
            row.setOriginated(arrayInfo[7]);
            row.setModified(arrayInfo[8]);
            row.setVault(arrayInfo[9]);
            row.setPolicy(arrayInfo[10]);
            row.setOwner(arrayInfo[11]);
            row.setState(arrayInfo[12]);
            row.setOrganization(arrayInfo.length > 13 ? arrayInfo[13] : "");
            row.setCollaborativeSpace(arrayInfo.length > 14 ? arrayInfo[14] : "");
            dataList.add(row);
        }
        return dataList;
    }

    private String[] loadPageObject(ObjectWhereExpression whereExpression, String condition) throws MQLException {
        var startIndex = (currentPage - 1) * pageSize;
        var result = MQLUtil.execute(project, "temp query bus '{}' '{}' '{}' where \"{}\" limit {} select id dump \001 recordsep \002", name, whereExpression.getName(), whereExpression.getRevision(), condition, (currentPage * pageSize));
        if (CharSequenceUtil.isNotBlank(result)) {
            var array = result.split("\002");
            List<String> ids = new ArrayList<>();
            for (var i = startIndex; i < array.length; i++) {
                ids.add(array[i].split("\001")[3]);
            }
            result = MQLUtil.execute(project, "temp query bus '{}' '{}' '{}' limit {} where \"id matchlist '{}' '{}'\" select id paths physicalid description originated modified lattice policy owner current organization project dump \001 recordsep \002", name, whereExpression.getName(), whereExpression.getRevision(), ids.size(), CharSequenceUtil.join(",", ids), ",");
            array = result.split("\002");
            return array;
        }
        return new String[0];
    }

    private String[] loadAllObject(ObjectWhereExpression whereExpression) throws MQLException {
        var result = MQLUtil.execute(project, "temp query bus '{}' '{}' '{}' select id paths physicalid description originated modified lattice policy owner current organization project dump \001 recordsep \002", name, whereExpression.getName(), whereExpression.getRevision());
        return result.split("\002");
    }
}
