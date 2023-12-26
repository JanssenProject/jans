package io.jans.kc.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.kc.scheduler.config.Configuration;
import io.jans.kc.scheduler.config.ConfigurationException;
/**
 * Background scheduler application 
 * This application runs and monitors various background jobs that are 
 * used for the proper operation of Janssen and Keycloak 
 * E.g. configuration synchronization 
 * Author : Rolain Djeumen <rolain@gluu.org> 
 */
public class App {
    
    private static final String APP_DISPLAY_NAME = "Keycloak";
    private static final String PROP_APP_CFG_FILE = "app.config";
    private static final String DEFAULT_APP_CFG_FILEPATH = "/opt/kc-scheduler/conf/config.properties";
    private static final Logger log = LoggerFactory.getLogger(App.class);
    /*
     * Entry point 
     */
    public static void main(String[] args) {
        
        log.info("Application starting ...");

        Configuration config = null;
        try {
            log.debug("Loading application configuration");
            config = loadApplicationConfiguration();
            log.debug("Application configuration loaded successfully. {}",config.toString());
        }catch(ConfigurationException e) {
            log.error("Application startup failed. ",e);
            System.exit(-1);
            return;
        }

        log.info("Application Startup Successful.");
    }

    private static final String getAppConfigFileName() {

        return System.getProperty(PROP_APP_CFG_FILE);
    }

    private static final Configuration loadApplicationConfiguration() throws ConfigurationException {

        String config_file_name = getAppConfigFileName();
        if(config_file_name == null) {
            log.debug("No application configuration specified in environment variable. Using default");
            config_file_name =  DEFAULT_APP_CFG_FILEPATH;
        }
        log.debug("Application configuration file: {} ",config_file_name);
        return Configuration.fromFile(config_file_name);
    }
}
