package org.gluu.configapi.rest.model;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

public class Fido2Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Size(min = 1)
    private String authenticatorCertsFolder;

    /*
     * @NotBlank
     *
     * @Size(min=1)
     */
    private String mdsAccessToken;

    @NotBlank
    @Size(min = 1)
    private String mdsCertsFolder;

    @NotBlank
    @Size(min = 1)
    private String mdsTocsFolder;

    @NotBlank
    @Size(min = 1)
    private String serverMetadataFolder;

    @NotEmpty
    @Size(min = 1)
    private List<String> requestedCredentialTypes;

    @NotEmpty
    @Size(min = 1)
    private List<RequestedParties> requestedParties;

    private Boolean userAutoEnrollment;

    @Min(value = 0)
    @Max(value = 2147483647)
    @Digits(integer = 10, fraction = 0)
    private Integer unfinishedRequestExpiration;

    @Min(value = 0)
    @Max(value = 2147483647)
    @Digits(integer = 10, fraction = 0)
    private Integer authenticationHistoryExpiration;

    private Boolean disableFido2;

    public String getAuthenticatorCertsFolder() {
        return authenticatorCertsFolder;
    }

    public void setAuthenticatorCertsFolder(String authenticatorCertsFolder) {
        this.authenticatorCertsFolder = authenticatorCertsFolder;
    }

    public String getMdsAccessToken() {
        return mdsAccessToken;
    }

    public void setMdsAccessToken(String mdsAccessToken) {
        this.mdsAccessToken = mdsAccessToken;
    }

    public String getMdsCertsFolder() {
        return mdsCertsFolder;
    }

    public void setMdsCertsFolder(String mdsCertsFolder) {
        this.mdsCertsFolder = mdsCertsFolder;
    }

    public String getMdsTocsFolder() {
        return mdsTocsFolder;
    }

    public void setMdsTocsFolder(String mdsTocsFolder) {
        this.mdsTocsFolder = mdsTocsFolder;
    }

    public String getServerMetadataFolder() {
        return serverMetadataFolder;
    }

    public void setServerMetadataFolder(String serverMetadataFolder) {
        this.serverMetadataFolder = serverMetadataFolder;
    }

    public List<String> getRequestedCredentialTypes() {
        return requestedCredentialTypes;
    }

    public void setRequestedCredentialTypes(List<String> requestedCredentialTypes) {
        this.requestedCredentialTypes = requestedCredentialTypes;
    }

    public List<RequestedParties> getRequestedParties() {
        return requestedParties;
    }

    public void setRequestedParties(List<RequestedParties> requestedParties) {
        this.requestedParties = requestedParties;
    }

    public Boolean getUserAutoEnrollment() {
        return userAutoEnrollment;
    }

    public void setUserAutoEnrollment(Boolean userAutoEnrollment) {
        this.userAutoEnrollment = userAutoEnrollment;
    }

    public Integer getUnfinishedRequestExpiration() {
        return unfinishedRequestExpiration;
    }

    public void setUnfinishedRequestExpiration(Integer unfinishedRequestExpiration) {
        this.unfinishedRequestExpiration = unfinishedRequestExpiration;
    }

    public Integer getAuthenticationHistoryExpiration() {
        return authenticationHistoryExpiration;
    }

    public void setAuthenticationHistoryExpiration(Integer authenticationHistoryExpiration) {
        this.authenticationHistoryExpiration = authenticationHistoryExpiration;
    }

    public Boolean getDisableFido2() {
        return disableFido2;
    }

    public void setDisableFido2(Boolean disableFido2) {
        this.disableFido2 = disableFido2;
    }

    @Override
    public String toString() {
        return "Fido2Configuration [authenticatorCertsFolder=" + authenticatorCertsFolder + ", mdsAccessToken="
                + mdsAccessToken + ", mdsCertsFolder=" + mdsCertsFolder + ", mdsTocsFolder=" + mdsTocsFolder
                + ", serverMetadataFolder=" + serverMetadataFolder + ", requestedCredentialTypes="
                + requestedCredentialTypes + ", requestedParties=" + requestedParties + ", userAutoEnrollment="
                + userAutoEnrollment + ", unfinishedRequestExpiration=" + unfinishedRequestExpiration
                + ", authenticationHistoryExpiration=" + authenticationHistoryExpiration + ", disableFido2="
                + disableFido2 + "]";
    }


}
