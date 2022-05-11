package io.jans.ca.server.service.logger;

import io.jans.ca.server.configuration.ApiAppConfiguration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LoggerService extends io.jans.service.logger.LoggerService {

    @Inject
    private ApiAppConfiguration appConfiguration;


    @Override
    public boolean isDisableJdkLogger() {
        System.out.println("Disable Jdk Logger : " + appConfiguration.getDisableJdkLogger());
        return (appConfiguration.getDisableJdkLogger() != null) && appConfiguration.getDisableJdkLogger();
    }

    @Override
    public String getLoggingLevel() {
        return appConfiguration.getLoggingLevel();
    }

    @Override
    public String getExternalLoggerConfiguration() {
        System.out.println("Logger Configuration : " + appConfiguration.getExternalLoggerConfiguration());
        return appConfiguration.getExternalLoggerConfiguration();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }


}

