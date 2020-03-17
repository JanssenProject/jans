package org.gluu.oxtrust.service.init;

import org.gluu.oxauth.model.util.SecurityProviderUtility;
import org.gluu.oxtrust.config.ConfigurationFactory;
import org.gluu.oxtrust.service.logger.LoggerService;
import org.gluu.service.timer.QuartzSchedulerManager;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class AppInitializer {

    @Inject
    private Logger log;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private LoggerService loggerService;

    public void applicationInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {

        log.info("SCIM service initializing");
        SecurityProviderUtility.installBCProvider();

        configurationFactory.create();
        quartzSchedulerManager.start();
        configurationFactory.initTimer();

        loggerService.initTimer();
        log.info("Initialized!");

    }

}
