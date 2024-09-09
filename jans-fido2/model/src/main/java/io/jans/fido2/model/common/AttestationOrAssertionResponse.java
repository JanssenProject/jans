package io.jans.fido2.model.common;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class AttestationOrAssertionResponse {
    private PublicKeyCredentialDescriptor credentials;
    private String status;
    private String errorMessage;
    private String username;
    private String credentialID;
	private String userVerification;
	private String userPresence;
	private boolean counterSupported;
	private boolean multiFactor;
	private boolean multiDevice;
	private String deviceType;
	private boolean certified;
	private String certificationLevel;
	private String aaguidOrSkid;
	private String authenticatorName;
	private String origin;
	private String hint;
	public String getCredentialID() {
		return credentialID;
	}

	public void setCredentialID(String credentialID) {
		this.credentialID = credentialID;
	}

	public String getUserVerification() {
		return userVerification;
	}

	public void setUserVerification(String userVerification) {
		this.userVerification = userVerification;
	}

	public String getUserPresence() {
		return userPresence;
	}

	public void setUserPresence(String userPresence) {
		this.userPresence = userPresence;
	}

	public boolean isCounterSupported() {
		return counterSupported;
	}

	public void setCounterSupported(boolean counterSupported) {
		this.counterSupported = counterSupported;
	}

	public boolean isMultiFactor() {
		return multiFactor;
	}

	public void setMultiFactor(boolean multiFactor) {
		this.multiFactor = multiFactor;
	}

	public boolean isMultiDevice() {
		return multiDevice;
	}

	public void setMultiDevice(boolean multiDevice) {
		this.multiDevice = multiDevice;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public boolean isCertified() {
		return certified;
	}

	public void setCertified(boolean certified) {
		this.certified = certified;
	}

	public String getCertificationLevel() {
		return certificationLevel;
	}

	public void setCertificationLevel(String certificationLevel) {
		this.certificationLevel = certificationLevel;
	}

	public String getAaguidOrSkid() {
		return aaguidOrSkid;
	}

	public void setAaguidOrSkid(String aaguidOrSkid) {
		this.aaguidOrSkid = aaguidOrSkid;
	}

	public String getAuthenticatorName() {
		return authenticatorName;
	}

	public void setAuthenticatorName(String authenticatorName) {
		this.authenticatorName = authenticatorName;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	
	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getRpId() {
		return rpId;
	}

	public void setRpId(String rpId) {
		this.rpId = rpId;
	}

	public List<PublicKeyCredentialDescriptor> getAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(List<PublicKeyCredentialDescriptor> allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	public JsonNode getExtensions() {
		return extensions;
	}

	public void setExtensions(JsonNode extensions) {
		this.extensions = extensions;
	}

	private String challenge;
	private String rpId;
	
	private List<PublicKeyCredentialDescriptor> allowCredentials;
	private Long timeout;
	private JsonNode extensions;
    
    

    public PublicKeyCredentialDescriptor getCredentials() {
		return credentials;
	}

	public void setCredentials(PublicKeyCredentialDescriptor credentials) {
		this.credentials = credentials;
	}

	public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

	@Override
	public String toString() {
		return "AttestationOrAssertionResponse [credentials=" + credentials + ", status=" + status + ", errorMessage="
				+ errorMessage + ", username=" + username + ", credentialID=" + credentialID + ", userVerification="
				+ userVerification + ", userPresence=" + userPresence + ", counterSupported=" + counterSupported
				+ ", multiFactor=" + multiFactor + ", multiDevice=" + multiDevice + ", deviceType=" + deviceType
				+ ", certified=" + certified + ", certificationLevel=" + certificationLevel + ", aaguidOrSkid="
				+ aaguidOrSkid + ", authenticatorName=" + authenticatorName + ", origin=" + origin
				+ ", hint=" + hint + ", challenge="
				+ challenge + ", rpId=" + rpId + ", allowCredentials=" + allowCredentials + ", timeout=" + timeout
				+ ", extensions=" + extensions + "]";
	}

   
}
