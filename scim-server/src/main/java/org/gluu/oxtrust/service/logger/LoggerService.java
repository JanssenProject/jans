package org.gluu.oxtrust.service.logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.oxtrust.service.ConfigurationService;

@ApplicationScoped
public class LoggerService extends org.gluu.service.logger.LoggerService {

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ConfigurationService configurationService;

    @Override
    public boolean isDisableJdkLogger() {
        return (appConfiguration.getDisableJdkLogger() != null) && appConfiguration.getDisableJdkLogger();
    }

    @Override
    public String getLoggingLevel() {
        return appConfiguration.getLoggingLevel();
    }

    @Override
    public String getExternalLoggerConfiguration() {
        return configurationService.getConfiguration().getOxLogConfigLocation();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }

}

