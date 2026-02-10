package io.jans.idp.authn.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

public class JansAuthenticationContext extends BaseContext {

    private String authorizationCode;
    private String accessToken;
    private String idToken;
    private String state;
    private String nonce;
    private String acrValues;
    private String relayingPartyId;
    private String externalProviderUri;
    private String userPrincipal;
    private boolean authenticated;
    private String errorMessage;

    public JansAuthenticationContext() {
        this.authenticated = false;
    }

    @Nullable
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(@Nullable String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(@Nullable String accessToken) {
        this.accessToken = accessToken;
    }

    @Nullable
    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(@Nullable String idToken) {
        this.idToken = idToken;
    }

    @Nullable
    public String getState() {
        return state;
    }

    public void setState(@Nullable String state) {
        this.state = state;
    }

    @Nullable
    public String getNonce() {
        return nonce;
    }

    public void setNonce(@Nullable String nonce) {
        this.nonce = nonce;
    }

    @Nullable
    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(@Nullable String acrValues) {
        this.acrValues = acrValues;
    }

    @Nullable
    public String getRelayingPartyId() {
        return relayingPartyId;
    }

    public void setRelayingPartyId(@Nullable String relayingPartyId) {
        this.relayingPartyId = relayingPartyId;
    }

    @Nullable
    public String getExternalProviderUri() {
        return externalProviderUri;
    }

    public void setExternalProviderUri(@Nullable String externalProviderUri) {
        this.externalProviderUri = externalProviderUri;
    }

    @Nullable
    public String getUserPrincipal() {
        return userPrincipal;
    }

    public void setUserPrincipal(@Nullable String userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "JansAuthenticationContext{" +
                "userPrincipal='" + userPrincipal + '\'' +
                ", authenticated=" + authenticated +
                ", relayingPartyId='" + relayingPartyId + '\'' +
                ", acrValues='" + acrValues + '\'' +
                '}';
    }
}
