/*
 * Copyright [2025] [Janssen Project]
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
