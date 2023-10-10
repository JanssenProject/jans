package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    public TokenResponse(String accessToken,
                         String idToken,
                         String tokenType) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.tokenType = tokenType;
    }

    public TokenResponse() {
    }

    public TokenResponse(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }

    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("id_token")
    private String idToken;
    @SerializedName("token_type")
    private String tokenType;
    private boolean isSuccessful;
    private OperationError operationError;

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
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
}
