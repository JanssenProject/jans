/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */

package io.jans.ca.common.params;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/06/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationCodeFlowParams implements HasRpIdParams {

    @JsonProperty(value = "rp_id")
    private String rp_id;
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

    public AuthorizationCodeFlowParams() {
    }

    public String getRpId() {
        return rp_id;
    }

    public void setRpId(String rpId) {
        this.rp_id = rpId;
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
        sb.append("{rp_id='").append(rp_id).append('\'');
        sb.append(", redirect_url='").append(redirect_url).append('\'');
        sb.append(", client_id='").append(client_id).append('\'');
        sb.append(", user_id='").append(user_id).append('\'');
        sb.append(", user_secret='").append(user_secret).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", nonce='").append(nonce).append('\'');
        sb.append(", acr='").append(acr).append('\'');
        sb.append('}');
        return sb.toString();
    }
}