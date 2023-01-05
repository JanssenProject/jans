package io.jans.ca.plugin.adminui.model.auth;

public class LicenseApiResponse {
    private boolean apiResult;
    private String responseMessage;
    private int responseCode;

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

    public boolean isApiResult() {
        return apiResult;
    }

    public void setApiResult(boolean apiResult) {
        this.apiResult = apiResult;
    }

    @Override
    public String toString() {
        return "LicenseApiResponse{" +
                "apiResult=" + apiResult +
                ", responseMessage='" + responseMessage + '\'' +
                ", responseCode=" + responseCode +
                '}';
    }
}
