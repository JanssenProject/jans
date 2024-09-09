package io.jans.fido2.model.attestation;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.fido2.ctap.AuthenticatorAttachment;
import io.jans.orm.model.fido2.UserVerification;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticatorSelection {
    private AuthenticatorAttachment authenticatorAttachment;
    private UserVerification userVerification;
    private Boolean requireResidentKey;
    private UserVerification residentKey;

    public AuthenticatorAttachment getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(AuthenticatorAttachment authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public UserVerification getUserVerification() {
        return userVerification;
    }

    public void setUserVerification(UserVerification userVerification) {
        this.userVerification = userVerification;
    }

    public Boolean getRequireResidentKey() {
        return requireResidentKey;
    }

    public void setRequireResidentKey(Boolean requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
    }

    public UserVerification getResidentKey() {
        return residentKey;
    }

    public void setResidentKey(UserVerification residentKey) {
        this.residentKey = residentKey;
    }

    @Override
    public String toString() {
        return "AuthenticatorSelection{" +
                "authenticatorAttachment=" + authenticatorAttachment +
                ", userVerification=" + userVerification +
                ", requireResidentKey=" + requireResidentKey +
                ", residentKey=" + residentKey +
                '}';
    }
}
