package io.jans.fido2.model.assertion;

import com.google.common.base.Strings;
import io.jans.fido2.model.common.PublicKeyCredentialType;
import io.jans.fido2.model.common.SuperGluuSupport;

public class AssertionResult extends SuperGluuSupport {
    private String id;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
    private String rawId;
    private Response response;

    public AssertionResult() {
    }

    public AssertionResult(String id, String rawId, Response response) {
        this.id = id;
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
        this.rawId = rawId;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public static AssertionResult createAssertionResult(String id, String rawId, Response response) {
        return new AssertionResult(id, rawId, response);
    }

    @Override
    public String toString() {
        return "AssertionResult{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", rawId='" + rawId + '\'' +
                ", response=" + response +
                '}';
    }
}
