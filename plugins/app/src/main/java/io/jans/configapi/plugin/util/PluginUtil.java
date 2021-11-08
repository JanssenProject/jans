package io.jans.configapi.plugin.config.util;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;

@ApplicationScoped
public class PluginUtil {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static boolean onWindows() {
        return System.getProperty("os.name").toLowerCase().matches(".*win.*");
    }

}
