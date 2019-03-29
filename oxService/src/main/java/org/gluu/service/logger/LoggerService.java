package org.gluu.service.logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.gluu.service.cdi.async.Asynchronous;
import org.gluu.service.cdi.event.ConfigurationUpdate;
import org.gluu.service.cdi.event.LoggerUpdateEvent;
import org.gluu.service.cdi.event.Scheduled;
import org.gluu.service.timer.event.TimerEvent;
import org.gluu.service.timer.schedule.TimerSchedule;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * Logger service
 *
 * @author Yuriy Movchan Date: 08/19/2018
 */
public abstract class LoggerService {

    private final static int DEFAULT_INTERVAL = 15; // 15 seconds

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    private AtomicBoolean isActive;
    
    @PostConstruct
    public void create() {
        this.isActive = new AtomicBoolean(false);
    }

    public void initTimer() {
        log.info("Initializing Logger Update Timer");

        final int delay = 15;
        final int interval = DEFAULT_INTERVAL;

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new LoggerUpdateEvent(),
                Scheduled.Literal.INSTANCE));
    }

    @Asynchronous
    public void updateLoggerTimerEvent(@Observes @Scheduled LoggerUpdateEvent loggerUpdateEvent) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            updateLoggerConfiguration();
        } catch (Throwable ex) {
            log.error("Exception happened while updating newly added logger configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    private void updateLoggerConfiguration() {
        String loggingLevel = getLoggingLevel();
        if (StringHelper.isEmpty(loggingLevel)) {
            return;
        }

        Level level = Level.toLevel(loggingLevel, Level.INFO);
        if (StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
            return;
        }

        updateLoggers(level);
    }

    public void updateLoggerSeverity(@Observes @ConfigurationUpdate Object appConfiguration) {
        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            updateLoggerSeverityImpl();
        } catch (Throwable ex) {
            log.error("Exception happened while updating logger configuration after base configuration update", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    public void updateLoggerSeverityImpl() {
        if (isDisableJdkLogger()) {
            disableJdkLogger();
        }
        updateLoggerConfigLocation();

        String loggingLevel = getLoggingLevel();
        if (StringHelper.isEmpty(loggingLevel)) {
            return;
        }

        log.info("Setting loggers level to: '{}'", loggingLevel);
        if (StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
            log.info("Reloading log4j configuration");
            LoggerContext loggerContext = LoggerContext.getContext(false);
            loggerContext.reconfigure();
            return;
        }

        Level level = Level.toLevel(loggingLevel, Level.INFO);
        updateLoggers(level);
    }

    private void updateLoggers(Level level) {
        LoggerContext loggerContext = LoggerContext.getContext(false);

        int count = 0;
        for (org.apache.logging.log4j.core.Logger logger : loggerContext.getLoggers()) {
            String loggerName = logger.getName();
            if (loggerName.startsWith("org.gluu")) {
                if (logger.getLevel() != level) {
                    count++;
                    logger.setLevel(level);
                }
            }
        }
        
        if (count > 0) {
            log.info("Uppdated log level of '{}' loggers to {}", count, level.toString());
        }
    }

    /**
     * First trying to set external logger config from GluuAppliance.
     * If there is no valid external path to log4j2.xml location then set default configuration.
     */
    public void updateLoggerConfigLocation() {
        if (setExternalLoggerConfig()) {
            return;
        }

        LoggerContext loggerContext = LoggerContext.getContext(false);
        if (loggerContext.getConfigLocation() != null) {
            loggerContext.setConfigLocation(null);
            loggerContext.reconfigure();
        }
    }

    private void disableJdkLogger() {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        if (globalLogger != null) {
            globalLogger.setLevel(java.util.logging.Level.OFF);
        }
    }

    private boolean setExternalLoggerConfig() {
        String externalLoggerConfiguration = getExternalLoggerConfiguration();
        log.info("External log configuration: {}", externalLoggerConfiguration);
        if (StringUtils.isEmpty(externalLoggerConfiguration)) {
            return false;
        }

        File log4jFile = new File(externalLoggerConfiguration);
        if (!log4jFile.exists()) {
            log.info("External log configuration does not exist.");
            return false;
        }

        LoggerContext loggerContext = LoggerContext.getContext(false);
        loggerContext.setConfigLocation(log4jFile.toURI());
        loggerContext.reconfigure();

        return true;
    }
    
    public abstract boolean isDisableJdkLogger();

    public abstract String getLoggingLevel();
    
    public abstract String getExternalLoggerConfiguration();


}
