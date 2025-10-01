package io.jans.casa.plugins.consent;

import io.jans.casa.core.ITrackable;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * The plugin for consent management.
 * @author jgomer
 */
public class AuthorizedClientsPlugin extends Plugin implements ITrackable {

    public AuthorizedClientsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

}
