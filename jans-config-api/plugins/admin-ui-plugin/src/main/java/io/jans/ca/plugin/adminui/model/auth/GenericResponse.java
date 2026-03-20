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
    private byte[] responseBytes;

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

    public byte[] getResponseBytes() {
        return responseBytes;
    }

    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
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
