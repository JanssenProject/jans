package org.xdi.oxauth.service.logger;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.LoggerContext;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.service.cdi.event.ConfigurationUpdate;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * Created by eugeniuparvan on 8/3/17.
 */
@Stateless
@Named
public class LoggerService {

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

    private boolean setExternalLoggerConfig() {
        if (StringUtils.isEmpty(appConfiguration.getExternalLoggerConfiguration())) {
            return false;
        }
        File log4jFile = new File(appConfiguration.getExternalLoggerConfiguration());
        if (!log4jFile.exists())
            return false;
        LoggerContext loggerContext = LoggerContext.getContext(false);
        loggerContext.setConfigLocation(log4jFile.toURI());
        loggerContext.reconfigure();

        configurationUpdateEvent.select(ConfigurationUpdate.Literal.INSTANCE).fire(this.appConfiguration);
        return true;
    }
}
