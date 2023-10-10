package io.jans.ca.plugin.adminui.model.auth;

public class DCRResponse {
    private String clientId;
    private String clientSecret;
    private String opHost;
    private String scanHostname;
    private String hardwareId;

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getScanHostname() {
        return scanHostname;
    }

    public void setScanHostname(String scanHostname) {
        this.scanHostname = scanHostname;
    }
}
