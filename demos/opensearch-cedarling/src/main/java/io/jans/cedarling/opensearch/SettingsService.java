package io.jans.cedarling.opensearch;

import java.util.*;
import java.time.Instant;

import org.apache.logging.log4j.*;
import org.json.*;
import org.opensearch.common.settings.*;

public class SettingsService {

    private static SettingsService instance = new SettingsService();
    
    private Logger logger = LogManager.getLogger(getClass());
    private PluginSettings pluginSettings;
    
    public static SettingsService getInstance() {
        return instance;
    }
    
    private SettingsService() { }
    
    public PluginSettings getSettings() {
        
        AbstractScopedSettings clSettings = getClusterSettings();
        if (pluginSettings == null) {
            reloadPluginSettings(clSettings);
        } else {
            long lastUpdated = getLastUpdated(clSettings);

            if (lastUpdated <= 0) {
                logger.warn("Plugin settings seem to have been wiped. Previous settings are retained");
            } else if (pluginSettings.getLastUpdated() < lastUpdated) {
                //Current "in memory" settings are not in sync with database settings
                reloadPluginSettings(clSettings);
            }
        }
        return pluginSettings;
        
    }
    
    public void reloadPluginSettings() {
        reloadPluginSettings(getClusterSettings());
    }
    
    private void reloadPluginSettings(AbstractScopedSettings settings) {
        
        try {
            logger.info("Reloading Cedarling plugin settings...");
            
            long lastUpdated = getLastUpdated(settings);
            if (lastUpdated <= 0) {
                logger.warn("Plugin settings not set yet");
                return;
            }
            logger.debug("Last updated on {}", Instant.ofEpochMilli(lastUpdated).toString());
            
            String key = CedarlingPlugin.SETTINGS_KEY;
            JSONObject job = Optional.ofNullable(settings.get(settings.get(key)))
                    .map(Object::toString).map(JSONObject::new).orElse(null);

            if (job == null) {
                logger.warn("Plugin settings have not been set yet. {} is missing", key);
                return;
            }
                    
            pluginSettings = PluginSettings.from(job, lastUpdated);
            CedarlingService.getInstance()
                .init(pluginSettings.getBootstrapProperties(), pluginSettings.isLogCedarlingLogs());

        } catch (Exception e) {
            logger.error("Error trying to parse Cedarling plugin settings", e);
        }

    }

    private AbstractScopedSettings getClusterSettings() {
        return CedarlingPlugin.getClusterService().getClusterSettings();
    }
    
    private long getLastUpdated(AbstractScopedSettings settings) {
        return Optional.ofNullable(settings.get(settings.get(CedarlingPlugin.LAST_UPDATED_KEY)))
                .map(Long.class::cast).orElse(0L);
    }
    
}
