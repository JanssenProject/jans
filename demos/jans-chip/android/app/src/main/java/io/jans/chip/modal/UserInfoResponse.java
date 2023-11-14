package io.jans.chip.modal;

import com.fasterxml.jackson.databind.JsonNode;

import org.json.JSONObject;

public class UserInfoResponse {
    Object reponse;
    private boolean isSuccessful;
    private OperationError operationError;

    public UserInfoResponse(Object reponse) {
        this.reponse = reponse;
    }

    public UserInfoResponse(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }

    public UserInfoResponse() {
    }

    public Object getReponse() {
        return reponse;
    }

    public void setReponse(Object reponse) {
        this.reponse = reponse;
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
