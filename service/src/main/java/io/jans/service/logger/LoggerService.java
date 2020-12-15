/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.logger;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;

import io.jans.model.types.LoggingLayoutType;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.LoggerUpdateEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;

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
    	// Do periodic update to apply changes to new loggers as well
        String loggingLevel = getLoggingLevel();
		if (StringHelper.isEmpty(loggingLevel) || StringUtils.isEmpty(this.getLoggingLayout())
				|| StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
			return;
		}

        Level level = Level.toLevel(loggingLevel, Level.INFO);
        LoggingLayoutType loggingLayout = LoggingLayoutType.getByValue(this.getLoggingLayout().toUpperCase());

        updateAppendersAndLogLevel(loggingLayout, level);
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

    public void updateLoggerSeverity() {
    	// Full log4j2 configuration reload
    	updateLoggerSeverityImpl();
    }

    private void updateLoggerSeverityImpl() {
        setDisableJdkLogger();

        if (setExternalLoggerConfig()) {
        	return;
        }

        resetLoggerConfigLocation();

        String loggingLevel = getLoggingLevel();
		if (StringHelper.isEmpty(loggingLevel) || StringUtils.isEmpty(this.getLoggingLayout())
				|| StringHelper.equalsIgnoreCase("DEFAULT", loggingLevel)) {
			return;
		}

        Level level = Level.toLevel(loggingLevel, Level.INFO);
        LoggingLayoutType loggingLayout = LoggingLayoutType.getByValue(this.getLoggingLayout().toUpperCase());

        log.info("Setting layout and loggers level to '{}`, `{}' after configuration update", loggingLayout, loggingLevel);

        updateAppendersAndLogLevel(loggingLayout, level);
    }

    private void setDisableJdkLogger() {
    	if (isDisableJdkLogger()) {
	        LogManager.getLogManager().reset();
	        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
	        if (globalLogger != null) {
	            globalLogger.setLevel(java.util.logging.Level.OFF);
	        }
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

    public void resetLoggerConfigLocation() {
        log.info("Reloading log4j2 configuration");

        LoggerContext loggerContext = LoggerContext.getContext(false);
        if (loggerContext.getConfigLocation() != null) {
            loggerContext.setConfigLocation(null);
        }
        loggerContext.reconfigure();
    }

    private void updateAppendersAndLogLevel(LoggingLayoutType loggingLayout, Level level) {
    	boolean runLoggersUpdate = false;
    	int loggerConfigUpdates = 0;
    	int appenderConfigUpdates = 0;
        LoggerContext ctx = LoggerContext.getContext(false);

        AbstractConfiguration config = (AbstractConfiguration) ctx.getConfiguration();
        for (Entry<String, LoggerConfig> loggerConfigEntry : config.getLoggers().entrySet()) {
        	LoggerConfig loggerConfig = loggerConfigEntry.getValue();
        	log.trace("Updating log configuration '{}'", loggerConfig.getName());

			if (!loggerConfig.getLevel().equals(level)) {
				loggerConfig.setLevel(level);
	        	log.trace("Updating log level in configuration '{}' to '{}'", loggerConfig.getName(), level);
                runLoggersUpdate = true;
                loggerConfigUpdates++;
			}

			for (Map.Entry<String, Appender> appenderEntry : loggerConfig.getAppenders().entrySet()) {
	        	Appender appender = appenderEntry.getValue();
	        	log.trace("Updating appender '{}'", appender.getName());

	        	Layout<?> layout = appender.getLayout();
	            if (loggingLayout == LoggingLayoutType.TEXT) {
	            	layout = PatternLayout.newBuilder().withPattern("%d %-5p [%t] [%C{6}] (%F:%L) - %m%n").build();
	            } else if (loggingLayout == LoggingLayoutType.JSON) {
	            	layout = JsonLayout.createDefaultLayout();
	            }
	
	        	if (appender instanceof RollingFileAppender) {
	                RollingFileAppender rollingFile = (RollingFileAppender) appender;
	                if (rollingFile.getLayout().getClass().isAssignableFrom(layout.getClass())) {
	                	continue;
	                }
	                RollingFileAppender newFileAppender = RollingFileAppender.newBuilder()
	                        .setLayout(layout)
	                        .withStrategy(rollingFile.getManager().getRolloverStrategy())
	                        .withPolicy(rollingFile.getTriggeringPolicy())
	                        .withFileName(rollingFile.getFileName())
	                        .withFilePattern(rollingFile.getFilePattern())
	                        .setName(rollingFile.getName())
	                        .build();
	                newFileAppender.start();
	                appender.stop();
	                loggerConfig.removeAppender(appenderEntry.getKey());
	                loggerConfig.addAppender(newFileAppender, null, null);

	                runLoggersUpdate = true;
	                appenderConfigUpdates++;
	        	} else if (appender instanceof ConsoleAppender) {
	                ConsoleAppender consoleAppender = (ConsoleAppender) appender;
	                if (consoleAppender.getLayout().getClass().isAssignableFrom(layout.getClass())) {
	                	continue;
	                }

	                ConsoleAppender newConsoleAppender = ConsoleAppender.newBuilder()
	                        .setLayout(layout)
	                        .setTarget(consoleAppender.getTarget())
	                        .setName(consoleAppender.getName())
	                        .build();
	                newConsoleAppender.start();
	                appender.stop();
	                loggerConfig.removeAppender(appenderEntry.getKey());
	                loggerConfig.addAppender(newConsoleAppender, null, null);

	                runLoggersUpdate = true;
	                appenderConfigUpdates++;
	            }
	        }
        }

        if (runLoggersUpdate) {
        	log.trace("Trigger loggers update after '{}' updates", loggerConfigUpdates + appenderConfigUpdates);
        	ctx.updateLoggers();
        }
    }
    
    public abstract boolean isDisableJdkLogger();

    public abstract String getLoggingLevel();
    
    public abstract String getExternalLoggerConfiguration();

    public abstract String getLoggingLayout();

}
