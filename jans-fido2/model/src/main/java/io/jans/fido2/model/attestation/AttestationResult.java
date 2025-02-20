package io.jans.fido2.model.attestation;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationResult {
	private String id;
	private String type;
	private String rawId;
	private Response response;
	private HashMap<String, Object> clientExtensionResults;
	private String authentictatorAttachment;

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

	public HashMap<String, Object> getClientExtensionResults() {
		return clientExtensionResults;
	}

	public void setClientExtensionResults(HashMap<String, Object> clientExtensionResults) {
		this.clientExtensionResults = clientExtensionResults;
	}

	public String getAuthentictatorAttachment() {
		return authentictatorAttachment;
	}

	public void setAuthentictatorAttachment(String authentictatorAttachment) {
		this.authentictatorAttachment = authentictatorAttachment;
	}

	public String getRawId() {
		return rawId;
	}

	public void setRawId(String rawId) {
		this.rawId = rawId;
	}

	@Override
	public String toString() {
		return "AttestationResult [id=" + id + ", type=" + type + ", rawId=" + rawId + ", response=" + response
				+ ", clientExtensionResults=" + clientExtensionResults + ", authentictatorAttachment="
				+ authentictatorAttachment + "]";
	}

}

class ClientExtensionResults {
	@JsonProperty("credProps")
	private CredProps credProps;

	public CredProps getCredProps() {
		return credProps;
	}

	public void setCredProps(CredProps credProps) {
		this.credProps = credProps;
	}

}

class CredProps {
	@JsonProperty("rk")
	private boolean rk;

	public boolean isRk() {
		return rk;
	}

	public void setRk(boolean rk) {
		this.rk = rk;
	}

}
