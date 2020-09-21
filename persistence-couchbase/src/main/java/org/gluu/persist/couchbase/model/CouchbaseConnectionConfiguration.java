package org.gluu.persist.couchbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchbaseConnectionConfiguration {

    private String userName;
    private String userPassword;
    private List<String> servers;
    private String defaultBucket;
    private List<String> buckets;
    private String passwordEncryptionMethod;
    private Boolean operationTracingEnabled;
    private Boolean mutationTokensEnabled;
    private int connectTimeout;
    private int computationPoolSize;
    private Boolean useSSL;
    private String sslTrustStoreFile;
    private String sslTrustStorePin;
    private String sslTrustStoreFormat;
    private List<String> binaryAttributes;
    private List<String> certificateAttributes;

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

    public Boolean getOperationTracingEnabled() {
        return operationTracingEnabled;
    }

    public void setOperationTracingEnabled(Boolean operationTracingEnabled) {
        this.operationTracingEnabled = operationTracingEnabled;
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

    public int getComputationPoolSize() {
        return computationPoolSize;
    }

    public void setComputationPoolSize(int computationPoolSize) {
        this.computationPoolSize = computationPoolSize;
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

    public String getSslTrustStoreFormat() {
        return sslTrustStoreFormat;
    }

    public void setSslTrustStoreFormat(String sslTrustStoreFormat) {
        this.sslTrustStoreFormat = sslTrustStoreFormat;
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
