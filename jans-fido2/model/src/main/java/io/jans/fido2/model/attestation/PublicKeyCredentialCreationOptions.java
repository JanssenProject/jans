package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.entry.PublicKeyCredentialHints;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.model.common.*;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicKeyCredentialCreationOptions {
    private AttestationConveyancePreference attestation;
    private AuthenticatorSelection authenticatorSelection;
    private String challenge;
    private Set<PublicKeyCredentialParameters> pubKeyCredParams;
    private RelyingParty rp;
    private User user;
    private String username;
    private String displayName;
    private Set<PublicKeyCredentialDescriptor> excludeCredentials;
    private Long timeout;
    private JsonNode extensions;
    private String status;
    private String errorMessage;
    private Set<String> hints;

    public AttestationConveyancePreference getAttestation() {
        return attestation;
    }

    public void setAttestation(AttestationConveyancePreference attestation) {
        this.attestation = attestation;
    }

    public AuthenticatorSelection getAuthenticatorSelection() {
        return authenticatorSelection;
    }

    public void setAuthenticatorSelection(AuthenticatorSelection authenticatorSelection) {
        this.authenticatorSelection = authenticatorSelection;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public Set<PublicKeyCredentialParameters> getPubKeyCredParams() {
        return pubKeyCredParams;
    }

    public void setPubKeyCredParams(Set<PublicKeyCredentialParameters> pubKeyCredParams) {
        this.pubKeyCredParams = pubKeyCredParams;
    }

    public RelyingParty getRp() {
        return rp;
    }

    public void setRp(RelyingParty rp) {
        this.rp = rp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<PublicKeyCredentialDescriptor> getExcludeCredentials() {
        return excludeCredentials;
    }

    public void setExcludeCredentials(Set<PublicKeyCredentialDescriptor> excludeCredentials) {
        this.excludeCredentials = excludeCredentials;
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

	public Set<String> getHints() {
		return hints;
	}

	public void setHints(Set<String> hints) {
		this.hints = hints;
	}

	@Override
	public String toString() {
		return "PublicKeyCredentialCreationOptions [attestation=" + attestation + ", authenticatorSelection="
				+ authenticatorSelection + ", challenge=" + challenge + ", pubKeyCredParams=" + pubKeyCredParams
				+ ", rp=" + rp + ", user=" + user + ", username=" + username + ", displayName=" + displayName
				+ ", excludeCredentials=" + excludeCredentials + ", timeout=" + timeout + ", extensions=" + extensions
				+ ", status=" + status + ", errorMessage=" + errorMessage + ", hints="
				+ hints + "]";
	}
}
