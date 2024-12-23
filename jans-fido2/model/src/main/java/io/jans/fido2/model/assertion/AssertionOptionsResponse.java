package io.jans.fido2.model.assertion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;

import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssertionOptionsResponse {
    private String challenge;
    private String user;
    private String userVerification;
    private String rpId;
    private String status;
    private String errorMessage;
    private List<PublicKeyCredentialDescriptor> allowCredentials;
    private Long timeout;
    private JsonNode extensions;

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUserVerification() {
        return userVerification;
    }

    public void setUserVerification(String userVerification) {
        this.userVerification = userVerification;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<PublicKeyCredentialDescriptor> getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(List<PublicKeyCredentialDescriptor> allowCredentials) {
        this.allowCredentials = allowCredentials;
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

    @Override
    public String toString() {
        return "AssertionOptionsResponse{" +
                "challenge='" + challenge + '\'' +
                ", user='" + user + '\'' +
                ", userVerification='" + userVerification + '\'' +
                ", rpId='" + rpId + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", allowCredentials=" + allowCredentials +
                ", timeout=" + timeout +
                ", extensions='" + extensions + '\'' +
                '}';
    }
}
