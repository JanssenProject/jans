package io.jans.ca.plugin.adminui.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

public class GenericResponse {
    private boolean success;
    private String responseMessage;
    private int responseCode;

    public JsonNode getResponseObject() {
        return responseObject;
    }

    /**
     * Sets the JSON payload to include in this GenericResponse.
     *
     * @param responseObject the JSON payload to store as the response object
     */
    public void setResponseObject(JsonNode responseObject) {
        this.responseObject = responseObject;
    }

    private JsonNode responseObject;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private byte[] responseBytes;

    /**
     * Gets the human-readable response message.
     *
     * @return the response message, or `null` if none has been set
     */
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

    /**
     * Set whether this response indicates a successful outcome.
     *
     * @param success `true` if the response indicates success, `false` otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Provides the raw byte payload of this response.
     *
     * @return the raw response bytes, or {@code null} if no bytes are set
     */
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    /**
     * Set the raw binary payload for the response.
     *
     * @param responseBytes the raw payload bytes to store, or {@code null} if none
     */
    public void setResponseBytes(byte[] responseBytes) {
        this.responseBytes = responseBytes;
    }

    /**
     * Produce a string representation of this GenericResponse including its primary fields.
     *
     * The returned string contains the values of `success`, `responseMessage`, `responseCode`,
     * and `responseObject`. The `responseBytes` field is intentionally omitted.
     *
     * @return a string containing the values of `success`, `responseMessage`, `responseCode`, and `responseObject`
     */
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
