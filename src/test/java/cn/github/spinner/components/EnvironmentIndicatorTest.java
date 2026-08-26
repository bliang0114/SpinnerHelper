package cn.github.spinner.components;

import cn.github.spinner.config.EnvironmentConfig;
import cn.github.spinner.context.UserInput;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.swing.*;
import java.awt.*;

public class EnvironmentIndicatorTest extends BasePlatformTestCase {
    public void testDisplaysConnectedEnvironmentNameAndFormatsActionText() {
        EnvironmentConfig environment = new EnvironmentConfig();
        environment.setName("DEV-01");
        UserInput.getInstance().connectEnvironment.put(getProject(), environment);

        EnvironmentIndicator indicator = new EnvironmentIndicator(getProject());

        assertEquals("DEV-01", EnvironmentIndicator.currentEnvironmentName(getProject()));
        assertTrue(findLabel(indicator).getText().contains("DEV-01"));
        assertTrue(EnvironmentIndicator.actionText(getProject(), "Deploy").contains("Deploy"));
        assertTrue(EnvironmentIndicator.actionText(getProject(), "Deploy").contains("DEV-01"));
    }

    public void testRefreshReflectsEnvironmentChanges() {
        EnvironmentIndicator indicator = new EnvironmentIndicator(getProject());
        EnvironmentConfig environment = new EnvironmentConfig();
        environment.setName("TEST");
        UserInput.getInstance().connectEnvironment.put(getProject(), environment);

        indicator.refresh();

        assertTrue(findLabel(indicator).getText().contains("TEST"));
    }

    private static JLabel findLabel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label) {
                return label;
            }
        }
        throw new AssertionError("Environment label not found");
    }
}
