/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */

package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/06/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationCodeFlowParams implements org.gluu.oxd.common.params.HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "redirect_url")
    private String redirect_url;
    @JsonProperty(value = "client_id")
    private String client_id;
    @JsonProperty(value = "client_secret")
    private String client_secret;
    @JsonProperty(value = "user_id")
    private String user_id;
    @JsonProperty(value = "user_secret")
    private String user_secret;
    @JsonProperty(value = "scope")
    private String scope;
    @JsonProperty(value = "nonce")
    private String nonce;
    @JsonProperty(value = "acr")
    private String acr;
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;

    public AuthorizationCodeFlowParams() {
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(String clientSecret) {
        this.client_secret = clientSecret;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUrl() {
        return redirect_url;
    }

    public void setRedirectUrl(String p_redirectUrl) {
        redirect_url = p_redirectUrl;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String p_clientId) {
        client_id = p_clientId;
    }

    public String getUserId() {
        return user_id;
    }

    public void setUserId(String p_userId) {
        user_id = p_userId;
    }

    public String getUserSecret() {
        return user_secret;
    }

    public void setUserSecret(String p_userSecret) {
        user_secret = p_userSecret;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AuthorizationCodeFlowParams");
        sb.append("{oxd_id='").append(oxd_id).append('\'');
        sb.append(", redirect_url='").append(redirect_url).append('\'');
        sb.append(", client_id='").append(client_id).append('\'');
        sb.append(", user_id='").append(user_id).append('\'');
        sb.append(", user_secret='").append(user_secret).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", nonce='").append(nonce).append('\'');
        sb.append(", acr='").append(acr).append('\'');
        sb.append(", protection_access_token='").append(protection_access_token).append('\'');
        sb.append('}');
        return sb.toString();
    }
}