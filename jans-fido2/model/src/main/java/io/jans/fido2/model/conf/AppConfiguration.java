/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AuthenticationProtectionConfiguration;
import io.jans.as.model.configuration.Configuration;

import io.jans.doc.annotation.DocProperty;
import jakarta.enterprise.inject.Vetoed;

import static io.jans.as.model.configuration.AppConfiguration.DEFAULT_SESSION_ID_LIFETIME;

/**
 * Represents the configuration JSON file.
 *
 * @author Yuriy Movchan
 * @version May 13, 2020
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Vetoed
public class AppConfiguration implements Configuration {

	@DocProperty(description = "URL using the https scheme for Issuer identifier")
    private String issuer;
	@DocProperty(description = "The base URL for Fido2 endpoints")
    private String baseEndpoint;
	@DocProperty(description = "Time interval for the Clean Service in seconds")
    private int cleanServiceInterval;
	@DocProperty(description = "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage")
    private int cleanServiceBatchChunkSize = 100;
	@DocProperty(description = "Boolean value to indicate if Local Cache is to be used")
    private boolean useLocalCache;
	@DocProperty(description = "Boolean value specifying whether to enable JDK Loggers")
    private boolean disableJdkLogger = true;
	@DocProperty(description = "Logging level for Fido2 logger")
    private String loggingLevel;
	@DocProperty(description = "Logging layout used for Fido2")
    private String loggingLayout;
	@DocProperty(description = "Path to external Fido2 logging configuration")
    private String externalLoggerConfiguration;
	@DocProperty(description = "The interval for metric reporter in seconds")
    private int metricReporterInterval;
	@DocProperty(description = "The days to keep report data")
    private int metricReporterKeepDataDays;
	@DocProperty(description = "Boolean value specifying whether metric reporter is enabled")
    private boolean metricReporterEnabled = true;
	@DocProperty(description = "Custom object class list for dynamic person enrolment")
    private List<String> personCustomObjectClassList;
	@DocProperty(description = "Choose if application should update oxLastLogonTime attribute upon user authentication")
	private Boolean updateUserLastLogonTime;
	@DocProperty(description = "Authentication Brute Force Protection Configuration")
	private AuthenticationProtectionConfiguration authenticationProtectionConfiguration;
	@DocProperty(description = "The lifetime for unused session states")
	private int sessionIdUnusedLifetime;
	@DocProperty(description = "The lifetime of session id in seconds. If 0 or -1 then expiration is not set. session_id cookie expires when browser session ends")
	private Integer sessionIdLifetime = DEFAULT_SESSION_ID_LIFETIME;
	@DocProperty(description = "Dedicated property to control lifetime of the server side OP session object in seconds. Overrides sessionIdLifetime. By default value is 0, so object lifetime equals sessionIdLifetime (which sets both cookie and object expiration). It can be useful if goal is to keep different values for client cookie and server object")
	private Integer serverSessionIdLifetime = sessionIdLifetime; // by default same as sessionIdLifetime
	@DocProperty(description = "The lifetime for unused unauthenticated session states")
	private int sessionIdUnauthenticatedUnusedLifetime = 120; // 120 seconds
	@DocProperty(description = "Boolean value specifying whether to persist session_id in cache", defaultValue = "false")
	private Boolean sessionIdPersistInCache = false;
	@DocProperty(description = "Boolean value specifying whether to persist session ID on prompt none")
	private Boolean sessionIdPersistOnPromptNone;
	@DocProperty(description = "Boolean value specifying whether to return detailed reason of the error from AS. Default value is false", defaultValue = "false")
	private Boolean errorReasonEnabled = false;
	@DocProperty(description = "List of enabled feature flags")
	private List<String> featureFlags;
	@DocProperty(description = "Boolean value to enable disable Super Gluu extension")
	private boolean useSuperGluu;
    private Fido2Configuration fido2Configuration;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getBaseEndpoint() {
		return baseEndpoint;
	}

	public void setBaseEndpoint(String baseEndpoint) {
		this.baseEndpoint = baseEndpoint;
	}

	public int getCleanServiceInterval() {
		return cleanServiceInterval;
	}

	public void setCleanServiceInterval(int cleanServiceInterval) {
		this.cleanServiceInterval = cleanServiceInterval;
	}

	public int getCleanServiceBatchChunkSize() {
		return cleanServiceBatchChunkSize;
	}

	public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
		this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
	}

	public boolean isUseLocalCache() {
		return useLocalCache;
	}

	public void setUseLocalCache(boolean useLocalCache) {
		this.useLocalCache = useLocalCache;
	}

	public Boolean getDisableJdkLogger() {
		return disableJdkLogger;
	}

	public void setDisableJdkLogger(Boolean disableJdkLogger) {
		this.disableJdkLogger = disableJdkLogger;
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

	public String getExternalLoggerConfiguration() {
		return externalLoggerConfiguration;
	}

	public void setExternalLoggerConfiguration(String externalLoggerConfiguration) {
		this.externalLoggerConfiguration = externalLoggerConfiguration;
	}

	public int getMetricReporterInterval() {
		return metricReporterInterval;
	}

	public void setMetricReporterInterval(int metricReporterInterval) {
		this.metricReporterInterval = metricReporterInterval;
	}

	public int getMetricReporterKeepDataDays() {
		return metricReporterKeepDataDays;
	}

	public void setMetricReporterKeepDataDays(int metricReporterKeepDataDays) {
		this.metricReporterKeepDataDays = metricReporterKeepDataDays;
	}

	public Boolean getMetricReporterEnabled() {
		return metricReporterEnabled;
	}

	public void setMetricReporterEnabled(Boolean metricReporterEnabled) {
		this.metricReporterEnabled = metricReporterEnabled;
	}

	public List<String> getPersonCustomObjectClassList() {
		return personCustomObjectClassList;
	}

	public void setPersonCustomObjectClassList(List<String> personCustomObjectClassList) {
		this.personCustomObjectClassList = personCustomObjectClassList;
	}

	public Fido2Configuration getFido2Configuration() {
		return fido2Configuration;
	}

	public void setFido2Configuration(Fido2Configuration fido2Configuration) {
		this.fido2Configuration = fido2Configuration;
	}

	public Boolean getUpdateUserLastLogonTime() {
		return updateUserLastLogonTime != null && updateUserLastLogonTime;
	}

	public void setUpdateUserLastLogonTime(Boolean updateUserLastLogonTime) {
		this.updateUserLastLogonTime = updateUserLastLogonTime;
	}

	public AuthenticationProtectionConfiguration getAuthenticationProtectionConfiguration() {
		return authenticationProtectionConfiguration;
	}

	public void setAuthenticationProtectionConfiguration(AuthenticationProtectionConfiguration authenticationProtectionConfiguration) {
		this.authenticationProtectionConfiguration = authenticationProtectionConfiguration;
	}

	public int getSessionIdUnusedLifetime() {
		return sessionIdUnusedLifetime;
	}

	public void setSessionIdUnusedLifetime(int sessionIdUnusedLifetime) {
		this.sessionIdUnusedLifetime = sessionIdUnusedLifetime;
	}
	public Integer getSessionIdLifetime() {
		return sessionIdLifetime;
	}

	public void setSessionIdLifetime(Integer sessionIdLifetime) {
		this.sessionIdLifetime = sessionIdLifetime;
	}
	public Integer getServerSessionIdLifetime() {
		return serverSessionIdLifetime;
	}

	public void setServerSessionIdLifetime(Integer serverSessionIdLifetime) {
		this.serverSessionIdLifetime = serverSessionIdLifetime;
	}
	public int getSessionIdUnauthenticatedUnusedLifetime() {
		return sessionIdUnauthenticatedUnusedLifetime;
	}

	public void setSessionIdUnauthenticatedUnusedLifetime(int sessionIdUnauthenticatedUnusedLifetime) {
		this.sessionIdUnauthenticatedUnusedLifetime = sessionIdUnauthenticatedUnusedLifetime;
	}
	public Boolean getSessionIdPersistInCache() {
		if (sessionIdPersistInCache == null) sessionIdPersistInCache = false;
		return sessionIdPersistInCache;
	}

	public void setSessionIdPersistInCache(Boolean sessionIdPersistInCache) {
		this.sessionIdPersistInCache = sessionIdPersistInCache;
	}

	public Boolean getSessionIdPersistOnPromptNone() {
		return sessionIdPersistOnPromptNone;
	}

	public void setSessionIdPersistOnPromptNone(Boolean sessionIdPersistOnPromptNone) {
		this.sessionIdPersistOnPromptNone = sessionIdPersistOnPromptNone;
	}

	public Boolean getErrorReasonEnabled() {
		if (errorReasonEnabled == null) errorReasonEnabled = false;
		return errorReasonEnabled;
	}

	public void setErrorReasonEnabled(Boolean errorReasonEnabled) {
		this.errorReasonEnabled = errorReasonEnabled;
	}
	public Set<FeatureFlagType> getEnabledFeatureFlags() {
		return FeatureFlagType.fromValues(getFeatureFlags());
	}

	public boolean isFeatureEnabled(FeatureFlagType flagType) {
		final Set<FeatureFlagType> flags = getEnabledFeatureFlags();
		if (flags.isEmpty())
			return true;

		return flags.contains(flagType);
	}

	public List<String> getFeatureFlags() {
		if (featureFlags == null) featureFlags = new ArrayList<>();
		return featureFlags;
	}

	public void setFeatureFlags(List<String> featureFlags) {
		this.featureFlags = featureFlags;
	}

	public boolean isUseSuperGluu() {
		return useSuperGluu;
	}

	public void setUseSuperGluu(boolean useSuperGluu) {
		this.useSuperGluu = useSuperGluu;
	}

}
