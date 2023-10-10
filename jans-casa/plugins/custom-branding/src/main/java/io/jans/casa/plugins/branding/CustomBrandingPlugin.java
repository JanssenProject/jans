package io.jans.casa.plugins.branding;

import io.jans.casa.misc.Utils;
import io.jans.casa.service.IBrandingManager;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The plugin for custom branding Gluu Casa.
 * @author jgomer
 */
public class CustomBrandingPlugin extends Plugin {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public CustomBrandingPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void delete() {

        try {
            Utils.managedBean(IBrandingManager.class).factoryReset();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
