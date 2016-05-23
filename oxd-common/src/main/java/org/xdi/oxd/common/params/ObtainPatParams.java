/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class ObtainPatParams implements IParams, HasOxdId {

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

    public ObtainPatParams() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String p_clientSecret) {
        clientSecret = p_clientSecret;
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

    public String getUmaDiscoveryUrl() {
        return umaDiscoveryUrl;
    }

    public void setUmaDiscoveryUrl(String p_umaDiscoveryUrl) {
        umaDiscoveryUrl = p_umaDiscoveryUrl;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObtainPatParams");
        sb.append("{oxdId='").append(oxdId).append('\'');
        sb.append(", redirectUrl='").append(redirectUrl).append('\'');
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", clientSecret='").append(clientSecret).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", userSecret='").append(userSecret).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
