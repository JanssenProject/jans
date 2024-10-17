package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
	private String attestationObject;
	private String clientDataJSON;

	public String getAttestationObject() {
		return attestationObject;
	}

	public void setAttestationObject(String attestationObject) {
		this.attestationObject = attestationObject;
	}

	public String getClientDataJSON() {
		return clientDataJSON;
	}

	public void setClientDataJSON(String clientDataJSON) {
		this.clientDataJSON = clientDataJSON;
	}

}
