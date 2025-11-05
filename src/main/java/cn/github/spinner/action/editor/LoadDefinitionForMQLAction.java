package cn.github.spinner.action.editor;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.driver.connection.MatrixResultSet;
import cn.github.driver.connection.MatrixStatement;
import cn.hutool.core.text.CharSequenceUtil;
import cn.github.spinner.config.SpinnerToken;
import cn.github.spinner.editor.MQLKeywords;
import cn.github.spinner.util.UIUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LoadDefinitionForMQLAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MatrixConnection connection = SpinnerToken.getCurrentConnection(project);
        if  (connection == null) return;

        try {
            StringBuilder builder = new StringBuilder();
            MatrixStatement statement = connection.executeStatement("list type");
            MatrixResultSet resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                MQLKeywords.TYPE_INSTANCES.clear();
                List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                MQLKeywords.TYPE_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
                builder.append("Load Type Definition Successfully<br/>");
            }

            statement = connection.executeStatement("list relationship");
            resultSet = statement.executeQuery();
            if (resultSet.isSuccess()) {
                MQLKeywords.RELATIONSHIP_INSTANCES.clear();
                List<String> allData = CharSequenceUtil.split(resultSet.getResult(), "\n");
                MQLKeywords.RELATIONSHIP_INSTANCES.addAll(allData.stream().filter(CharSequenceUtil::isNotBlank).toList());
                builder.append("Load Relationship Definition Successfully<br/>");
            }

            if (!builder.isEmpty()) {
                UIUtil.showNotification(project, "Load Definition For MQL", builder.toString());
            }
        } catch (MQLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
