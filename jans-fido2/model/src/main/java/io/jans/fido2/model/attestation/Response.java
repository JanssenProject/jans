package io.jans.fido2.model.attestation;

public class Response {
    private String attestationObject;
    private String clientDataJSON;
    private String clientExtensionResults;
    private String deviceData;

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }

    public String getClientExtensionResults() {
        return clientExtensionResults;
    }

    public void setClientExtensionResults(String clientExtensionResults) {
        this.clientExtensionResults = clientExtensionResults;
    }

    public String getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(String deviceData) {
        this.deviceData = deviceData;
    }
}
