package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttestationOptions {
    private String username ;
    private String displayName;
    private AttestationConveyancePreference attestation;
    private String origin;
    private JsonNode extensions;
    private AuthenticatorSelection authenticatorSelection;
    private AuthenticatorAttachment authenticatorAttachment;
    private Long timeout;
    @JsonProperty(value = "session_id")
    private String sessionId;

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

    public AttestationConveyancePreference getAttestation() {
        return attestation;
    }

    public void setAttestation(AttestationConveyancePreference attestation) {
        this.attestation = attestation;
    }

    public JsonNode getExtensions() {
        return extensions;
    }

    public void setExtensions(JsonNode extensions) {
        this.extensions = extensions;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public AuthenticatorSelection getAuthenticatorSelection() {
        return authenticatorSelection;
    }

    public void setAuthenticatorSelection(AuthenticatorSelection authenticatorSelection) {
        this.authenticatorSelection = authenticatorSelection;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public AuthenticatorAttachment getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(AuthenticatorAttachment authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "AttestationOptions{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", attestation=" + attestation +
                ", origin='" + origin + '\'' +
                ", extensions=" + extensions +
                ", authenticatorSelection=" + authenticatorSelection +
                ", timeout=" + timeout +
                '}';
    }

	public AttestationOptions() {
		
	}
    
}
