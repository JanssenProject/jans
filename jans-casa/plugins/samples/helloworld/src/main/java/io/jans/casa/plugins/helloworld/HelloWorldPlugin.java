package io.jans.casa.plugins.helloworld;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Main class of this project Note this class is referenced in plugin's manifest file (entry <code>Plugin-Class</code>).
 * <p>See <a href="http://www.pf4j.org/" target="_blank">PF4J</a> plugin framework.</p>
 * @author jgomer
 */
public class HelloWorldPlugin extends Plugin {

    public HelloWorldPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

}
