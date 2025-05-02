
package io.jans.casa.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

public class Fido2RegistrationData {

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
	private String status;
	private int counter;
	private String attestationType;
	private int signatureAlgorithm;
	private String rpId;
	// Credential backup eligibility and current backup state is conveyed by the
	// backupStateFlag and backupEligibilityFlag flags in the authenticator data.
	// See https://w3c.github.io/webauthn/#sctn-authenticator-data
	private boolean backupStateFlag;
	private boolean backupEligibilityFlag;
	private boolean attestedCredentialDataFlag;
	private boolean extensionDataFlag;
	private boolean userVerifiedFlag;
	private boolean userPresentFlag;

	private String authentictatorAttachment;

	private String credId;
	private String transports[];

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
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

	public boolean isAttestedCredentialDataFlag() {
		return attestedCredentialDataFlag;
	}

	public void setAttestedCredentialDataFlag(boolean attestedCredentialDataFlag) {
		this.attestedCredentialDataFlag = attestedCredentialDataFlag;
	}

	public boolean isExtensionDataFlag() {
		return extensionDataFlag;
	}

	public void setExtensionDataFlag(boolean extensionDataFlag) {
		this.extensionDataFlag = extensionDataFlag;
	}

	public boolean isUserVerifiedFlag() {
		return userVerifiedFlag;
	}

	public void setUserVerifiedFlag(boolean userVerifiedFlag) {
		this.userVerifiedFlag = userVerifiedFlag;
	}

	public boolean isUserPresentFlag() {
		return userPresentFlag;
	}

	public void setUserPresentFlag(boolean userPresentFlag) {
		this.userPresentFlag = userPresentFlag;
	}

	public String getAuthentictatorAttachment() {
		return authentictatorAttachment;
	}

	public void setAuthentictatorAttachment(String authentictatorAttachment) {
		this.authentictatorAttachment = authentictatorAttachment;
	}

	public String getCredId() {
		return credId;
	}

	public void setCredId(String credId) {
		this.credId = credId;
	}

	public String[] getTransports() {
		return transports;
	}

	public void setTransports(String[] transports) {
		this.transports = transports;
	}

}
