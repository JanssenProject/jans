/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RegisterClientOpResponse implements IOpResponse {

    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "registration_access_token")
    private String registrationAccessToken;
    @JsonProperty(value = "client_secret_expires_at")
    private long clientSecretExpiresAt;
    @JsonProperty(value = "registration_client_uri")
    private String registrationClientUri;
    @JsonProperty(value = "client_id_issued_at")
    private long clientIdIssuedAt;

    public RegisterClientOpResponse() {
    }

    public long getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(long clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public String getRegistrationClientUri() {
        return registrationClientUri;
    }

    public void setRegistrationClientUri(String registrationClientUri) {
        this.registrationClientUri = registrationClientUri;
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

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String p_registrationAccessToken) {
        registrationAccessToken = p_registrationAccessToken;
    }

    public long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(long p_clientSecretExpiresAt) {
        clientSecretExpiresAt = p_clientSecretExpiresAt;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterClientOpResponse");
        sb.append("{clientId='").append(clientId).append('\'');
        sb.append(", clientSecret='").append(clientSecret).append('\'');
        sb.append(", registrationAccessToken='").append(registrationAccessToken).append('\'');
        sb.append(", clientSecretExpiresAt=").append(clientSecretExpiresAt);
        sb.append('}');
        return sb.toString();
    }
}
