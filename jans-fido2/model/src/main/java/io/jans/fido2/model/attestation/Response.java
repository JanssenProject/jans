package io.jans.fido2.model.attestation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
	private String attestationObject;
	private String clientDataJSON;
	private String authenticatorData;
	private String publicKey;
	private int publicKeyAlgorithm;

	@JsonProperty
	private String[] transports;

	public String getAuthenticatorData() {
		return authenticatorData;
	}

	public void setAuthenticatorData(String authenticatorData) {
		this.authenticatorData = authenticatorData;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public int getPublicKeyAlgorithm() {
		return publicKeyAlgorithm;
	}

	public void setPublicKeyAlgorithm(int publicKeyAlgorithm) {
		this.publicKeyAlgorithm = publicKeyAlgorithm;
	}

	public String[] getTransports() {
		return transports;
	}

	public void setTransports(String[] transports) {
		this.transports = transports;
	}

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

	@Override
	public String toString() {
		return "Response [attestationObject=" + attestationObject + ", clientDataJSON=" + clientDataJSON
				+ ", authenticatorData=" + authenticatorData + ", publicKey=" + publicKey + ", publicKeyAlgorithm="
				+ publicKeyAlgorithm + ", transports=" + Arrays.toString(transports) + "]";
	}

}
