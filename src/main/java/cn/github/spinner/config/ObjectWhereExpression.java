package cn.github.spinner.config;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.Data;

@Data
public class ObjectWhereExpression {
    private String name;
    private String revision;
    private String id;
    private String physicalId;
    private String policy;
    private String state;
    private String organization;
    private String collaborative;

    public String getName() {
        return CharSequenceUtil.emptyToDefault(name, "*");
    }

    public String getRevision() {
        return CharSequenceUtil.emptyToDefault(revision, "*");
    }

    public String build() {
        StringBuilder builder = new StringBuilder();
        if (CharSequenceUtil.isNotEmpty(id)) {
            builder.append("id == '").append(id).append("'");
        }
        if (CharSequenceUtil.isNotEmpty(physicalId)) {
            builder.append(builder.isEmpty() ? "": " && ");
            builder.append("physicalid == '").append(physicalId).append("'");
        }
        if (CharSequenceUtil.isNotEmpty(policy)) {
            builder.append(builder.isEmpty() ? "": " && ");
            builder.append("policy == '").append(policy).append("'");
        }
        if (CharSequenceUtil.isNotEmpty(state)) {
            builder.append(builder.isEmpty() ? "": " && ");
            builder.append("state == '").append(state).append("'");
        }
        if (CharSequenceUtil.isNotEmpty(organization)) {
            builder.append(builder.isEmpty() ? "": " && ");
            builder.append("organization == '").append(organization).append("'");
        }
        if (CharSequenceUtil.isNotEmpty(collaborative)) {
            builder.append(builder.isEmpty() ? "": " && ");
            builder.append("project == '").append(collaborative).append("'");
        }
        if (!builder.isEmpty()) {
            builder.insert(0, "(").append(")");
        }
        return builder.toString();
    }
}
