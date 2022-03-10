/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.scim.model.conf.AppConfiguration;

@ApplicationScoped
public class LoggerService extends io.jans.service.logger.LoggerService {

    @Inject
    private AppConfiguration appConfiguration;

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
        return appConfiguration.getExternalLoggerConfiguration();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }

}

