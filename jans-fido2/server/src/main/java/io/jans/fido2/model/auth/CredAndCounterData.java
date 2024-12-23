/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */



package io.jans.fido2.model.auth;

public class CredAndCounterData {
	private String credId;
	private int counters;
	private String attestationType;
	private String uncompressedEcPoint;
	private int signatureAlgorithm;
	// The BS flag SHALL be set if and only if the credential is a multi-device
	// credential and is currently backed up. If the backup status of a credential
	// is uncertain or the authenticator suspects a problem with the backed up
	// credential, the BS flag SHOULD NOT be set.
	private boolean backupStateFlag;
	// The BE flag SHALL be set if and only if the credential is a multi-device
	// credential.
	private boolean backupEligibilityFlag;
	// For attestation signatures, the authenticator MUST set the AT flag and
	// include the attestedCredentialData.
	private boolean attestedCredentialDataFlag;
	// If the authenticator does not include any extension data, it MUST set the ED
	// flag to zero, and to one if extension data is included.
	private boolean extensionDataFlag;
	private boolean userVerifiedFlag;
	private boolean userPresentFlag;
	private String authenticatorName;

	public String getCredId() {
		return credId;
	}

	public int getCounters() {
		return counters;
	}

	public CredAndCounterData setCredId(String credId) {
		this.credId = credId;
		return this;
	}

	public CredAndCounterData setCounters(int counters) {
		this.counters = counters;
		return this;
	}

	public CredAndCounterData setAttestationType(String attestationType) {
		this.attestationType = attestationType;
		return this;
	}

	public String getAttestationType() {
		return attestationType;
	}

	public CredAndCounterData setUncompressedEcPoint(String uncompressedEcPoint) {
		this.uncompressedEcPoint = uncompressedEcPoint;
		return this;
	}

	public String getUncompressedEcPoint() {
		return uncompressedEcPoint;
	}

	public int getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	public void setSignatureAlgorithm(int signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
	}

	public boolean getBackupStateFlag() {
		return backupStateFlag;
	}

	public void setBackupStateFlag(boolean backupStateFlag) {
		this.backupStateFlag = backupStateFlag;
	}

	public boolean getBackupEligibilityFlag() {
		return backupEligibilityFlag;
	}

	public void setBackupEligibilityFlag(boolean backupEligibilityFlag) {
		this.backupEligibilityFlag = backupEligibilityFlag;
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

	public void setAuthenticatorName(String authenticatorName) {this.authenticatorName = authenticatorName;}
	public String getAuthenticatorName() {return authenticatorName;}
}
