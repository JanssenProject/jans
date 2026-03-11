/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.model.config.cedarling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 
 * @author Yuriy Movchan Date: 10/08/2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CedarlingConfiguration {

	@DocProperty(description = "Specify if Cedraling is enabled", defaultValue = "false")
	@Schema(description = "Specify if Cedraling is enabled")
	private boolean enabled = false;

	@DocProperty(description = "List of Policy Sources")
    @Schema(description = "List of Policy Sources")
    private List<PolicySource> policySources;

	@DocProperty(description = "Log type: off, memory, std_out", defaultValue = "std_out")
	@Schema(description = "Log type: off, memory, std_out")
	private LogType logType = LogType.STD_OUT;

	@DocProperty(description = "System Log Level: FATAL, ERROR, WARN, INFO, DEBUG, TRACE", defaultValue = "INFO")
	@Schema(description = "System Log Level")
	private LogLevel logLevel = LogLevel.INFO;

	@DocProperty(description = "External policy store URI")
	@Schema(description = "External policy store URI")
	private String externalPolicyStoreUri;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<PolicySource> getPolicySources() {
		return policySources;
	}

	public void setPolicySources(List<PolicySource> policySources) {
		this.policySources = policySources;
	}

	public LogType getLogType() {
		return logType;
	}

	public void setLogType(LogType logType) {
		this.logType = logType;
	}

	public LogLevel getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public String getExternalPolicyStoreUri() {
		return externalPolicyStoreUri;
	}

	public void setExternalPolicyStoreUri(String externalPolicyStoreUri) {
		this.externalPolicyStoreUri = externalPolicyStoreUri;
	}

	@Override
	public String toString() {
		return "CedarlingConfiguration [enabled=" + enabled + ", policySources=" + policySources + ", logType="
				+ logType + ", logLevel=" + logLevel + ", externalPolicyStoreUri=" + externalPolicyStoreUri + "]";
	}

}
