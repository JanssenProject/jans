package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

public class DCResponse {
    public DCResponse(String clientId,
                      String clientSecret,
                      String clientName,
                      String authorizationChallengeEndpoint,
                      String endSessionEndpoint) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientName = clientName;
        this.authorizationChallengeEndpoint = authorizationChallengeEndpoint;
        this.endSessionEndpoint = endSessionEndpoint;
    }

    @SerializedName("client_id")
    private String clientId;
    @SerializedName("client_secret")
    private String clientSecret;
    @SerializedName("client_name")
    private String clientName;
    @SerializedName("authorization_challenge_endpoint")
    private String authorizationChallengeEndpoint;
    @SerializedName("end_session_endpoint")
    private String endSessionEndpoint;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getAuthorizationChallengeEndpoint() {
        return authorizationChallengeEndpoint;
    }

    public void setAuthorizationChallengeEndpoint(String authorizationChallengeEndpoint) {
        this.authorizationChallengeEndpoint = authorizationChallengeEndpoint;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }
}
