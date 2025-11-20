/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.jans.lock.model.config.cedarling.CedarlingConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.enterprise.inject.Vetoed;

/**
 * Janssen Project configuration
 *
 * @author Yuriy Movchan Date: 12/12/2023
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfiguration implements Configuration {

    @DocProperty(description = "Entry Base distinguished name (DN) that identifies the starting point of a search")
    @Schema(description = "Entry Base distinguished name (DN) that identifies the starting point of a search")
    private String baseDN;

    @DocProperty(description = "Lock base endpoint URL")
    @Schema(description = "Lock base endpoint URL")
    private String baseEndpoint;

    @DocProperty(description = "OpenID issuer URL")
    @Schema(description = "OpenID issuer URL")
    private String openIdIssuer;

    @DocProperty(description = "Protection mode for the Lock server (OAuth or Cedarling)")
    @Schema(description = "Protection mode for the Lock server (OAuth or Cedarling)")
    private LockProtectionMode protectionMode = LockProtectionMode.OAUTH;

    @DocProperty(description = "Audit persistence mode")
    @Schema(description = "Audit persistence mode")
    private AuditPersistenceMode auditPersistenceMode = AuditPersistenceMode.INTERNAL;

    @DocProperty(description = "Cedarling configuration")
    @Schema(description = "Cedarling configuration")
    private CedarlingConfiguration cedarlingConfiguration;

    @DocProperty(description = "Active stat enabled")
    @Schema(description = "Active stat enabled")
    private boolean statEnabled;

    @DocProperty(description = "Statistical data capture time interval")
    @Schema(description = "Statistical data capture time interval")
    private int statTimerIntervalInSeconds;

    @DocProperty(description = "List of token channel names", defaultValue = "jans_token")
    @Schema(description = "List of token channel names")
    private List<String> tokenChannels;

    @DocProperty(description = "Lock Client ID")
    @Schema(description = "Lock Client ID")
    private String clientId;

    @DocProperty(description = "Lock client password")
    @Schema(description = "Lock client password")
    private String clientPassword;

    @DocProperty(description = "Choose whether to disable JDK loggers", defaultValue = "true")
    @Schema(description = "Choose whether to disable JDK loggers")
    private Boolean disableJdkLogger = true;

    @DocProperty(description = "Specify the logging level of loggers")
    @Schema(description = "Specify the logging level of loggers")
    private String loggingLevel;

    @DocProperty(description = "Logging layout used for Jans Authorization Server loggers")
    @Schema(description = "Logging layout used for Jans Authorization Server loggers")
    private String loggingLayout;

    @DocProperty(description = "The path to the external log4j2 logging configuration")
    @Schema(description = "The path to the external log4j2 logging configuration")
    private String externalLoggerConfiguration;

    @DocProperty(description = "The interval for metric reporter in seconds")
    @Schema(description = "The interval for metric reporter in seconds")
    private int metricReporterInterval;

    @DocProperty(description = "The days to keep metric reported data")
    @Schema(description = "The days to keep metric reported data")
    private int metricReporterKeepDataDays;

    @DocProperty(description = "Enable metric reporter")
    @Schema(description = "Enable metric reporter")
    private Boolean metricReporterEnabled;

    // Period in seconds
    @DocProperty(description = "Time interval for the Clean Service in seconds")
    @Schema(description = "Time interval for the Clean Service in seconds")
    private int cleanServiceInterval;

    @DocProperty(description = "PubSub consumer service")
    @Schema(description = "PubSub consumer service")
    private String messageConsumerType;

    @DocProperty(description = "Boolean value specifying whether to return detailed reason of the error from AS. Default value is false", defaultValue = "false")
    private Boolean errorReasonEnabled = false;

    @DocProperty(description = "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage")
    @Schema(description = "Each clean up iteration fetches chunk of expired data per base dn and removes it from storage")
    private int cleanServiceBatchChunkSize;

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    public void setBaseEndpoint(String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    public String getOpenIdIssuer() {
        return openIdIssuer;
    }

    public void setOpenIdIssuer(String openIdIssuer) {
        this.openIdIssuer = openIdIssuer;
    }

    public LockProtectionMode getProtectionMode() {
		return protectionMode;
	}

	/**
	 * Set the lock protection mode.
	 *
	 * @param protectionMode the lock protection mode to use
	 */
	public void setProtectionMode(LockProtectionMode protectionMode) {
		this.protectionMode = protectionMode;
	}

	/**
	 * Retrieves the configured audit persistence mode used for storing audit events.
	 *
	 * @return the configured AuditPersistenceMode; defaults to {@link AuditPersistenceMode#INTERNAL} when not explicitly set.
	 */
	public AuditPersistenceMode getAuditPersistenceMode() {
		return auditPersistenceMode;
	}

	/**
	 * Sets the audit persistence mode for the application configuration.
	 *
	 * @param auditPersistenceMode the audit persistence mode to apply
	 */
	public void setAuditPersistenceMode(AuditPersistenceMode auditPersistenceMode) {
		this.auditPersistenceMode = auditPersistenceMode;
	}

	/**
	 * Accesses the Cedarling configuration for the application.
	 *
	 * @return the CedarlingConfiguration instance for this application, or null if not configured
	 */
	public CedarlingConfiguration getCedarlingConfiguration() {
		return cedarlingConfiguration;
	}

	public void setCedarlingConfiguration(CedarlingConfiguration cedarlingConfiguration) {
		this.cedarlingConfiguration = cedarlingConfiguration;
	}

	public boolean isStatEnabled() {
		return statEnabled;
	}

    public void setStatEnabled(boolean statEnabled) {
        this.statEnabled = statEnabled;
    }

    public int getStatTimerIntervalInSeconds() {
        return statTimerIntervalInSeconds;
    }

    public void setStatTimerIntervalInSeconds(int statTimerIntervalInSeconds) {
        this.statTimerIntervalInSeconds = statTimerIntervalInSeconds;
    }

    public List<String> getTokenChannels() {
        return tokenChannels;
    }

    public void setTokenChannels(List<String> tokenChannels) {
        this.tokenChannels = tokenChannels;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientPassword() {
        return clientPassword;
    }

    /**
     * Sets the Lock client password.
     *
     * @param clientPassword the client password to use for Lock authentication, or {@code null} to unset it
     */
    public void setClientPassword(String clientPassword) {
        this.clientPassword = clientPassword;
    }

    /**
     * Indicates whether JDK loggers are disabled.
     *
     * @return `true` if JDK loggers are disabled, `false` otherwise.
     */
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

    public int getCleanServiceInterval() {
        return cleanServiceInterval;
    }

    public void setCleanServiceInterval(int cleanServiceInterval) {
        this.cleanServiceInterval = cleanServiceInterval;
    }

    public String getMessageConsumerType() {
        if (messageConsumerType == null)
            messageConsumerType = "DISABLED";
        return messageConsumerType;
    }

    public void setMessageConsumerType(String messageConsumerType) {
        this.messageConsumerType = messageConsumerType;
    }

    public Boolean getErrorReasonEnabled() {
        if (errorReasonEnabled == null)
            errorReasonEnabled = false;
        return errorReasonEnabled;
    }

    public void setErrorReasonEnabled(Boolean errorReasonEnabled) {
        this.errorReasonEnabled = errorReasonEnabled;
    }

    public int getCleanServiceBatchChunkSize() {
        return cleanServiceBatchChunkSize;
    }

    /**
     * Sets the number of items processed per base DN in each clean-up iteration.
     *
     * @param cleanServiceBatchChunkSize the chunk size (number of entries) to process per base DN
     */
    public void setCleanServiceBatchChunkSize(int cleanServiceBatchChunkSize) {
        this.cleanServiceBatchChunkSize = cleanServiceBatchChunkSize;
    }

    @Override
	public String toString() {
		return "AppConfiguration [baseDN=" + baseDN + ", baseEndpoint=" + baseEndpoint + ", openIdIssuer="
				+ openIdIssuer + ", protectionMode=" + protectionMode + ", auditPersistenceMode=" + auditPersistenceMode
				+ ", cedarlingConfiguration=" + cedarlingConfiguration + ", statEnabled=" + statEnabled
				+ ", statTimerIntervalInSeconds=" + statTimerIntervalInSeconds + ", tokenChannels=" + tokenChannels
				+ ", clientId=" + clientId + ", clientPassword=" + clientPassword + ", disableJdkLogger="
				+ disableJdkLogger + ", loggingLevel=" + loggingLevel + ", loggingLayout=" + loggingLayout
				+ ", externalLoggerConfiguration=" + externalLoggerConfiguration + ", metricReporterInterval="
				+ metricReporterInterval + ", metricReporterKeepDataDays=" + metricReporterKeepDataDays
				+ ", metricReporterEnabled=" + metricReporterEnabled + ", cleanServiceInterval=" + cleanServiceInterval
				+ ", messageConsumerType=" + messageConsumerType + ", errorReasonEnabled=" + errorReasonEnabled
				+ ", cleanServiceBatchChunkSize=" + cleanServiceBatchChunkSize + "]";
	}

}