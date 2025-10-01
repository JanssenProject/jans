/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchbaseConnectionConfiguration {

    private String configId;
    private String userName;
    private String userPassword;

    private List<String> servers;
    private String defaultBucket;
    private List<String> buckets;
    
    private String passwordEncryptionMethod;
    
    private int connectTimeout;

    private Boolean mutationTokensEnabled;
    private int kvTimeout;
    private int queryTimeout;

    private Boolean useSSL;
    private String sslTrustStoreFile;
    private String sslTrustStorePin;
    private String sslTrustStoreType;
    
    private List<String> binaryAttributes;
    private List<String> certificateAttributes;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public List<String> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<String> buckets) {
        this.buckets = buckets;
    }

    public String getPasswordEncryptionMethod() {
        return passwordEncryptionMethod;
    }

    public void setPasswordEncryptionMethod(String passwordEncryptionMethod) {
        this.passwordEncryptionMethod = passwordEncryptionMethod;
    }

    public Boolean getMutationTokensEnabled() {
        return mutationTokensEnabled;
    }

    public void setMutationTokensEnabled(Boolean mutationTokensEnabled) {
        this.mutationTokensEnabled = mutationTokensEnabled;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getKvTimeout() {
		return kvTimeout;
	}

	public void setKvTimeout(int kvTimeout) {
		this.kvTimeout = kvTimeout;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	public Boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslTrustStoreFile() {
        return sslTrustStoreFile;
    }

    public void setSslTrustStoreFile(String sslTrustStoreFile) {
        this.sslTrustStoreFile = sslTrustStoreFile;
    }

    public String getSslTrustStorePin() {
        return sslTrustStorePin;
    }

    public void setSslTrustStorePin(String sslTrustStorePin) {
        this.sslTrustStorePin = sslTrustStorePin;
    }

    public String getSslTrustStoreType() {
		return sslTrustStoreType;
	}

	public void setSslTrustStoreType(String sslTrustStoreType) {
		this.sslTrustStoreType = sslTrustStoreType;
	}

	public List<String> getBinaryAttributes() {
        return binaryAttributes;
    }

    public void setBinaryAttributes(List<String> binaryAttributes) {
        this.binaryAttributes = binaryAttributes;
    }

    public List<String> getCertificateAttributes() {
        return certificateAttributes;
    }

    public void setCertificateAttributes(List<String> certificateAttributes) {
        this.certificateAttributes = certificateAttributes;
    }
}
