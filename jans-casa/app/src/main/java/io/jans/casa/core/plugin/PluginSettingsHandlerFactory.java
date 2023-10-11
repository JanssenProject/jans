package io.jans.casa.core.plugin;

import io.jans.casa.service.settings.IPluginSettingsHandler;
import io.jans.casa.service.settings.IPluginSettingsHandlerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class PluginSettingsHandlerFactory implements IPluginSettingsHandlerFactory {

    private Map<String, IPluginSettingsHandler> registered = new HashMap<>();

    public <T> IPluginSettingsHandler<T> getHandler(String pluginID, Class<T> configClass) {
        registered.computeIfAbsent(pluginID, key -> new PluginSettingsHandler<T>(key, configClass));
        return registered.get(pluginID);
    }

}
