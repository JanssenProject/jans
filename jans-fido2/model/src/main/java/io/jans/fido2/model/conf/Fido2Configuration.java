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
    @DocProperty(description = "String value to provide source of URLs with external metadata")
    private String metadataUrlsProvider;
    @DocProperty(description = "Boolean value indicating whether the MDS download should be omitted")
    private boolean skipDownloadMdsEnabled = false;
    @DocProperty(description = "Boolean value indicating whether MDS validation should be omitted during attestation")
    private boolean skipAttestation = false;
    @DocProperty(description = "Hints to the RP - security-key, client-device, hybrid")
    private List<String> hints = new ArrayList<String>();
    @DocProperty(description = "If authenticators have been enabled for use in a specific protected envt (enterprise authenticators)")
    private boolean enterpriseAttestation = false;
    
    
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

    public String getMetadataUrlsProvider() {
        return metadataUrlsProvider;
    }

    public void setMetadataUrlsProvider(String metadataUrlsProvider) {
        this.metadataUrlsProvider = metadataUrlsProvider;
    }

    public boolean isSkipDownloadMdsEnabled() {
        return skipDownloadMdsEnabled;
    }

    public void setSkipDownloadMdsEnabled(boolean skipDownloadMdsEnabled) {
        this.skipDownloadMdsEnabled = skipDownloadMdsEnabled;
    }

	public boolean isSkipAttestation() {
		return skipAttestation;
	}

	public void setSkipAttestation(boolean skipAttestation) {
		this.skipAttestation = skipAttestation;
	}

	public List<String> getHints() {
		return hints;
	}

	public void setHints(List<String> hints) {
		this.hints = hints;
	}

	public boolean isEnterpriseAttestation() {
		return enterpriseAttestation;
	}

	public void setEnterpriseAttestation(boolean enterpriseOnly) {
		this.enterpriseAttestation = enterpriseOnly;
	}

	@Override
	public String toString() {
		return "Fido2Configuration [authenticatorCertsFolder=" + authenticatorCertsFolder + ", mdsAccessToken="
				+ mdsAccessToken + ", mdsCertsFolder=" + mdsCertsFolder + ", mdsTocsFolder=" + mdsTocsFolder
				+ ", checkU2fAttestations=" + checkU2fAttestations + ", userAutoEnrollment=" + userAutoEnrollment
				+ ", unfinishedRequestExpiration=" + unfinishedRequestExpiration + ", authenticationHistoryExpiration="
				+ authenticationHistoryExpiration + ", serverMetadataFolder=" + serverMetadataFolder
				+ ", requestedCredentialTypes=" + requestedCredentialTypes + ", requestedParties=" + requestedParties
				+ ", metadataUrlsProvider=" + metadataUrlsProvider + ", skipDownloadMdsEnabled="
				+ skipDownloadMdsEnabled + ", skipAttestation=" + skipAttestation + ", hints=" + hints
				+ ", enterpriseAttestation=" + enterpriseAttestation + "]";
	}

	

	
 }
