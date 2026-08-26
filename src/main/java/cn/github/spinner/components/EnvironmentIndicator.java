package cn.github.spinner.components;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import cn.github.spinner.i18n.SpinnerBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/** A theme-aware, live indicator for UI that operates on the connected Matrix environment. */
public final class EnvironmentIndicator extends JPanel {
    private static final int REFRESH_INTERVAL_MILLIS = 1000;

    private final Project project;
    private final JBLabel label = new JBLabel();
    private final Timer refreshTimer = new Timer(REFRESH_INTERVAL_MILLIS, event -> refresh());
    private String displayedEnvironmentName;

    public EnvironmentIndicator(@NotNull Project project) {
        super(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(4)));
        this.project = project;
        setOpaque(true);
        setBackground(JBColor.namedColor(
                "Banner.infoBackground",
                new JBColor(0xD9ECFF, 0x28445C)
        ));
        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.namedColor(
                        "Component.infoForeground",
                        new JBColor(0x3574A8, 0x6CA6D9)
                )),
                JBUI.Borders.empty(2, 8)
        ));

        label.setIcon(AllIcons.RunConfigurations.Remote);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        add(label);
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refresh();
        refreshTimer.start();
    }

    @Override
    public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    public void refresh() {
        String environmentName = currentEnvironmentName(project);
        if (environmentName.equals(displayedEnvironmentName)) {
            return;
        }
        displayedEnvironmentName = environmentName;
        label.setText(SpinnerBundle.message("label.current.environment", environmentName));
        label.setToolTipText(SpinnerBundle.message("tooltip.current.environment", environmentName));
    }

    public static @NotNull String currentEnvironmentName(@NotNull Project project) {
        EnvironmentConfig environment = UserInput.getInstance().connectEnvironment.get(project);
        if (environment == null || environment.getName() == null || environment.getName().isBlank()) {
            return SpinnerBundle.message("label.environment.not.connected");
        }
        return environment.getName();
    }

    public static @NotNull String actionText(@NotNull Project project, @NotNull String baseText) {
        return SpinnerBundle.message("action.with.environment", baseText, currentEnvironmentName(project));
    }
}
