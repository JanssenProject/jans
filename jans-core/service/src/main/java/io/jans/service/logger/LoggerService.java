/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.jans.model.types.LoggingLayoutType;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.ConfigurationUpdate;
import io.jans.service.cdi.event.LoggerUpdateEvent;
import io.jans.service.cdi.event.Scheduled;
import io.jans.service.timer.event.TimerEvent;
import io.jans.service.timer.schedule.TimerSchedule;
import io.jans.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogManager;

/**
 * Logger service
 * At startup of any server (FIDO2, jans-auth, casa etc)
 * LoggerService is initialized inside the Application Initializer
 * (AppInitializer) class for the respective server.
 *  
 * In the server configuration for each application fido2, jans-auth,casa etc,
 * you can change the log level and the same is reflected in the corresponding
 * log level database entry
 * 
 * And this service/timer will update log level in all created loggers
 * 
 * There are 2 limitations of this Timer
 * 
 * 1. It updates log level only after server startup. First time it does this
 * after 15 seconds delay. 2. It can update logging level only after
 * instantiating loggers. This means that if no one call specific service with
 * own logger this logger will be not created But after first call logger timer
 * will update it level as well
 *
 * 
 * @author Yuriy Movchan Date: 08/19/2018
 */
public abstract class LoggerService {

    private static final JsonLayout DEFAULT_JSON_PATTERN_LAYOUT = JsonLayout.createDefaultLayout();

	private static final PatternLayout DEFAULT_TEXT_PATTERN_LAYOUT = PatternLayout.newBuilder().withPattern("%d %-5p [%t] [%C{6}] (%F:%L) - %m%n").build();

	private final static int DEFAULT_INTERVAL = 15; // 15 seconds

	private final static String OVERRIDE_JAVA_PROPERTY = "log4j2.configurationFile";

    @Inject
    private Logger log;

    @Inject
    private Event<TimerEvent> timerEvent;

    private Level prevLogLevel;
	private LoggingLayoutType prevLogLoggingLayout;

    private AtomicBoolean isActive;

	private boolean useExternalConfiguration = false;
    
    @PostConstruct
    public void create() {
        this.isActive = new AtomicBoolean(false);
    }

    public void initTimer() {
    	initTimer(false);
    }

    public void initTimer(boolean updateNow) {
    	// Log message if external log4j configuration will be used
        isDisableConfigurationUpdate(false);

        log.info("Initializing Logger Update Timer");

        final int delay = 15;
        final int interval = DEFAULT_INTERVAL;
        
        this.prevLogLevel = getCurrentLogLevel();
        this.prevLogLoggingLayout = getCurrentLoggingLayout();

        timerEvent.fire(new TimerEvent(new TimerSchedule(delay, interval), new LoggerUpdateEvent(),
                Scheduled.Literal.INSTANCE));
        
        if (updateNow) {
        	updateLoggerTimerEvent(null);
        }
    }

    @Asynchronous
    public void updateLoggerTimerEvent(@Observes @Scheduled LoggerUpdateEvent loggerUpdateEvent) {
        if (isDisableConfigurationUpdate(true)) {
        	return;
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
            updateLoggerConfiguration(true);
            this.prevLogLevel = getCurrentLogLevel();
            this.prevLogLoggingLayout = getCurrentLoggingLayout();
        } catch (Throwable ex) {
            log.error("Exception happened while updating newly added logger configuration", ex);
        } finally {
            this.isActive.set(false);
        }
    }

    @Asynchronous
    public void updateLoggerSeverity(@Observes @ConfigurationUpdate Object appConfiguration) {
        if (isDisableConfigurationUpdate(true)) {
        	return;
        }

        if (this.isActive.get()) {
            return;
        }

        if (!this.isActive.compareAndSet(false, true)) {
            return;
        }

        try {
        	updateApplicationConfiguration();
            this.prevLogLevel = getCurrentLogLevel();
            this.prevLogLoggingLayout = getCurrentLoggingLayout();
        } catch (Throwable ex) {
            log.error("Exception happened while updating logger configuration after base configuration update", ex);
        } finally {
            this.isActive.set(false);
        }
    }

	private boolean isDisableConfigurationUpdate(boolean silent) {
		if (!silent) {
			if (System.getProperty(OVERRIDE_JAVA_PROPERTY) != null) {
				log.info("Property log4j2.configurationFile is specifed. Ignoring it according to \"disableExternalLoggerConfiguration\" configuration property");
			}
			if (isDisableExternalLoggerConfiguration()) {
				log.info("External configuration is disabled with 'disableExternalLoggerConfiguration=true'");
			}
		}

		if (isDisableExternalLoggerConfiguration()) {
			if (System.getProperty(OVERRIDE_JAVA_PROPERTY) != null) {
				// Unset log4j2.configurationFile
				log.info("Property log4j2.configurationFile is cleared");
				System.clearProperty(OVERRIDE_JAVA_PROPERTY);
				// Reload configuration
				resetLoggerConfigLocation();
			}
			
			// Allow configuration dynamic update
			return false;
		}

		if (System.getProperty(OVERRIDE_JAVA_PROPERTY) != null) {
			if (!silent) {
				log.info("Property log4j2.configurationFile is specifed. Update configuration is turned off");
			}
        }

		// Disable configuration dynamic update
		return true;
	}

