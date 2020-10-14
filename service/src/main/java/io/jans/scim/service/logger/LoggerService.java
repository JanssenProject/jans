/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.jans.config.oxtrust.AppConfiguration;
import io.jans.scim.service.ConfigurationService;

@ApplicationScoped
public class LoggerService extends io.jans.service.logger.LoggerService {

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

