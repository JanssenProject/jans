package io.jans.ca.plugin.adminui.model.auth;

import com.fasterxml.jackson.databind.JsonNode;

public class GenericResponse {
    private boolean success;
    private String responseMessage;
    private int responseCode;

    public JsonNode getResponseObject() {
        return responseObject;
    }

    public void setResponseObject(JsonNode responseObject) {
        this.responseObject = responseObject;
    }

    private JsonNode responseObject;

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "GenericResponse{" +
                "success=" + success +
                ", responseMessage='" + responseMessage + '\'' +
                ", responseCode=" + responseCode +
                ", responseObject=" + responseObject +
                '}';
    }
}
