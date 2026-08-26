package cn.github.spinner.components;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServerCacheJpoSourceTest {
    @Test
    public void implementsStandardAndPropertiesCacheReloading() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/jpo/SpinnerDeployJPO.java")) {
            assertNotNull(input);
            String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(source.contains("PropertyUtil.clearAdminPropertyCache()"));
            assertTrue(source.contains("UICache.clearTenantCache(ctx)"));
            assertTrue(source.contains("CacheManager.resetAPPServerCache(ctx)"));
            assertTrue(source.contains("CacheManager.resetRMIServerCache(ctx)"));
            assertTrue(source.contains("public void reloadCache(Context ctx, String[] args)"));
            assertTrue(source.contains("public void reloadProperties(Context ctx, String[] args)"));
            assertTrue(source.contains("ResourceBundle.class.getDeclaredField(\"cacheList\")"));
            assertTrue(source.contains("ResourceBundle.clearCache(contextClassLoader)"));
            assertTrue(source.contains("ContextUtil.commitTransaction(ctx)"));
        }
    }

    @Test
    public void exposesOnlyManualCacheAndInternationalizationActionsWithDistinctIcons() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/META-INF/plugin.xml")) {
            assertNotNull(input);
            String pluginXml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertFalse(pluginXml.contains("Spinner Config.LoadDefinitions"));
            assertTrue(pluginXml.contains("Spinner Config.ReloadServerCache"));
            assertTrue(pluginXml.contains("AllIcons.Actions.ClearCash"));
            assertTrue(pluginXml.contains("Spinner Config.ReloadI18n"));
            assertTrue(pluginXml.contains("AllIcons.Actions.Properties"));
        }
    }
}
