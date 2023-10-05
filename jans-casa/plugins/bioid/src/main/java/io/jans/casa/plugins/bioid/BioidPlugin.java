package io.jans.casa.plugins.bioid;

import io.jans.casa.core.ITrackable;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * A plugin for handling second factor authentication settings for administrators and users.
 * @author jgomer
 */
public class BioidPlugin extends Plugin implements ITrackable {

    public BioidPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }


}
