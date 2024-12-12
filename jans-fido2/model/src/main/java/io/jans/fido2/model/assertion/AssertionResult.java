package io.jans.fido2.model.assertion;

import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.fido2.model.common.PublicKeyCredentialType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssertionResult  {
    private String id;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
    private String rawId;
    private Response response;
    private HashMap<String, String> clientExtensionResults;
    private String[] transports;
    
    public AssertionResult() {
    }

    public AssertionResult(String id, String rawId, Response response, String []transports) {
        this.id = id;
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
        this.rawId = rawId;
        this.response = response;
        this.clientExtensionResults = clientExtensionResults;
        this.transports = transports;
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

    public HashMap<String, String> getClientExtensionResults() {
		return clientExtensionResults;
	}

	public void setClientExtensionResults(HashMap<String, String> clientExtensionResults) {
		this.clientExtensionResults = clientExtensionResults;
	}

	public String[] getTransports() {
		return transports;
	}

	public void setTransports(String[] transports) {
		this.transports = transports;
	}

	@Override
	public String toString() {
		return "AssertionResult [id=" + id + ", type=" + type + ", rawId=" + rawId + ", response=" + response
				+ ", clientExtensionResults=" + clientExtensionResults + ", transports=" + Arrays.toString(transports)
				+ "]";
	}

}
