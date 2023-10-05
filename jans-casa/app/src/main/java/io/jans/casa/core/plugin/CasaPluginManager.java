package io.jans.casa.core.plugin;

import io.jans.casa.misc.Utils;
import org.pf4j.DefaultPluginManager;
import org.pf4j.ExtensionFactory;
import org.pf4j.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

/**
 * @author jgomer
 */
public class CasaPluginManager extends DefaultPluginManager {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private SingletonExtensionFactory extensionFactory = new SingletonExtensionFactory();

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return extensionFactory;
    }

    @Override
    public ExtensionFactory getExtensionFactory() {
        return extensionFactory;
    }

    public CasaPluginManager(Path root) {
        super(root);
    }

    @Override
    public String loadPlugin(Path path) {

        String id = super.loadPlugin(path);
        if (id == null) {
            logger.warn("Plugin found at {} could not be loaded", path.toString());
        } else {
            logger.debug("Loaded plugin {}, now in state {}", id, getPlugin(id).getPluginState().toString());
        }
        return id;

    }

    @Override
    public PluginState stopPlugin(String pluginId) {

        //This call is needed before actual stop takes place, otherwise and empty set is obtained
        Set<String> extensionsClassNames = getExtensionClassNames(pluginId);
        PluginState state = super.stopPlugin(pluginId);

        if (PluginState.STOPPED.equals(state)) {
            extensionsClassNames.forEach(extensionFactory::removeSingleton);
        }
        return state;

    }

    @Override
    public boolean deletePlugin(String pluginId) {

        boolean success;
        if (Utils.onWindows()) {
            //On windows, delete is not called but this is not a problem since in practice (production environments)
            //Casa is only used in Linux (See: https://github.com/pf4j/pf4j/issues/217)
            success = unloadPlugin(pluginId);
        } else {
            //In casa 4.0 plugin removals are triggered by removal of the plugin file itself, this will cause deletePlugin
            //of DefaultPluginManager return false despite the operation succeeds. It's safe to return true here (see
            //AbstractPluginManager#deletePlugin
            super.deletePlugin(pluginId);
            success = true;
        }
        if (!success) {
            logger.warn("Plugin '{}' could not be deleted", pluginId);
        }
        return success;

    }

}
