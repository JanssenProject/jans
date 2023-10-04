package io.jans.chip.modal.appIntegrity;

import com.google.gson.annotations.SerializedName;

public class AppIntegrityResponse {
    @SerializedName("requestDetails")
    private RequestDetails requestDetails;
    @SerializedName("appIntegrity")
    private AppIntegrity appIntegrity;
    @SerializedName("deviceIntegrity")
    private DeviceIntegrity deviceIntegrity;
    @SerializedName("accountDetails")
    private AccountDetails accountDetails;
    @SerializedName("error")
    private String error;


    public RequestDetails getRequestDetails() {
        return requestDetails;
    }

    public void setRequestDetails(RequestDetails requestDetails) {
        this.requestDetails = requestDetails;
    }

    public AppIntegrity getAppIntegrity() {
        return appIntegrity;
    }

    public void setAppIntegrity(AppIntegrity appIntegrity) {
        this.appIntegrity = appIntegrity;
    }

    public DeviceIntegrity getDeviceIntegrity() {
        return deviceIntegrity;
    }

    public void setDeviceIntegrity(DeviceIntegrity deviceIntegrity) {
        this.deviceIntegrity = deviceIntegrity;
    }

    public AccountDetails getAccountDetails() {
        return accountDetails;
    }

    public void setAccountDetails(AccountDetails accountDetails) {
        this.accountDetails = accountDetails;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