    private void updateApplicationConfiguration() {
        log.info("Starting logging configuration update after configuration change");

        // Disable JDK loggers
        setDisableJdkLogger();

        boolean prevUseExternalConfiguration = this.useExternalConfiguration;
        this.useExternalConfiguration = setExternalLoggerConfig(); 
        if (this.useExternalConfiguration) {
            // Use external logging configuration
            log.info("Using external logging configuration. Layout type and log level update were disabled");
        	return;
        }
        
        // Reset to default logging configuration
        if (prevUseExternalConfiguration) {
	        log.info("Replacing logging configuration with default one. Layout type and log level update were enabled");
	        resetLoggerConfigLocation();
        }
        
        // Call periodic logging configuration update
        updateLoggerConfiguration(false);
    }

    private void updateLoggerConfiguration(boolean isLoggerUpdateEvent) {
    	if (this.useExternalConfiguration) {
	        log.trace("Using external logging configuration");
    	}
    	
    	// Do periodic update to apply changes to new loggers as well
		if (checkLoggingConfig()) {
	        log.warn("Log level is invalid in logging configuration");
			return;
		}

        Level level = getCurrentLogLevel();
        LoggingLayoutType loggingLayout = getCurrentLoggingLayout();

        String msgPattern = isLoggerUpdateEvent ? "Starting layout and loggers level periodic update. Layout: '{}`, level: `{}'. Previous Layout: '{}`, level: `{}'" :
        	"Starting layout and loggers level after configuration update. Layout: '{}`, level: `{}' ";
    	log.info(msgPattern, loggingLayout, level, prevLogLoggingLayout, prevLogLevel);

        updateAppendersAndLogLevel(prevLogLoggingLayout, loggingLayout, prevLogLevel, level);
    }

    private void setDisableJdkLogger() {
    	if (isDisableJdkLogger()) {
            log.info("Starting JDK loggers update");
	        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
	        if ((globalLogger != null) && (globalLogger.getLevel() != java.util.logging.Level.OFF)) {
	            log.info("Disabling JDK loggers");
		        LogManager.getLogManager().reset();
	            globalLogger.setLevel(java.util.logging.Level.OFF);
	        }
    	}
    }

    private boolean setExternalLoggerConfig() {
        String externalLoggerConfiguration = getExternalLoggerConfiguration();
        if (StringUtils.isEmpty(externalLoggerConfiguration)) {
            log.trace("External log configuration is not provided");
            return false;
        }

        log.info("External log configuration: {}", externalLoggerConfiguration);
        File log4jFile = new File(externalLoggerConfiguration);
        if (!log4jFile.exists()) {
            log.info("External log configuration file '{}' does not exist.", log4jFile.getAbsolutePath());
            return false;
        }

        LoggerContext loggerContext = LoggerContext.getContext(false);
        if (loggerContext.getConfigLocation() != log4jFile.toURI()) {
            log.info("Starting logger context reconfigure after setting path to external configuration: '{}'", log4jFile.toURI());
	        loggerContext.setConfigLocation(log4jFile.toURI());
	        loggerContext.reconfigure();
        } else { 
        	log.debug("Logger context reconfigure is not required. Logconfiguration path is the same");
        }

        return true;
    }

    public void resetLoggerConfigLocation() {
        log.info("Reloading log4j2 configuration");

        LoggerContext loggerContext = LoggerContext.getContext(false);
        if (loggerContext.getConfigLocation() != null) {
            loggerContext.setConfigLocation(null);
            loggerContext.reconfigure();
        }
    }

    private void updateAppendersAndLogLevel(LoggingLayoutType prevLoggingLayout, LoggingLayoutType newLoggingLayout, Level prevLevel, Level newLevel) {
        final LoggerContext ctx = LoggerContext.getContext(false);

        // Update root level if needed
        Level rootLevel = ctx.getConfiguration().getRootLogger().getLevel();
    	if ((newLevel != prevLevel) && (newLevel != rootLevel)) {
        	log.info("Updating root level to '{}'", newLevel);
            ctx.getConfiguration().getRootLogger().setLevel(newLevel);
            ctx.updateLoggers();
    	}

        // Update logger configurations and appenders on layout or level change
        if ((prevLoggingLayout != newLoggingLayout) || (prevLevel != newLevel)) {
        	if (prevLoggingLayout == null) {
            	log.info("Setting logging layout to specified in configuration '{}'", newLoggingLayout);
        	} else {
            	log.info("Updating logging layout configuration from '{}' to '{}' and loggin level from '{}' to '{}'", prevLoggingLayout, newLoggingLayout, prevLevel, newLevel);
        	}
	        updateLoggerConfig(newLoggingLayout, newLevel, ctx);
        }
    	
    	// Update active loggers
        updateActiveLoggers(newLevel, ctx);
    }

