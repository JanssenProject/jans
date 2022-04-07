/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.rest.model;

import java.util.HashSet;
import java.util.Set;

public class Logging {

    private String loggingLevel;
    private String loggingLayout;
    private boolean httpLoggingEnabled;
    private boolean disableJdkLogger;
    private boolean enabledOAuthAuditLogging;
    private String externalLoggerConfiguration;
    private Set<String> httpLoggingExcludePaths = new HashSet<String>();

    public Logging() {
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public String getLoggingLayout() {
        return loggingLayout;
    }

    public void setLoggingLayout(String loggingLayout) {
        this.loggingLayout = loggingLayout;
    }

    public boolean isHttpLoggingEnabled() {
        return httpLoggingEnabled;
    }

    public void setHttpLoggingEnabled(boolean httpLoggingEnabled) {
        this.httpLoggingEnabled = httpLoggingEnabled;
    }

    public boolean isDisableJdkLogger() {
        return disableJdkLogger;
    }

    public void setDisableJdkLogger(boolean disableJdkLogger) {
        this.disableJdkLogger = disableJdkLogger;
    }

    public boolean isEnabledOAuthAuditLogging() {
        return enabledOAuthAuditLogging;
    }

    public void setEnabledOAuthAuditLogging(boolean enabledOAuthAuditLogging) {
        this.enabledOAuthAuditLogging = enabledOAuthAuditLogging;
    }

    public String getExternalLoggerConfiguration() {
        return externalLoggerConfiguration;
    }

    public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
        this.externalLoggerConfiguration = externalLoggerConfiguration;
    }

    public Set<String> getHttpLoggingExcludePaths() {
        return httpLoggingExcludePaths;
    }

    public void setHttpLoggingExcludePaths(Set<String> httpLoggingExcludePaths) {
        this.httpLoggingExcludePaths = httpLoggingExcludePaths;
    }
}
