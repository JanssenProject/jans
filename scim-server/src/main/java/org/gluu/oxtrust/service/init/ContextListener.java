package org.gluu.oxtrust.service.init;

import org.gluu.oxauth.model.util.SecurityProviderUtility;
import org.gluu.oxtrust.config.ConfigurationFactory;
import org.gluu.oxtrust.service.logger.LoggerService;
import org.gluu.service.timer.QuartzSchedulerManager;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

    @Inject
    private Logger log;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private LoggerService loggerService;

    public void contextInitialized(ServletContextEvent sce) {

        log.info("SCIM service initializing");
        SecurityProviderUtility.installBCProvider();

        configurationFactory.create();
        quartzSchedulerManager.start();
        configurationFactory.initTimer();

        loggerService.initTimer();
        log.info("Initialized!");

    }

    public void contextDestroyed(ServletContextEvent sce) {
    }

}
