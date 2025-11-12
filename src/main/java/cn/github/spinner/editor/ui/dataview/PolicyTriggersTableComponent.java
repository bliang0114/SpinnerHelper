package cn.github.spinner.editor.ui.dataview;

import cn.github.driver.MQLException;
import cn.github.driver.connection.MatrixConnection;
import cn.github.spinner.editor.ui.dataview.bean.PolicyTriggersRow;
import cn.github.spinner.util.MQLUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PolicyTriggersTableComponent extends AbstractDataViewTableComponent<PolicyTriggersRow> {

    public PolicyTriggersTableComponent(@NotNull Project project, VirtualFile virtualFile) {
        super(project, virtualFile, new PolicyTriggersRow(), "Policy Triggers Table Toolbar");
    }

    @Override
    protected List<PolicyTriggersRow> loadDataFromMatrix(MatrixConnection connection) throws MQLException {
        var policyResult = MQLUtil.execute(project, "print type '{}' select policy dump", name);
        var policies = CharSequenceUtil.split(policyResult, ",");
        policies = policies.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<PolicyTriggersRow> dataList = new ArrayList<>();
        for (String policy : policies) {
            var stateResult = MQLUtil.execute(project, "print policy '{}' select state dump", policy);
            if (CharSequenceUtil.isBlank(stateResult)) continue;

            var states = CharSequenceUtil.split(stateResult, ",");
            for (var state : states) {
                var result = MQLUtil.execute(project, "print policy '{}' select state[{}].trigger dump", policy, state);
                if (CharSequenceUtil.isNotBlank(result)) {
                    var triggers = CharSequenceUtil.split(result, ",");
                    triggers = triggers.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
                    for (var trigger : triggers) {
                        var triggerSplit = trigger.split(":");
                        var prog = triggerSplit[1].substring(0, triggerSplit[1].indexOf("("));
                        var param = triggerSplit[1].substring(triggerSplit[1].indexOf("(") + 1, triggerSplit[1].indexOf(")"));
                        String[] params;
                        if (prog.equals("emxTriggerManager")) {
                            prog = "TM";
                            params = param.split(" ");
                        } else {
                            params = new String[]{param};
                        }
                        for (var param1 : params) {
                            if (param1.trim().isEmpty()) {
                                continue;
                            }
                            PolicyTriggersRow row = new PolicyTriggersRow(policy, state);
                            if (triggerSplit[0].endsWith("Check")) {
                                row.setTriggerName(triggerSplit[0].substring(0, triggerSplit[0].length() - 5));
                                row.setCheck(prog + ": " + param1);
                            } else if (triggerSplit[0].endsWith("Action")) {
                                row.setTriggerName(triggerSplit[0].substring(0, triggerSplit[0].length() - 6));
                                row.setAction(prog + ": " + param1);
                            } else if (triggerSplit[0].endsWith("Override")) {
                                row.setTriggerName(triggerSplit[0].substring(0, triggerSplit[0].length() - 8));
                                row.setOverride(prog + ": " + param1);
                            }
                            dataList.add(row);
                        }
                    }
                } else {
                    dataList.add(new PolicyTriggersRow(policy, state));
                }
            }
        }
        return dataList;
    }
}
