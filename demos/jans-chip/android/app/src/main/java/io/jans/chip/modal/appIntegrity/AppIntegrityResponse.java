package io.jans.chip.modal.appIntegrity;

import com.google.gson.annotations.SerializedName;

import io.jans.chip.modal.OperationError;

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
    private boolean isSuccessful;
    private OperationError operationError;

    public AppIntegrityResponse(RequestDetails requestDetails, AppIntegrity appIntegrity, DeviceIntegrity deviceIntegrity, AccountDetails accountDetails, String error) {
        this.requestDetails = requestDetails;
        this.appIntegrity = appIntegrity;
        this.deviceIntegrity = deviceIntegrity;
        this.accountDetails = accountDetails;
        this.error = error;
    }

    public AppIntegrityResponse(boolean isSuccessful, OperationError operationError) {
        this.isSuccessful = isSuccessful;
        this.operationError = operationError;
    }

    public AppIntegrityResponse() {
    }

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
