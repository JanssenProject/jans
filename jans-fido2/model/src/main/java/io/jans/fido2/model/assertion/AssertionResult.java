package io.jans.fido2.model.assertion;

import java.util.HashMap;

import com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.fido2.model.common.PublicKeyCredentialType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionResult  {
    private String id;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
    private String rawId;
    private Response response;
    private HashMap<String, String> clientExtensionResults;
    
    public AssertionResult() {
    }

    public AssertionResult(String id, String rawId, Response response) {
        this.id = id;
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
        this.rawId = rawId;
        this.response = response;
        this.clientExtensionResults = clientExtensionResults;
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

    public HashMap<String, String> getClientExtensionResults() {
		return clientExtensionResults;
	}

	public void setClientExtensionResults(HashMap<String, String> clientExtensionResults) {
		this.clientExtensionResults = clientExtensionResults;
	}

	@Override
	public String toString() {
		return "AssertionResult [id=" + id + ", type=" + type + ", rawId=" + rawId + ", response=" + response
				+ ", clientExtensionResults=" + clientExtensionResults + "]";
	}

	

	
}
