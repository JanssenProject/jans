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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
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

    @DocProperty(description = "Jans URL of the OpenID Connect Provider's OAuth 2.0 Token Endpoint")
    @Schema(description = "Jans URL of the OpenID Connect Provider's OAuth 2.0 Token Endpoint")
    private String tokenUrl;
    
    @DocProperty(description = "Group scope enabled")
    @Schema(description = "Group scope enabled")
    private boolean groupScopeEnabled;

    @DocProperty(description = "Endpoint groups")
    @Schema(description = "Endpoint groups")
    private Map<String, List<String>> endpointGroups;

    @DocProperty(description = "Jans URL of config-api audit endpoints and corresponding scope details")
    @Schema(description = "Jans URL of config-api audit endpoints and corresponding scope details")
    private Map<String, List<String>> endpointDetails;

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

    @DocProperty(description = "Channel for metric reports", defaultValue = "jans_pdp_metric")
    @Schema(description = "Channel for metric reports")
    private String metricChannel;

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

    @Schema(description = "Opa Configuration")
    private OpaConfiguration opaConfiguration;

    @DocProperty(description = "PDP type")
    @Schema(description = "PDP type")
    private String pdpType;

    @DocProperty(description = "Authorization token to access Json Uris")
    @Schema(description = "Authorization token to access Json Uris")
    private String policiesJsonUrisAuthorizationToken;

    @DocProperty(description = "List of Json Uris with link to Rego policies")
    @Schema(description = "List of Json Uris with link to Rego policies")
    private List<String> policiesJsonUris;

    @DocProperty(description = "Authorization token to access Zip Uris")
    @Schema(description = "Authorization token to access Zip Uris")
    private String policiesZipUrisAuthorizationToken;

    @DocProperty(description = "List of Zip Uris with policies")
    @Schema(description = "List of Zip Uris with policies")
    private List<String> policiesZipUris;

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

    public void setClientPassword(String clientPassword) {
        this.clientPassword = clientPassword;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
    
    public boolean isGroupScopeEnabled() {
        return groupScopeEnabled;
    }

    public void setGroupScopeEnabled(boolean groupScopeEnabled) {
        this.groupScopeEnabled = groupScopeEnabled;
    }

    public Map<String, List<String>> getEndpointGroups() {
        return endpointGroups;
    }

    public void setEndpointGroups(Map<String, List<String>> endpointGroups) {
        this.endpointGroups = endpointGroups;
    }

    public Map<String, List<String>> getEndpointDetails() {
        return endpointDetails;
    }

    public void setEndpointDetails(Map<String, List<String>> endpointDetails) {
        this.endpointDetails = endpointDetails;
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

    public String getMetricChannel() {
        return metricChannel;
    }

    public void setMetricChannel(String metricChannel) {
        this.metricChannel = metricChannel;
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

    public OpaConfiguration getOpaConfiguration() {
        return opaConfiguration;
    }

    public void setOpaConfiguration(OpaConfiguration opaConfiguration) {
        this.opaConfiguration = opaConfiguration;
    }

    public String getPdpType() {
        return pdpType;
    }

    public void setPdpType(String pdpType) {
        this.pdpType = pdpType;
    }

    public String getPoliciesJsonUrisAuthorizationToken() {
        return policiesJsonUrisAuthorizationToken;
    }

    public void setPoliciesJsonUrisAuthorizationToken(String policiesJsonUrisAuthorizationToken) {
        this.policiesJsonUrisAuthorizationToken = policiesJsonUrisAuthorizationToken;
    }

    public List<String> getPoliciesJsonUris() {
        return policiesJsonUris;
    }

    public void setPoliciesJsonUris(List<String> policiesJsonUris) {
        this.policiesJsonUris = policiesJsonUris;
    }

    public String getPoliciesZipUrisAuthorizationToken() {
        return policiesZipUrisAuthorizationToken;
    }

    public void setPoliciesZipUrisAuthorizationToken(String policiesZipUrisAuthorizationToken) {
        this.policiesZipUrisAuthorizationToken = policiesZipUrisAuthorizationToken;
    }

    public List<String> getPoliciesZipUris() {
        return policiesZipUris;
    }

    public void setPoliciesZipUris(List<String> policiesZipUris) {
        this.policiesZipUris = policiesZipUris;
    }

    @Override
	public String toString() {
		return "AppConfiguration [baseDN=" + baseDN + ", baseEndpoint=" + baseEndpoint + ", openIdIssuer="
				+ openIdIssuer + ", statEnabled=" + statEnabled + ", statTimerIntervalInSeconds="
				+ statTimerIntervalInSeconds + ", tokenChannels=" + tokenChannels + ", clientId=" + clientId
				+ ", clientPassword=" + clientPassword + ", tokenUrl=" + tokenUrl + ", groupScopeEnabled="
				+ groupScopeEnabled + ", endpointGroups=" + endpointGroups + ", endpointDetails=" + endpointDetails
				+ ", disableJdkLogger=" + disableJdkLogger + ", loggingLevel=" + loggingLevel + ", loggingLayout="
				+ loggingLayout + ", externalLoggerConfiguration=" + externalLoggerConfiguration + ", metricChannel="
				+ metricChannel + ", metricReporterInterval=" + metricReporterInterval + ", metricReporterKeepDataDays="
				+ metricReporterKeepDataDays + ", metricReporterEnabled=" + metricReporterEnabled
				+ ", cleanServiceInterval=" + cleanServiceInterval + ", opaConfiguration=" + opaConfiguration
				+ ", pdpType=" + pdpType + ", policiesJsonUrisAuthorizationToken=" + policiesJsonUrisAuthorizationToken
				+ ", policiesJsonUris=" + policiesJsonUris + ", policiesZipUrisAuthorizationToken="
				+ policiesZipUrisAuthorizationToken + ", policiesZipUris=" + policiesZipUris + "]";
	}

}
