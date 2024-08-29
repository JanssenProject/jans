package io.jans.casa.core.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.conf.Basic2FASettings;
import io.jans.casa.misc.Utils;
import io.jans.casa.service.settings.IPluginSettingsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class PluginSettingsHandler<T> implements IPluginSettingsHandler<T> {

    private static ConfigurationHandler configurationHandler = Utils.managedBean(ConfigurationHandler.class);
    private static ObjectMapper mapper = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String pluginKey;
    private Class<T> classRef;

    public PluginSettingsHandler(String pluginKey, Class<T> configClass) {
        this.pluginKey = pluginKey;
        //configClass is needed due to Java type erasure :-|
        this.classRef = configClass;
    }

    public void save() throws Exception {

        //Hack for happy 2fa plugin
        try {
            if (pluginKey.equals("strong-authn-settings")) {
            	
                //getSettingsMap() can return null if this call is caused by the plugin being removed 
                Object obj = Optional.ofNullable(getSettingsMap()).map(m -> m.get("basic_2fa_settings")).orElse(null);
            	Basic2FASettings b2s = obj == null ? new Basic2FASettings() :
            	       mapper.convertValue(obj, new TypeReference<Basic2FASettings>(){});
                configurationHandler.getSettings().setBasic2FASettings(b2s);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } //hack end

        configurationHandler.saveSettings();

    }

    public T getSettings() {
        Map<String, Object> map = getSettingsMap();
        return map == null ? null : mapper.convertValue(map, classRef);
    }

    public void setSettings(T settings) {
        Map<String, Object> map = mapper.convertValue(settings, new TypeReference<Map<String, Object>>(){});
        configurationHandler.getSettings().getPluginSettings().put(pluginKey, map);
    }

    private Map<String, Object> getSettingsMap() {
        return configurationHandler.getSettings().getPluginSettings().get(pluginKey);
    }

}
