package io.jans.fido2.model.assertion;

public class Response {
    private String authenticatorData;
    private String signature;
    private String clientDataJSON;
    private String userHandle;
    private String deviceData;
    private String attestationObject;

    public Response() {
    }

    public Response(String authenticatorData, String signature, String clientDataJSON, String userHandle, String deviceData, String attestationObject) {
        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.clientDataJSON = clientDataJSON;
        this.userHandle = userHandle;
        this.deviceData = deviceData;
        this.attestationObject = attestationObject;
    }

    public String getAuthenticatorData() {
        return authenticatorData;
    }

    public void setAuthenticatorData(String authenticatorData) {
        this.authenticatorData = authenticatorData;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getClientDataJSON() {
        return clientDataJSON;
    }

    public void setClientDataJSON(String clientDataJSON) {
        this.clientDataJSON = clientDataJSON;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(String deviceData) {
        this.deviceData = deviceData;
    }

    public String getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(String attestationObject) {
        this.attestationObject = attestationObject;
    }

    public static Response createResponse(String authenticatorData, String signature, String clientDataJSON, String userHandle, String deviceData, String attestationObject) {
        Response instance = new Response(authenticatorData, signature, clientDataJSON, userHandle, deviceData, attestationObject);
        return instance;
    }

    @Override
    public String toString() {
        return "Response{" +
                "authenticatorData='" + authenticatorData + '\'' +
                ", signature='" + signature + '\'' +
                ", clientDataJSON='" + clientDataJSON + '\'' +
                ", userHandle='" + userHandle + '\'' +
                ", deviceData='" + deviceData + '\'' +
                '}';
    }
}
