/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2RegistrationData extends Fido2Data {

    private static final long serialVersionUID = 4599467930864459334L;

    private String username;
    private String domain;
    private String userId;
    private String challenge;

    private String attenstationRequest;
    private String attenstationResponse;

    private String uncompressedECPoint;
    private String publicKeyId;

    private String type;

    private Fido2RegistrationStatus status;

    private int counter;

    private String attestationType;

    private int signatureAlgorithm;

    private String applicationId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getAttenstationRequest() {
        return attenstationRequest;
    }

    public void setAttenstationRequest(String attenstationRequest) {
        this.attenstationRequest = attenstationRequest;
    }

    public String getAttenstationResponse() {
        return attenstationResponse;
    }

    public void setAttenstationResponse(String attenstationResponse) {
        this.attenstationResponse = attenstationResponse;
    }

    public String getUncompressedECPoint() {
        return uncompressedECPoint;
    }

    public void setUncompressedECPoint(String uncompressedECPoint) {
        this.uncompressedECPoint = uncompressedECPoint;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Fido2RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(Fido2RegistrationStatus status) {
        this.status = status;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getAttestationType() {
        return attestationType;
    }

    public void setAttestationType(String attestationType) {
        this.attestationType = attestationType;
    }

    public int getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(int signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	@Override
	public String toString() {
		return "Fido2RegistrationData [username=" + username + ", domain=" + domain + ", userId=" + userId + ", challenge=" + challenge
				+ ", attenstationRequest=" + attenstationRequest + ", attenstationResponse=" + attenstationResponse
				+ ", uncompressedECPoint=" + uncompressedECPoint + ", publicKeyId=" + publicKeyId + ", type=" + type + ", status=" + status
				+ ", counter=" + counter + ", attestationType=" + attestationType + ", signatureAlgorithm=" + signatureAlgorithm
				+ ", applicationId=" + applicationId + "]";
	}
}
