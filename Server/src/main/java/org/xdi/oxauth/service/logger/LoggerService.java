package org.xdi.oxauth.service.logger;

import java.io.File;
import java.util.logging.LogManager;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.cdi.event.ConfigurationUpdate;

/**
 * Created by eugeniuparvan on 8/3/17.
 */
@Stateless
@Named
public class LoggerService {

    @Inject
    private Logger log;

    @Inject
    private Event<AppConfiguration> configurationUpdateEvent;

    @Inject
    private AppConfiguration appConfiguration;

    /**
     * First trying to set external logger config from GluuAppliance.
     * If there is no valid external path to log4j2.xml location then set default configuration.
     */
    public void updateLoggerConfigLocation() {
        if (setExternalLoggerConfig())
            return;
        LoggerContext loggerContext = LoggerContext.getContext(false);
        loggerContext.setConfigLocation(null);
        loggerContext.reconfigure();
    }

    public void disableJdkLogger() {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        if (globalLogger != null) {
            globalLogger.setLevel(java.util.logging.Level.OFF);
        }
    }

    private boolean setExternalLoggerConfig() {
        log.info("External log configuration: " + appConfiguration.getExternalLoggerConfiguration());
        if (StringUtils.isEmpty(appConfiguration.getExternalLoggerConfiguration())) {
            return false;
        }
        File log4jFile = new File(appConfiguration.getExternalLoggerConfiguration());
        if (!log4jFile.exists()) {
            log.info("External log configuration does not exist.");
            return false;
        }
        LoggerContext loggerContext = LoggerContext.getContext(false);
        loggerContext.setConfigLocation(log4jFile.toURI());
        loggerContext.reconfigure();

        configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(this.appConfiguration);
        return true;
    }

    public void configure() {
        if (ServerUtil.isTrue(appConfiguration.getDisableJdkLogger())) {
            disableJdkLogger();
        }
        updateLoggerConfigLocation();
    }
}
