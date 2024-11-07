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
    private String origin;
    private String userId;
    private String challenge;

    private String attestationRequest;
    private String attestationResponse;

    private String uncompressedECPoint;
    private String publicKeyId;

    private String type;

    private Fido2RegistrationStatus status;

    private int counter;

    private String attestationType;

    private int signatureAlgorithm;

    private String rpId;
    //Credential backup eligibility and current backup state is conveyed by the backupStateFlag and backupEligibilityFlag flags in the authenticator data. See https://w3c.github.io/webauthn/#sctn-authenticator-data
    private boolean backupStateFlag;

    private boolean backupEligibilityFlag;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getAttestationRequest() {
        return attestationRequest;
    }

    public void setAttestationRequest(String attestationRequest) {
        this.attestationRequest = attestationRequest;
    }

    public String getAttestationResponse() {
        return attestationResponse;
    }

    public void setAttestationResponse(String attestationResponse) {
        this.attestationResponse = attestationResponse;
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

   

    public boolean getBackupStateFlag() {
        return this.backupStateFlag;
    }

    public void setBackupStateFlag(boolean backupStateFlag) {
        this.backupStateFlag = backupStateFlag;
    }

    public boolean getBackupEligibilityFlag() {
        return this.backupEligibilityFlag;
    }

    public void setBackupEligibilityFlag(boolean backupEligibilityFlag) {
        this.backupEligibilityFlag = backupEligibilityFlag;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getRpId() {
        return rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

	@Override
	public String toString() {
		return "Fido2RegistrationData [username=" + username + ", origin=" + origin + ", userId=" + userId + ", challenge=" + challenge
				+ ", attestationRequest=" + attestationRequest + ", attestationResponse=" + attestationResponse
				+ ", uncompressedECPoint=" + uncompressedECPoint + ", publicKeyId=" + publicKeyId + ", type=" + type + ", status=" + status
				+ ", counter=" + counter + ", attestationType=" + attestationType + ", signatureAlgorithm=" + signatureAlgorithm
				+ ", rpId=" + rpId + "]";
	}
}
