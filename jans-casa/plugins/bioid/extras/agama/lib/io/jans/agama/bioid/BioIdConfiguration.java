package io.jans.agama.bioid;

public class BioIdConfiguration {

    private String endpoint;
    private String appIdentifier;
    private String appSecret;
    private String storage;
    private String partition;
    private String host;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAppIdentifier() {
        return appIdentifier;
    }

    public void setAppIdentifier(String appIdentifier) {
        this.appIdentifier = appIdentifier;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
