package io.jans.casa.plugins.sample;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Main class of this project Note this class is referenced in plugin's manifest file (entry <code>Plugin-Class</code>).
 * <p>See <a href="http://www.pf4j.org/" target="_blank">PF4J</a> plugin framework.</p>
 * @author jgomer
 */
public class SamplePlugin extends Plugin {

    public SamplePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

}
