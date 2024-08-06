package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.databind.JsonNode;

public class AsserOptGenerateResponse {
    private String challenge;
    private String rpId;
    private String userVerification;
    private Long timeout;
    private JsonNode extensions;
    private String status;

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    public String getUserVerification() {
        return userVerification;
    }

    public void setUserVerification(String userVerification) {
        this.userVerification = userVerification;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public JsonNode getExtensions() {
        return extensions;
    }

    public void setExtensions(JsonNode extensions) {
        this.extensions = extensions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AsserOptGenerateResponse{" +
                "challenge='" + challenge + '\'' +
                ", rpId='" + rpId + '\'' +
                ", userVerification='" + userVerification + '\'' +
                ", timeout=" + timeout +
                ", extensions=" + extensions +
                ", status='" + status + '\'' +
                '}';
    }
}
