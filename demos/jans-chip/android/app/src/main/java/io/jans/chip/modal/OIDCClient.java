package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

public class OIDCClient {
    @SerializedName("sno")
    private String sno;
    @SerializedName("CLIENT_NAME")
    private String clientName;
    @SerializedName("CLIENT_ID")
    private String clientId;
    @SerializedName("CLIENT_SECRET")
    private String clientSecret;
    @SerializedName("RECENT_GENERATED_ID_TOKEN")
    private String recentGeneratedIdToken;
    @SerializedName("RECENT_GENERATED_ACCESS_TOKEN")
    private String recentGeneratedAccessToken;
    @SerializedName("scope")
    private String scope;

    public String getRecentGeneratedAccessToken() {
        return recentGeneratedAccessToken;
    }

    public void setRecentGeneratedAccessToken(String recentGeneratedAccessToken) {
        this.recentGeneratedAccessToken = recentGeneratedAccessToken;
    }

    public String getRecentGeneratedIdToken() {
        return recentGeneratedIdToken;
    }

    public void setRecentGeneratedIdToken(String recentGeneratedIdToken) {
        this.recentGeneratedIdToken = recentGeneratedIdToken;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSno() {
        return sno;
    }

    public void setSno(String sno) {
        this.sno = sno;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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

    @Override
    public String toString() {
        return "OIDCClient{" +
                "sno='" + sno + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", recentGeneratedIdToken='" + recentGeneratedIdToken + '\'' +
                ", scope='" + scope + '\'' +
                '}';
    }
}