	private void updateActiveLoggers(Level newLevel, final LoggerContext ctx) {
		int count = 0;
		for (org.apache.logging.log4j.core.Logger logger : ctx.getLoggers()) {
		    String loggerName = logger.getName();
		    if (loggerName.startsWith("org.gluu")) {
		        if (logger.getLevel() != newLevel) {
		            count++;
		            logger.setLevel(newLevel);
		        }
		    }
		}

		if (count > 0) {
		    log.info("Updated '{}' loggers to level '{}'", count, newLevel.toString());
		}
	}

	private void updateLoggerConfig(LoggingLayoutType loggingLayout, Level newLevel, final LoggerContext ctx) {
    	int loggerConfigUpdates = 0;
    	int appenderConfigUpdates = 0;

    	Map<String, String> updates = new HashMap<>();
    	AbstractConfiguration config = (AbstractConfiguration) ctx.getConfiguration();
        for (Map.Entry<String, LoggerConfig> loggerConfigEntry : config.getLoggers().entrySet()) {
        	LoggerConfig loggerConfig = loggerConfigEntry.getValue();
        	log.debug("Analyzing log configuration '{}'", loggerConfig.getName());

			if (!loggerConfig.getLevel().equals(newLevel)) {
				loggerConfig.setLevel(newLevel);
	        	log.debug("Updating log level in configuration '{}' to '{}'", loggerConfig.getName(), newLevel);
                loggerConfigUpdates++;
			}

			for (Map.Entry<String, Appender> appenderEntry : loggerConfig.getAppenders().entrySet()) {
	        	Appender appender = appenderEntry.getValue();
	        	log.debug("Analyzing appender '{}'", appender.getName());

	        	Layout<?> layout = appender.getLayout();
	            if (loggingLayout == LoggingLayoutType.TEXT) {
	            	layout = DEFAULT_TEXT_PATTERN_LAYOUT;
	            } else if (loggingLayout == LoggingLayoutType.JSON) {
	            	layout = DEFAULT_JSON_PATTERN_LAYOUT;
	            }

	        	if (appender instanceof RollingFileAppender) {
	                RollingFileAppender rollingFile = (RollingFileAppender) appender;
	                if (rollingFile.getLayout().getClass().isAssignableFrom(layout.getClass())) {
		                log.debug("Skipping appender update '{}'", appender.getName());
	                	// Skip logger which have required logger type
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
	                loggerConfig.addAppender(newFileAppender, newLevel, null);

	                updates.put(appender.getName(), layout.getClass().getName());
	                appenderConfigUpdates++;
	        	} else if (appender instanceof ConsoleAppender) {
	                ConsoleAppender consoleAppender = (ConsoleAppender) appender;
	                if (consoleAppender.getLayout().getClass().isAssignableFrom(layout.getClass())) {
	                	log.debug("Skipping appender update '{}'", appender.getName());
	                	// Skip logger which have required logger type
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
	                loggerConfig.addAppender(newConsoleAppender, newLevel, null);

	                updates.put(appender.getName(), layout.getClass().getName());
	                appenderConfigUpdates++;
	            }
	        }
        }

        if ((loggerConfigUpdates > 0) || (appenderConfigUpdates > 0)) {
        	log.trace("Trigger loggers update after '{}' updates", loggerConfigUpdates + appenderConfigUpdates);
        	//ctx.updateLoggers();
        	ctx.updateLoggers();
        	
        	for (Entry<String, String> entry : updates.entrySet()) {
                log.debug("Updated appender '{}'. New layout: '{}'", entry.getKey(), entry.getValue());
        	}
        }
	}

	private boolean checkLoggingConfig() {
		return StringHelper.isEmpty(getLoggingLevel()) || StringUtils.isEmpty(this.getLoggingLayout())
				|| StringHelper.equalsIgnoreCase("DEFAULT", getLoggingLevel());
	}

    private Level getCurrentLogLevel() {
		if (checkLoggingConfig()) {
			return Level.INFO;
		}

        String loggingLevel = getLoggingLevel();
        Level level = Level.toLevel(loggingLevel, Level.INFO);
        
        return level;
    }

    private LoggingLayoutType getCurrentLoggingLayout() {
		if (checkLoggingConfig()) {
			return LoggingLayoutType.TEXT;
		}

        String loggingLayout = getLoggingLayout();
        LoggingLayoutType loggingLayoutType = LoggingLayoutType.getByValue(loggingLayout.toUpperCase());
        if (loggingLayoutType == null) {
			return LoggingLayoutType.TEXT;
        }
        
        return loggingLayoutType;
    }

    public abstract boolean isDisableJdkLogger();

    public abstract boolean isDisableExternalLoggerConfiguration();

    public abstract String getLoggingLevel();

    public abstract String getLoggingLayout();

    public abstract String getExternalLoggerConfiguration();

}
