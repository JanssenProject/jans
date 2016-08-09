/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/06/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImplicitFlowParams implements HasOxdIdParams {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "redirect_url")
    private String redirectUrl;
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "user_id")
    private String userId;
    @JsonProperty(value = "user_secret")
    private String userSecret;
    @JsonProperty(value = "scope")
    private String scope;
    @JsonProperty(value = "nonce")
    private String nonce;

    public ImplicitFlowParams() {
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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
        return redirectUrl;
    }

    public void setRedirectUrl(String p_redirectUrl) {
        redirectUrl = p_redirectUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String p_clientId) {
        clientId = p_clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String p_userId) {
        userId = p_userId;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String p_userSecret) {
        userSecret = p_userSecret;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ImplicitFlowParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", redirectUrl='").append(redirectUrl).append('\'');
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", userSecret='").append(userSecret).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", nonce='").append(nonce).append('\'');
        sb.append('}');
        return sb.toString();
    }
}