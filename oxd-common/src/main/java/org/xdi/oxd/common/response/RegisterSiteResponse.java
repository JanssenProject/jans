package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 29/09/2015
 */

public class RegisterSiteResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "op_host")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "op_host")
    private String opHost;
    @JsonProperty(value = "client_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_name")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_name")
    private String clientName;
    @JsonProperty(value = "client_secret")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "client_registration_access_token")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_registration_access_token")
    private String clientRegistrationAccessToken;
    @JsonProperty(value = "client_registration_client_uri")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_registration_client_uri")
    private String clientRegistrationClientUri;
    @JsonProperty(value = "client_id_issued_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_id_issued_at")
    private long clientIdIssuedAt;
    @JsonProperty(value = "client_secret_expires_at")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "client_secret_expires_at")
    private long clientSecretExpiresAt;

    public RegisterSiteResponse() {
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientRegistrationAccessToken() {
        return clientRegistrationAccessToken;
    }

    public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
        this.clientRegistrationAccessToken = clientRegistrationAccessToken;
    }

    public String getClientRegistrationClientUri() {
        return clientRegistrationClientUri;
    }

    public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
        this.clientRegistrationClientUri = clientRegistrationClientUri;
    }

    public long getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(long clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public long getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(long clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    @Override
    public String toString() {
        return "RegisterSiteResponse{" +
                "oxdId='" + oxdId + '\'' +
                ", opHost='" + opHost + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", clientRegistrationAccessToken='" + clientRegistrationAccessToken + '\'' +
                ", clientRegistrationClientUri='" + clientRegistrationClientUri + '\'' +
                ", clientIdIssuedAt=" + clientIdIssuedAt +
                ", clientSecretExpiresAt=" + clientSecretExpiresAt +
                '}';
    }
}
