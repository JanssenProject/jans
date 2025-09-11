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

package io.jans.lock.model.config;

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

	@DocProperty(description = "JSON object as string with policy store")
	@Schema(description = "JSON object as string with policy store")
	private String policyStoreLocal;

	@DocProperty(description = "Log type: off, memory, std_out", defaultValue = "std_out")
	@Schema(description = "Log type: off, memory, std_out")
	private CedarlingLogType logType = CedarlingLogType.STD_OUT;

	@DocProperty(description = "System Log Level: FATAL, ERROR, WARN, INFO, DEBUG, TRACE", defaultValue = "INFO")
	@Schema(description = "System Log Level")
	private CedarlingLogLevel logLevel = CedarlingLogLevel.INFO;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPolicyStoreLocal() {
		return policyStoreLocal;
	}

	public void setPolicyStoreLocal(String policyStoreLocal) {
		this.policyStoreLocal = policyStoreLocal;
	}

	public CedarlingLogType getLogType() {
		return logType;
	}

	public void setLogType(CedarlingLogType logType) {
		this.logType = logType;
	}

	public CedarlingLogLevel getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(CedarlingLogLevel logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public String toString() {
		return "CedarlingConfiguration [policyStoreLocal=" + policyStoreLocal + ", logType=" + logType + ", logLevel="
				+ logLevel + "]";
	}

}
