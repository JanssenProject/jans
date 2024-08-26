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
    private boolean debugUserAutoEnrollment = false;

    @DocProperty(description = "Expiration time in seconds for pending enrollment/authentication requests")
    private int unfinishedRequestExpiration = 120; // 120 seconds

    @DocProperty(description = "Expiration time in seconds for approved authentication requests")
    private int metadataRefreshInterval= 15 * 24 * 3600; // 15 days
    @DocProperty(description = "Authenticators metadata in json format")
    private String serverMetadataFolder;
    @DocProperty(description = "List of Requested Credential Types")
    private List<String> enabledFidoAlgorithms = new ArrayList<String>();
    @DocProperty(description = "Authenticators metadata in json format")
    private List<RequestedParty> requestedParties = new ArrayList<RequestedParty>();

    @DocProperty(description = "String value to provide source of URLs with external metadata")
    private List<MetadataServer> metadataServers = new ArrayList<MetadataServer>();
    @DocProperty(description = "Boolean value indicating whether the MDS download should be omitted")
    private boolean disableMetadataService = false;
    @DocProperty(description = "Boolean value indicating whether MDS validation should be omitted during attestation")
    private boolean skipValidateMdsInAttestationEnabled = false;
    @DocProperty(description = "Boolean value indicating whether the assertion custom endpoint (used especially in passkey) is enabled.")
    private boolean assertionOptionsGenerateEndpointEnabled = false;

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

    public int getUnfinishedRequestExpiration() {
        return unfinishedRequestExpiration;
    }

    public void setUnfinishedRequestExpiration(int unfinishedRequestExpiration) {
        this.unfinishedRequestExpiration = unfinishedRequestExpiration;
    }

    public String getServerMetadataFolder() {
        return serverMetadataFolder;
    }

    public void setServerMetadataFolder(String serverMetadataFolder) {
        this.serverMetadataFolder = serverMetadataFolder;
    }
	public List<RequestedParty> getRequestedParties() {
		return requestedParties;
	}

	public void setRequestedParties(List<RequestedParty> requestedParties) {
		this.requestedParties = requestedParties;
	}

    public boolean isSkipValidateMdsInAttestationEnabled() {
        return skipValidateMdsInAttestationEnabled;
    }

    public void setSkipValidateMdsInAttestationEnabled(boolean skipValidateMdsInAttestationEnabled) {
        this.skipValidateMdsInAttestationEnabled = skipValidateMdsInAttestationEnabled;
    }

    public boolean isAssertionOptionsGenerateEndpointEnabled() {
        return assertionOptionsGenerateEndpointEnabled;
    }

    public void setAssertionOptionsGenerateEndpointEnabled(boolean assertionOptionsGenerateEndpointEnabled) {
        this.assertionOptionsGenerateEndpointEnabled = assertionOptionsGenerateEndpointEnabled;
    }

    public int getMetadataRefreshInterval() {
        return metadataRefreshInterval;
    }

    public void setMetadataRefreshInterval(int metadataRefreshInterval) {
        this.metadataRefreshInterval = metadataRefreshInterval;
    }

    public boolean isDebugUserAutoEnrollment() {
        return debugUserAutoEnrollment;
    }

    public void setDebugUserAutoEnrollment(boolean debugUserAutoEnrollment) {
        this.debugUserAutoEnrollment = debugUserAutoEnrollment;
    }

    public List<String> getEnabledFidoAlgorithms() {
        return enabledFidoAlgorithms;
    }

    public void setEnabledFidoAlgorithms(List<String> enabledFidoAlgorithms) {
        this.enabledFidoAlgorithms = enabledFidoAlgorithms;
    }

    public boolean isDisableMetadataService() {
        return disableMetadataService;
    }

    public void setDisableMetadataService(boolean disableMetadataService) {
        this.disableMetadataService = disableMetadataService;
    }

    public List<MetadataServer> getMetadataServers() {
        return metadataServers;
    }

    public void setMetadataServers(List<MetadataServer> metadataServers) {
        this.metadataServers = metadataServers;
    }
}
