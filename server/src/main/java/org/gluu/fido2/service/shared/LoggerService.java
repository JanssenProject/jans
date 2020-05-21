package org.gluu.fido2.service.shared;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.fido2.model.conf.AppConfiguration;

/**
 * Logger service
 *
 * @author Yuriy Movchan Date: 05/13/2020
 */
@ApplicationScoped
@Named
public class LoggerService extends org.gluu.service.logger.LoggerService {

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public boolean isDisableJdkLogger() {
        return appConfiguration.getDisableJdkLogger() != null && appConfiguration.getDisableJdkLogger();
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
