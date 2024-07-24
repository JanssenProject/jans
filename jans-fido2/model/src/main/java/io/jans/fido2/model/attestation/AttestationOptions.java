package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.ctap.AttestationConveyancePreference;
import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.fido2.model.common.SuperGluuSupport;

public class AttestationOptions extends SuperGluuSupport {
    private String username;
    private String displayName;
    private AttestationConveyancePreference attestation;
    private String documentDomain;
    private JsonNode extensions;
    private AuthenticatorSelection authenticatorSelection;
    private AuthenticatorAttachment authenticatorAttachment;
    private Long timeout;
    private String session_id;

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

    public String getDocumentDomain() {
        return documentDomain;
    }

    public void setDocumentDomain(String documentDomain) {
        this.documentDomain = documentDomain;
    }

    public AuthenticatorAttachment getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(AuthenticatorAttachment authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public String getSession_id() {
        return session_id;
    }

    public void setSession_id(String session_id) {
        this.session_id = session_id;
    }

    @Override
    public String toString() {
        return "AttestationOptions{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", attestation=" + attestation +
                ", documentDomain='" + documentDomain + '\'' +
                ", extensions=" + extensions +
                ", authenticatorSelection=" + authenticatorSelection +
                ", timeout=" + timeout +
                '}';
    }
}
