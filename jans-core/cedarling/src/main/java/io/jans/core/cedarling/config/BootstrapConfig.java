/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.core.cedarling.config;

import java.util.Arrays;

/**
 * Cedarling BootstrapConfig builder
 *
 */
import org.json.JSONArray;
import org.json.JSONObject;
import io.jans.core.cedarling.model.*;

/**
 * Configuration class for Cedarling initialization using Enums for Logging.
 *
 * @author Author Date: 12/05/2026
 */
public class BootstrapConfig {

    // Configuration Keys
    public static final String CEDARLING_APPLICATION_NAME = "CEDARLING_APPLICATION_NAME";

    public static final String CEDARLING_POLICY_STORE_URI = "CEDARLING_POLICY_STORE_URI";
    
    public static final String CEDARLING_LOG_TYPE = "CEDARLING_LOG_TYPE";
    public static final String CEDARLING_LOG_LEVEL = "CEDARLING_LOG_LEVEL";
    public static final String CEDARLING_LOG_TTL = "CEDARLING_LOG_TTL";
    public static final String CEDARLING_LOCAL_JWKS = "CEDARLING_LOCAL_JWKS";

    public static final String CEDARLING_POLICY_STORE_LOCAL = "CEDARLING_POLICY_STORE_LOCAL";
    public static final String CEDARLING_POLICY_STORE_LOCAL_FN = "CEDARLING_POLICY_STORE_LOCAL_FN";
    public static final String CEDARLING_JWT_SIG_VALIDATION = "CEDARLING_JWT_SIG_VALIDATION";
    public static final String CEDARLING_JWT_STATUS_VALIDATION = "CEDARLING_JWT_STATUS_VALIDATION";
    public static final String CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED = "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED";

    public static final String CEDARLING_SERVER_CONFIGURATION_URI = "CEDARLING_SERVER_CONFIGURATION_URI";
    public static final String CEDARLING_TRANSPORT = "CEDARLING_TRANSPORT";
    public static final String CEDARLING_SSA_JWT = "CEDARLING_SSA_JWT";
    public static final String CEDARLING_ACCESS_TOKEN_JWT = "CEDARLING_ACCESS_TOKEN_JWT";
    
   
    public static final String CEDARLING_MAX_DEFAULT_ENTITIES = "CEDARLING_MAX_DEFAULT_ENTITIES";
    public static final String CEDARLING_MAX_BASE64_SIZE = "CEDARLING_MAX_BASE64_SIZE";

    // Variables
    private String applicationName;

    private String policyStoreUri;

    private LogType logType;
    private LogLevel logLevel;
    private int logTtl;
    private String localJwks;
    
    private String policyStoreLocal;
    private String policyStoreLocalFn;
    private boolean jwtSigValidation;
    private boolean jwtStatusValidation;
    private String[] jwtSignatureAlgorithmsSupported;

    private int maxDefaultEntities;
    private long maxBase64Size;

    private BootstrapConfig() {}

    private BootstrapConfig(Builder builder) {
        this.applicationName = builder.applicationName;
        this.policyStoreUri = builder.policyStoreUri;
        this.logType = builder.logType;
        this.logLevel = builder.logLevel;
        this.logTtl = builder.logTtl;
        this.localJwks = builder.localJwks;
        this.policyStoreLocal = builder.policyStoreLocal;
        this.policyStoreLocalFn = builder.policyStoreLocalFn;
        this.jwtSigValidation = builder.jwtSigValidation;
        this.jwtStatusValidation = builder.jwtStatusValidation;
        this.jwtSignatureAlgorithmsSupported = builder.jwtSignatureAlgorithmsSupported;
    
        this.maxDefaultEntities = builder.maxDefaultEntities;
        this.maxBase64Size = builder.maxBase64Size;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts configuration to JSON string for Cedarling initialization.
     */
    public String toJsonConfig() {
        JSONObject jo = new JSONObject();
        jo.put(CEDARLING_APPLICATION_NAME, applicationName);
        jo.put(CEDARLING_POLICY_STORE_URI, policyStoreUri);
        
        // Extract string values from Enums
        jo.put(CEDARLING_LOG_TYPE, logType.getType());
        jo.put(CEDARLING_LOG_LEVEL, logLevel.getType());
        
        jo.put(CEDARLING_LOG_TTL, logTtl);
        jo.put(CEDARLING_LOCAL_JWKS, localJwks);
        
        jo.put(CEDARLING_POLICY_STORE_LOCAL, policyStoreLocal);
        jo.put(CEDARLING_POLICY_STORE_LOCAL_FN, policyStoreLocalFn);
        jo.put(CEDARLING_JWT_SIG_VALIDATION, toEnabled(jwtSigValidation));
        jo.put(CEDARLING_JWT_STATUS_VALIDATION, toEnabled(jwtStatusValidation));
        jo.put(CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED, new JSONArray(Arrays.asList(jwtSignatureAlgorithmsSupported)));

        jo.put(CEDARLING_MAX_DEFAULT_ENTITIES, maxDefaultEntities);
        jo.put(CEDARLING_MAX_BASE64_SIZE, maxBase64Size);

        return jo.toString();
    }

    private String toEnabled(boolean value) {
        return value ? "enabled" : "disabled";
    }

    /**
     * Builder class for BootstrapConfig.
     */
    public static class Builder {
		private String applicationName = "App";
        private String policyStoreUri = "";
        private LogType logType = LogType.MEMORY;
        private LogLevel logLevel = LogLevel.DEBUG;
        private int logTtl = 60;
        private String localJwks = null;
        private String policyStoreLocal = null;
        private String policyStoreLocalFn = "";
        private boolean jwtSigValidation = true;
        private boolean jwtStatusValidation = false;
        private String[] jwtSignatureAlgorithmsSupported = {"HS256", "RS256"};

        private int maxDefaultEntities = 1000;
        private long maxBase64Size = 1048576L;

        protected Builder() {}

        public Builder applicationName(String applicationName) { this.applicationName = applicationName; return this; }
        public Builder policyStoreUri(String policyStoreUri) { this.policyStoreUri = policyStoreUri; return this; }
        public Builder logType(LogType logType) { this.logType = logType; return this; }
        public Builder logLevel(LogLevel logLevel) { this.logLevel = logLevel; return this; }
        public Builder logTtl(int logTtl) { this.logTtl = logTtl; return this; }
        public Builder localJwks(String localJwks) { this.localJwks = localJwks; return this; }
        public Builder policyStoreLocal(String policyStoreLocal) { this.policyStoreLocal = policyStoreLocal; return this; }
        public Builder policyStoreLocalFn(String policyStoreLocalFn) { this.policyStoreLocalFn = policyStoreLocalFn; return this; }
        public Builder jwtSigValidation(boolean jwtSigValidation) { this.jwtSigValidation = jwtSigValidation; return this; }
        public Builder jwtStatusValidation(boolean jwtStatusValidation) { this.jwtStatusValidation = jwtStatusValidation; return this; }
        public Builder jwtSignatureAlgorithmsSupported(String[] algorithms) { this.jwtSignatureAlgorithmsSupported = algorithms; return this; }
        
        public Builder maxDefaultEntities(int max) { this.maxDefaultEntities = max; return this; }
        public Builder maxBase64Size(long size) { this.maxBase64Size = size; return this; }

        public BootstrapConfig build() {
            return new BootstrapConfig(this);
        }
    }
}