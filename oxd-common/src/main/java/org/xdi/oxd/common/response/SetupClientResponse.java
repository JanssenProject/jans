package org.xdi.oxd.common.response;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/03/2017
 */

public class SetupClientResponse implements IOpResponse {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "client_id_of_oxd_id")
    private String clientIdOfOxdId;
    @JsonProperty(value = "op_host")
    private String opHost;


    @JsonProperty(value = "setup_client_oxd_id")
    private String setupClientOxdId;
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "client_registration_access_token")
    private String clientRegistrationAccessToken;
    @JsonProperty(value = "client_registration_client_uri")
    private String clientRegistrationClientUri;
    @JsonProperty(value = "client_id_issued_at")
    private long clientIdIssuedAt;
    @JsonProperty(value = "client_secret_expires_at")
    private long clientSecretExpiresAt;

    public String getSetupClientOxdId() {
        return setupClientOxdId;
    }

    public void setSetupClientOxdId(String setupClientOxdId) {
        this.setupClientOxdId = setupClientOxdId;
    }

    public String getClientIdOfOxdId() {
        return clientIdOfOxdId;
    }

    public void setClientIdOfOxdId(String clientIdOfOxdId) {
        this.clientIdOfOxdId = clientIdOfOxdId;
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
        return "SetupClientResponse{" +
                "oxdId='" + oxdId + '\'' +
                ", opHost='" + opHost + '\'' +
                ", setupClientOxdId='" + setupClientOxdId + '\'' +
                ", clientIdOfOxdId='" + clientIdOfOxdId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientRegistrationAccessToken='" + clientRegistrationAccessToken + '\'' +
                ", clientRegistrationClientUri='" + clientRegistrationClientUri + '\'' +
                ", clientIdIssuedAt='" + clientIdIssuedAt + '\'' +
                ", clientSecretExpiresAt='" + clientSecretExpiresAt + '\'' +
                '}';
    }
}
