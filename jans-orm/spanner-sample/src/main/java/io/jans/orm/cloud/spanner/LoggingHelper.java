/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public final class LoggingHelper {

    private LoggingHelper() { }

    public static void configureConsoleAppender() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        AbstractConfiguration config = (AbstractConfiguration) ctx.getConfiguration();
        ConsoleAppender appender = ConsoleAppender.createDefaultAppenderForLayout(PatternLayout.createDefaultLayout());
        appender.start();
        config.addAppender(appender);

        AppenderRef[] refs = new AppenderRef[] {AppenderRef.createAppenderRef(appender.getName(), null, null)};
        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.DEBUG, LogManager.ROOT_LOGGER_NAME, "true",
                refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger(LogManager.ROOT_LOGGER_NAME, loggerConfig);
        ctx.updateLoggers();
    }

}
