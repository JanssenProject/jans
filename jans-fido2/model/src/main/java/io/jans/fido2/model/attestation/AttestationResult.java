package io.jans.fido2.model.attestation;

import io.jans.fido2.model.common.PublicKeyCredentialType;
import io.jans.fido2.model.common.SuperGluuSupport;

public class AttestationResult extends SuperGluuSupport {
    private String id;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
    private Response response;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "AttestationResult{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", response=" + response +
                '}';
    }
}
