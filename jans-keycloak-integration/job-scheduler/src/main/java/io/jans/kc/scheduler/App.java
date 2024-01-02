/**
 * Background scheduler application 
 * This application runs and monitors various background jobs that are 
 * used for the proper operation of Janssen and Keycloak 
 * E.g. configuration synchronization 
 * Author : Rolain Djeumen <rolain@gluu.org> 
 */

package io.jans.kc.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.kc.scheduler.config.Configuration;
import io.jans.kc.scheduler.config.ConfigurationException;

import io.jans.kc.scheduler.job.*;
import io.jans.kc.scheduler.job.service.JobScheduler;
import io.jans.kc.scheduler.job.service.JobSchedulerException;
import io.jans.kc.scheduler.job.service.impl.QuartzJobScheduler;

public class App {
    
    private static final String APP_DISPLAY_NAME = "Keycloak";
    private static final String PROP_APP_CFG_FILE = "app.config";
    private static final String DEFAULT_APP_CFG_FILEPATH = "/opt/kc-scheduler/conf/config.properties";
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static JobScheduler jobScheduler = null;
    private static boolean running = false;
    /*
     * Entry point 
     */
    public static void main(String[] args) throws InterruptedException {

        log.info("Application starting ...");
        Configuration config = null;
        try {
            log.debug("Loading application configuration");
            config = loadApplicationConfiguration();
            log.debug("Application configuration loaded successfully. {}",config.toString());

            log.debug("Initializing scheduler ");
            jobScheduler = createJobScheduler(config);
            startJobScheduler(jobScheduler);

            log.debug("Performing post-startup operations");
            performPostStartupOperations();

            log.debug("Application startup successful");
            while(running) {
                Thread.sleep(1000);
            }
            log.debug("Application shutting down");
        }catch(ConfigurationException e) {
            log.error("Application startup failed. ",e);
            System.exit(-1);
            return;
        }

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

    private static final JobScheduler createJobScheduler(Configuration configuration) {

        return createQuartzJobSchedulerFromConfiguration(configuration);
    }

    private static final void startJobScheduler(JobScheduler jobScheduler) {

        jobScheduler.start();
    }

    private static final JobScheduler createQuartzJobSchedulerFromConfiguration(Configuration configuration) {
        
        try {
            return QuartzJobScheduler.builder()
                .name(configuration.quatzSchedulerName())
                .instanceId(configuration.quartzSchedulerInstanceId())
                .threadPoolSize(configuration.quartzSchedulerThreadPoolSize())
                .build();
        }catch(ConfigurationException e) {
            throw new StartupError("Could not create quartz job scheduler",e);
        }
    }

    private static final void performPostStartupOperations() {

        running = true;
        registerShutdownHook();
    }

    private static final void registerShutdownHook() {

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    
    public static class ShutdownHook extends Thread  {
        
        
        @Override
        public void run() {

            try {
                log.debug("Shutting down application");
                if (jobScheduler != null) {
                    jobScheduler.stop();
                }
            }catch(Exception e) {
                log.warn("Something unexpected happened while stopping the scheduler",e);
            }

            running = false;
        }
    }

    public static class StartupError extends RuntimeException {

        public StartupError(String msg) {
            super(msg);
        }

        public StartupError(String msg, Throwable cause) {
            super(msg,cause);
        }
    }
}
