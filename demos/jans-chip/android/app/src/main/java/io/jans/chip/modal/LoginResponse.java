package io.jans.chip.modal;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    public LoginResponse() {
    }

    public LoginResponse(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }

    public LoginResponse(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @SerializedName("authorization_code")
    private String authorizationCode;
    private boolean isSuccessful;
    private OperationError operationError;

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
    public OperationError getOperationError() {
        return operationError;
    }

    public void setOperationError(OperationError operationError) {
        this.operationError = operationError;
    }
    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }
}
