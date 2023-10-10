package io.jans.casa.service.settings;

public interface IPluginSettingsHandlerFactory {

    //Atention: pluginID is not good to guarantee that plugin X cannot try to access or set data of plugin Y
    //Probably classloader is better, however a dev can still get access to it via the plugin manager, see:
    //https://github.com/pf4j/pf4j/issues/353

    /**
     * Obtains an instance of {@link IPluginSettingsHandler} for getting access to the configuration settings of the
     * plugin identified by <code>pluginID</code>.
     * @param pluginID Plugin identifier
     * @param configClass Class depicting the configuration for the plugin
     * @param <T> Type parameter for configClass
     * @return An object to handle plugin's configuration. The configuration data will be saved alongside the app (Casa)
     * configuration, that is, in the underlying database.
     */
    <T> IPluginSettingsHandler<T> getHandler(String pluginID, Class<T> configClass);

}
