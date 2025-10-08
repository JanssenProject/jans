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

import io.jans.lock.model.config.cedarling.LogLevel;
import io.jans.lock.model.config.cedarling.LogType;

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
	public static final String CEDARLING_POLICY_STORE_LOCAL_ID = "CEDARLING_POLICY_STORE_LOCAL_ID";
	public static final String CEDARLING_USER_AUTHZ = "CEDARLING_USER_AUTHZ";
	public static final String CEDARLING_WORKLOAD_AUTHZ = "CEDARLING_WORKLOAD_AUTHZ";

	public static final String CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS = "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS";
	
	public static final String CEDARLING_PRINCIPAL_BOOLEAN_OPERATION = "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION";

	public static final String CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED";
	public static final String CEDARLING_JWT_SIG_VALIDATION = "CEDARLING_JWT_SIG_VALIDATION";
	public static final String CEDARLING_JWT_STATUS_VALIDATION = "CEDARLING_JWT_STATUS_VALIDATION";

	public static final String CEDARLING_ID_TOKEN_TRUST_MODE = "CEDARLING_ID_TOKEN_TRUST_MODE";

	public static final String CEDARLING_LOCK = "CEDARLING_LOCK";

	private String applicationName;
	private String policyStoreLocal;
	private boolean userAuthz;
	private boolean workloadAuthz;
	private String idTokenTrustMode;
	private LogType logType;
	private LogLevel logLevel;
	private Integer logTtl;

	private String decisionLogWorkloadClaims;
	private String principalBooleanOperation;
	
	private String jwtSignatureAlgorithmsSupported;
	private boolean jwtSigValidation;
	private boolean jwtStatusValidation;

	private boolean lockEnabled;

	private BootstrapConfig() {
	}

	private BootstrapConfig(Builder builder) {
		this.applicationName = builder.applicationName;
		this.policyStoreLocal = builder.policyStoreLocal;
		this.userAuthz = builder.userAuthz;
		this.workloadAuthz = builder.workloadAuthz;
		this.idTokenTrustMode = builder.idTokenTrustMode;
		this.logType = builder.logType;
		this.logLevel = builder.logLevel;
		this.logTtl = builder.logTtl;

		this.decisionLogWorkloadClaims = builder.decisionLogWorkloadClaims;
		this.principalBooleanOperation = builder.principalBooleanOperation;

		this.jwtSignatureAlgorithmsSupported = builder.jwtSignatureAlgorithmsSupported;
		this.jwtSigValidation = builder.jwtSigValidation;
		this.jwtStatusValidation = builder.jwtStatusValidation;

		this.lockEnabled = builder.lockEnabled;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String toJsonConfig() {
		JSONObject jo = new JSONObject();
		jo.put(CEDARLING_APPLICATION_NAME, applicationName);

		jo.put(CEDARLING_WORKLOAD_AUTHZ, toEnabled(workloadAuthz));
		jo.put(CEDARLING_USER_AUTHZ, toEnabled(userAuthz));

		jo.put(CEDARLING_AUDIT_HEALTH_INTERVAL, 0);
		jo.put(CEDARLING_AUDIT_TELEMETRY_INTERVAL, 0);

		jo.put(CEDARLING_LOG_TYPE, logType.getType());
		jo.put(CEDARLING_LOG_LEVEL, logLevel);
		jo.put(CEDARLING_LOG_TTL, logTtl);

		jo.put(CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS, decisionLogWorkloadClaims);

		jo.put(CEDARLING_PRINCIPAL_BOOLEAN_OPERATION, principalBooleanOperation);

		jo.put(CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED, jwtSignatureAlgorithmsSupported);
		jo.put(CEDARLING_JWT_SIG_VALIDATION, toEnabled(jwtSigValidation));
		jo.put(CEDARLING_JWT_STATUS_VALIDATION, toEnabled(jwtStatusValidation));

		jo.put(CEDARLING_ID_TOKEN_TRUST_MODE, idTokenTrustMode);

		jo.put(CEDARLING_LOCK, toEnabled(lockEnabled));
		
		jo.put(CEDARLING_POLICY_STORE_LOCAL_ID, "lock-server-policy-id");
		jo.put(CEDARLING_POLICY_STORE_LOCAL, policyStoreLocal);

		return jo.toString();
	}

	private String toEnabled(boolean value) {
		return value ? "enabled" : "disabled";
	}

	public static class Builder {
		private String applicationName = "Lock Server";
		private String policyStoreLocal = null;
		private boolean userAuthz = false;
		private boolean workloadAuthz = true;
		private String idTokenTrustMode = "never";
		private LogType logType = LogType.MEMORY;
		private LogLevel logLevel = LogLevel.DEBUG;
		private Integer logTtl;

		private String decisionLogWorkloadClaims = "[\"client_id\", \"rp_id\"]";
		private String principalBooleanOperation = "{\"===\": [{\"var\": \"Jans::Workload\"}, \"ALLOW\"]}";
		
		private String jwtSignatureAlgorithmsSupported = "[\"HS256\", \"RS256\"]";
		private boolean jwtSigValidation = false;
		private boolean jwtStatusValidation = false;

		private boolean lockEnabled = false;

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

		public Builder idTokenTrustMode(String idTokenTrustMode) {
			this.idTokenTrustMode = idTokenTrustMode;
			return this;
		}

		public Builder logType(LogType logType) {
			this.logType = logType;
			return this;
		}

		public Builder logLevel(LogLevel logLevel) {
			this.logLevel = logLevel;
			return this;
		}

		public Builder decisionLogWorkloadClaims(String decisionLogWorkloadClaims) {
			this.decisionLogWorkloadClaims = decisionLogWorkloadClaims;
			return this;
		}

		public Builder principalBooleanOperation(String principalBooleanOperation) {
			this.principalBooleanOperation = principalBooleanOperation;
			return this;
		}

		public Builder jwtSignatureAlgorithmsSupported(String jwtSignatureAlgorithmsSupported) {
			this.jwtSignatureAlgorithmsSupported = jwtSignatureAlgorithmsSupported;
			return this;
		}

		public Builder jwtSigValidation(boolean jwtSigValidation) {
			this.jwtSigValidation = jwtSigValidation;
			return this;
		}

		public Builder jwtStatusValidation(boolean jwtStatusValidation) {
			this.jwtStatusValidation = jwtStatusValidation;
			return this;
		}

		public Builder lockEnabled(boolean lockEnabled) {
			this.lockEnabled = lockEnabled;
			return this;
		}

		public BootstrapConfig build() {
			return new BootstrapConfig(this);
		}
	}

}