/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.doc.annotation.DocProperty;

/**
 * FIDO 2 configuration
 *
 * @author Yuriy Movchan Date: 11/05/2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2Configuration {

    @DocProperty(description = "Authenticators certificates folder")
    private String authenticatorCertsFolder;
    @DocProperty(description = "MDS access token")
    private String mdsAccessToken;
    @DocProperty(description = "MDS TOC root certificates folder")
    private String mdsCertsFolder;
    @DocProperty(description = "MDS TOC files folder")
    private String mdsTocsFolder;
    @DocProperty(description = "Boolean value indicating if U2f attestation needs to be checked")
    private boolean checkU2fAttestations = false;
    @DocProperty(description = "Allow to enroll users on enrollment/authentication requests")
    private boolean userAutoEnrollment = false;

    @DocProperty(description = "Expiration time in seconds for pending enrollment/authentication requests")
    private int unfinishedRequestExpiration = 120; // 120 seconds
    @DocProperty(description = "Expiration time in seconds for approved authentication requests")
    private int authenticationHistoryExpiration = 15 * 24 * 3600; // 15 days
    @DocProperty(description = "Authenticators metadata in json format")
    private String serverMetadataFolder;
    @DocProperty(description = "List of Requested Credential Types")
    private List<String> requestedCredentialTypes = new ArrayList<String>();
    @DocProperty(description = "Authenticators metadata in json format")
    private List<RequestedParty> requestedParties = new ArrayList<RequestedParty>();

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

    public boolean isCheckU2fAttestations() {
		return checkU2fAttestations;
	}

	public void setCheckU2fAttestations(boolean checkU2fAttestations) {
		this.checkU2fAttestations = checkU2fAttestations;
	}

	public boolean isUserAutoEnrollment() {
        return userAutoEnrollment;
    }

    public void setUserAutoEnrollment(boolean userAutoEnrollment) {
        this.userAutoEnrollment = userAutoEnrollment;
    }

    public int getUnfinishedRequestExpiration() {
        return unfinishedRequestExpiration;
    }

    public void setUnfinishedRequestExpiration(int unfinishedRequestExpiration) {
        this.unfinishedRequestExpiration = unfinishedRequestExpiration;
    }

    public int getAuthenticationHistoryExpiration() {
        return authenticationHistoryExpiration;
    }

    public void setAuthenticationHistoryExpiration(int authenticationHistoryExpiration) {
        this.authenticationHistoryExpiration = authenticationHistoryExpiration;
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

	public List<RequestedParty> getRequestedParties() {
		return requestedParties;
	}

	public void setRequestedParties(List<RequestedParty> requestedParties) {
		this.requestedParties = requestedParties;
	}

}
