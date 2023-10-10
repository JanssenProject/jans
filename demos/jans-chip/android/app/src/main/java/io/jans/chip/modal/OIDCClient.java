package io.jans.chip.modal;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "OIDC_CLIENT")
public class OIDCClient {
    public OIDCClient() {
    }

    public OIDCClient(String sno,
                      String clientName,
                      String clientId,
                      String clientSecret,
                      String recentGeneratedIdToken,
                      String recentGeneratedAccessToken,
                      String scope,
                      boolean isSuccessful) {
        this.sno = sno;
        this.clientName = clientName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.recentGeneratedIdToken = recentGeneratedIdToken;
        this.recentGeneratedAccessToken = recentGeneratedAccessToken;
        this.scope = scope;
        this.isSuccessful = isSuccessful;
    }

    public OIDCClient(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }
    @NonNull
    @PrimaryKey
    @SerializedName("SNO")
    private String sno;
    @ColumnInfo(name = "CLIENT_NAME")
    @SerializedName("CLIENT_NAME")
    private String clientName;
    @ColumnInfo(name = "CLIENT_ID")
    @SerializedName("CLIENT_ID")
    private String clientId;
    @ColumnInfo(name = "CLIENT_SECRET")
    @SerializedName("CLIENT_SECRET")
    private String clientSecret;
    @ColumnInfo(name = "RECENT_GENERATED_ID_TOKEN")
    @SerializedName("RECENT_GENERATED_ID_TOKEN")
    private String recentGeneratedIdToken;
    @ColumnInfo(name = "RECENT_GENERATED_ACCESS_TOKEN")
    @SerializedName("RECENT_GENERATED_ACCESS_TOKEN")
    private String recentGeneratedAccessToken;
    @SerializedName("scope")
    private String scope;
    @Ignore
    private boolean isSuccessful;
    @Ignore
    private OperationError operationError;

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

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public OperationError getOperationError() {
        return operationError;
    }

    public void setOperationError(OperationError operationError) {
        this.operationError = operationError;
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
