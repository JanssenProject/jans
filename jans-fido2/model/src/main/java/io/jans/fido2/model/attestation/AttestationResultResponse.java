package io.jans.fido2.model.attestation;

import io.jans.fido2.model.common.PublicKeyCredentialDescriptor;

public class AttestationResultResponse {
    private PublicKeyCredentialDescriptor createdCredentials;
    private String errorMessage;
    private String status;

    public PublicKeyCredentialDescriptor getCreatedCredentials() {
        return createdCredentials;
    }

    public void setCreatedCredentials(PublicKeyCredentialDescriptor createdCredentials) {
        this.createdCredentials = createdCredentials;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
