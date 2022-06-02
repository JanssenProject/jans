package io.jans.ca.server.service.logger;

import io.jans.ca.server.configuration.ApiAppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LoggerServiceImpl extends io.jans.service.logger.LoggerService {

    private static final Logger LOG = LoggerFactory.getLogger(LoggerServiceImpl.class);

    @Inject
    private ApiAppConfiguration appConfiguration;


    @Override
    public boolean isDisableJdkLogger() {
        LOG.info("Disable Jdk Logger : ", appConfiguration.getDisableJdkLogger());
        return (appConfiguration.getDisableJdkLogger() != null) && appConfiguration.getDisableJdkLogger();
    }

    @Override
    public String getLoggingLevel() {
        return appConfiguration.getLoggingLevel();
    }

    @Override
    public String getExternalLoggerConfiguration() {
        LOG.info("Logger Configuration : {}", appConfiguration.getExternalLoggerConfiguration());
        return appConfiguration.getExternalLoggerConfiguration();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }


}

