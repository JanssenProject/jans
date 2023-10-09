package io.jans.chip.modal;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import io.jans.chip.modal.OperationError;
@Entity(tableName = "OP_CONFIGURATION")
public class OPConfiguration {
    public OPConfiguration() {
    }

    public OPConfiguration(String sno,
                           String issuer,
                           String registrationEndpoint,
                           String tokenEndpoint,
                           String userinfoEndpoint,
                           String authorizationChallengeEndpoint,
                           String revocationEndpoint,
                           boolean isSuccessful) {
        this.sno = sno;
        this.issuer = issuer;
        this.registrationEndpoint = registrationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userinfoEndpoint = userinfoEndpoint;
        this.authorizationChallengeEndpoint = authorizationChallengeEndpoint;
        this.revocationEndpoint = revocationEndpoint;
        this.isSuccessful = isSuccessful;
    }

    public OPConfiguration(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }
    @NonNull
    @PrimaryKey
    @SerializedName("SNO")
    private String sno;
    @ColumnInfo(name = "ISSUER")
    @SerializedName("issuer")
    private String issuer;
    @ColumnInfo(name = "REGISTRATION_ENDPOINT")
    @SerializedName("registration_endpoint")
    private String registrationEndpoint;
    @ColumnInfo(name = "TOKEN_ENDPOINT")
    @SerializedName("token_endpoint")
    private String tokenEndpoint;
    @ColumnInfo(name = "USERINFO_ENDPOINT")
    @SerializedName("userinfo_endpoint")
    private String userinfoEndpoint;
    @ColumnInfo(name = "AUTHORIZATION_CHALLENGE_ENDPOINT")
    @SerializedName("authorization_challenge_endpoint")
    private String authorizationChallengeEndpoint;
    @ColumnInfo(name = "REVOCATION_ENDPOINT")
    @SerializedName("revocation_endpoint")
    private String revocationEndpoint;
    @Ignore
    private boolean isSuccessful;
    @Ignore
    private OperationError operationError;
    public String getRevocationEndpoint() {
        return revocationEndpoint;
    }

    public void setRevocationEndpoint(String revocationEndpoint) {
        this.revocationEndpoint = revocationEndpoint;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizationChallengeEndpoint() {
        return authorizationChallengeEndpoint;
    }
    public void setAuthorizationChallengeEndpoint(String authorizationChallengeEndpoint) {
        this.authorizationChallengeEndpoint = authorizationChallengeEndpoint;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
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

    @NonNull
    public String getSno() {
        return sno;
    }

    public void setSno(@NonNull String sno) {
        this.sno = sno;
    }

    @Override
    public String toString() {
        return "OPConfiguration{" +
                "issuer='" + issuer + '\'' +
                ", registrationEndpoint='" + registrationEndpoint + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", userinfoEndpoint='" + userinfoEndpoint + '\'' +
                ", authorizationChallengeEndpoint='" + authorizationChallengeEndpoint + '\'' +
                '}';
    }
}
