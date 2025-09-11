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

package io.jans.lock.cedarling.config;

import org.json.JSONObject;

import io.jans.lock.model.config.CedarlingLogLevel;
import io.jans.lock.model.config.CedarlingLogType;

/**
 * Cedarling BootstrapConfig builder
 *
 * @author Yuriy Movchan Date: 12/18/2025
 */
public class BootstrapConfig {

	public static final String CEDARLING_APPLICATION_NAME = "CEDARLING_APPLICATION_NAME";
	public static final String CEDARLING_LOG_TYPE = "CEDARLING_LOG_TYPE";
	public static final String CEDARLING_LOG_LEVEL = "CEDARLING_LOG_LEVEL";
	public static final String CEDARLING_LOG_TTL = "CEDARLING_LOG_TTL";
	public static final String CEDARLING_AUDIT_TELEMETRY_INTERVAL = "CEDARLING_AUDIT_TELEMETRY_INTERVAL";
	public static final String CEDARLING_AUDIT_HEALTH_INTERVAL = "CEDARLING_AUDIT_HEALTH_INTERVAL";
	public static final String CEDARLING_POLICY_STORE_LOCAL = "CEDARLING_POLICY_STORE_LOCAL";
	public static final String CEDARLING_USER_AUTHZ = "CEDARLING_USER_AUTHZ";
	public static final String CEDARLING_WORKLOAD_AUTHZ = "CEDARLING_WORKLOAD_AUTHZ";

	private String applicationName = "Lock Server";
	private String policyStoreLocal = null;
	private boolean userAuthz = false;
	private boolean workloadAuthz = true;
	private CedarlingLogType logType = CedarlingLogType.MEMORY;
	private CedarlingLogLevel logLevel = CedarlingLogLevel.DEBUG;
	private Integer logTtl;

	private BootstrapConfig() {
	}

	private BootstrapConfig(Builder builder) {
		this.applicationName = builder.applicationName;
		this.policyStoreLocal = builder.policyStoreLocal;
		this.userAuthz = builder.userAuthz;
		this.workloadAuthz = builder.workloadAuthz;
		this.logType = builder.logType;
		this.logLevel = builder.logLevel;
		this.logTtl = 120;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String toJsonConfig() {
		JSONObject jo = new JSONObject();
		jo.put(CEDARLING_APPLICATION_NAME, applicationName);
		jo.put(CEDARLING_WORKLOAD_AUTHZ, workloadAuthz);
		jo.put(CEDARLING_USER_AUTHZ, userAuthz);
		jo.put(CEDARLING_POLICY_STORE_LOCAL, policyStoreLocal);
		jo.put(CEDARLING_AUDIT_HEALTH_INTERVAL, 0);
		jo.put(CEDARLING_AUDIT_TELEMETRY_INTERVAL, 0);
		jo.put(CEDARLING_LOG_TYPE, logType);
		jo.put(CEDARLING_LOG_LEVEL, logLevel);
		jo.put(CEDARLING_LOG_TTL, logTtl);

		return jo.toString();
	}

	public static class Builder {
		private String applicationName = "Lock Server";
		private String policyStoreLocal = null;
		private boolean userAuthz = false;
		private boolean workloadAuthz = true;
		private CedarlingLogType logType = CedarlingLogType.MEMORY;
		private CedarlingLogLevel logLevel = CedarlingLogLevel.DEBUG;

		protected Builder() {
		}

		public Builder applicationName(String applicationName) {
			this.applicationName = applicationName;
			return this;
		}

		public Builder policyStoreLocal(String policyStoreLocal) {
			this.policyStoreLocal = policyStoreLocal;
			return this;
		}

		public Builder userAuthz(boolean userAuthz) {
			this.userAuthz = userAuthz;
			return this;
		}

		public Builder workloadAuthz(boolean workloadAuthz) {
			this.workloadAuthz = workloadAuthz;
			return this;
		}

		public Builder logType(CedarlingLogType logType) {
			this.logType = logType;
			return this;
		}

		public Builder logLevel(CedarlingLogLevel logLevel) {
			this.logLevel = logLevel;
			return this;
		}

		public BootstrapConfig build() {
			return new BootstrapConfig(this);
		}
	}

}