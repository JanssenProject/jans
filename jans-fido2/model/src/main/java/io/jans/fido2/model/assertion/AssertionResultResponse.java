package io.jans.fido2.model.assertion;

import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;

public class AssertionResultResponse {
    private PublicKeyCredentialDescriptor authenticatedCredentials;
    private String status;
    private String errorMessage;
    private String username;

    public PublicKeyCredentialDescriptor getAuthenticatedCredentials() {
        return authenticatedCredentials;
    }

    public void setAuthenticatedCredentials(PublicKeyCredentialDescriptor authenticatedCredentials) {
        this.authenticatedCredentials = authenticatedCredentials;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "AssertionResultResponse{" +
                "authenticatedCredentials=" + authenticatedCredentials +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
