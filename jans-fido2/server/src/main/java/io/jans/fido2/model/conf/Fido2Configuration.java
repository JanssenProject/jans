/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * FIDO 2 configuration
 *
 * @author Yuriy Movchan Date: 11/05/2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2Configuration {

    private String authenticatorCertsFolder;

    private String mdsAccessToken;
    private String mdsCertsFolder;
    private String mdsTocsFolder;

    private boolean checkU2fAttestations = false;

    private boolean userAutoEnrollment = false;

    private int unfinishedRequestExpiration = 120; // 120 seconds
    private int authenticationHistoryExpiration = 15 * 24 * 3600; // 15 days

    private String serverMetadataFolder;
    
    private List<String> requestedCredentialTypes = new ArrayList<String>();

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
