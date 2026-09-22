/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.conf;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
	@DocProperty(description = "MDS TOC root certificates folder")
	private String mdsCertsFolder;
	@DocProperty(description = "MDS TOC files folder")
	private String mdsTocsFolder;

	@DocProperty(description = "Allow to enroll users on enrollment/authentication requests")
	private boolean userAutoEnrollment = false;
	@DocProperty(description = "Expiration time in seconds for pending enrollment/authentication requests")
	private int unfinishedRequestExpiration = 120; // 120 seconds
	@DocProperty(description = "Expiration time in seconds for approved authentication requests")
	private int authenticationHistoryExpiration = 15 * 24 * 3600; // 15 days

	@DocProperty(description = "Boolean value indicating whether assertion ceremonies that lapse without being completed are relabelled as abandoned instead of being deleted unlabelled", defaultValue = "true")
	private boolean recordAbandonedAssertions = true;
	@DocProperty(description = "Expiration time in seconds for abandoned assertion ceremonies. Kept much shorter than authenticationHistoryExpiration because conditional-UI ceremonies start on nearly every login page load, making abandonment the highest-volume outcome", defaultValue = "86400")
	private int abandonedRequestExpiration = 24 * 3600; // 1 day
	@DocProperty(description = "Interval in seconds between sweeps for lapsed assertion ceremonies. Must stay below unfinishedRequestExpiration so a ceremony cannot lapse and be deleted between two sweeps", defaultValue = "30")
	private int abandonedRequestSweepInterval = 30;

	@DocProperty(description = "Authenticators metadata in json format")
	private String serverMetadataFolder;
	@DocProperty(description = "List of Requested Credential Types")
	private List<String> enabledFidoAlgorithms = new ArrayList<>();
	@DocProperty(description = "Authenticators metadata in json format")
	@JsonProperty(value = "rp")
	private List<RequestedParty> requestedParties = new ArrayList<>();
	@DocProperty(description = "String value to provide source of URLs with external metadata")
	private List<MetadataServer> metadataServers = new ArrayList<>();
	@DocProperty(description = "Boolean value indicating whether the MDS download should be omitted", defaultValue = "false")
	private boolean disableMetadataService = false;
	@DocProperty(description = "Number of times the MDS TOC download is retried at server startup when the TOC blob is missing (a missing TOC prevents attestation validation)", defaultValue = "3")
	private int mdsDownloadStartupRetries = 3;
	@DocProperty(description = "Delay in seconds between MDS TOC download retries at server startup when the TOC blob is missing", defaultValue = "30")
	private int mdsDownloadStartupRetryInterval = 30;
	@DocProperty(description = "Hints to the RP - security-key, client-device, hybrid")
	private List<String> hints = new ArrayList<>();
	@DocProperty(description = "If authenticators have been enabled for use in a specific protected envt (enterprise authenticators)", defaultValue = "false")
	private boolean enterpriseAttestation = false;
	@DocProperty(description = "String value indicating whether MDS validation should be omitted during attestation", defaultValue = "monitor")
	private String attestationMode = "monitor";
	@DocProperty(description = "Full origins (scheme, host and optional port) permitted to frame a cross-origin ceremony; empty denies every framed ceremony")
	private List<String> allowedTopOrigins = new ArrayList<>();

	@DocProperty(description = "Boolean value indicating whether passkey registration and authentication events are delivered to the Lock Server as audit evidence", defaultValue = "false")
	private boolean lockAuditEnabled = false;
	@DocProperty(description = "Base URL of the Lock Server audit endpoint (e.g. https://lock.example.com/audit), used to derive /audit/log and /audit/log/bulk")
	private String lockAuditEndpoint;
	@DocProperty(description = "OAuth2 client ID used to obtain a token (scope https://jans.io/oauth/lock/log.write) for posting Lock Server audit events")
	private String lockAuditClientId;
	@DocProperty(description = "OAuth2 client secret (encrypted), paired with lockAuditClientId")
	private String lockAuditClientPassword;
	@DocProperty(description = "Interval in seconds between batched deliveries of buffered Lock Server audit events", defaultValue = "20")
	private int lockAuditFlushInterval = 20;

	public boolean isRecordAbandonedAssertions() {
		return recordAbandonedAssertions;
	}

	public void setRecordAbandonedAssertions(boolean recordAbandonedAssertions) {
		this.recordAbandonedAssertions = recordAbandonedAssertions;
	}

	public int getAbandonedRequestExpiration() {
		return abandonedRequestExpiration;
	}

	public void setAbandonedRequestExpiration(int abandonedRequestExpiration) {
		this.abandonedRequestExpiration = abandonedRequestExpiration;
	}

	public int getAbandonedRequestSweepInterval() {
		return abandonedRequestSweepInterval;
	}

	public void setAbandonedRequestSweepInterval(int abandonedRequestSweepInterval) {
		this.abandonedRequestSweepInterval = abandonedRequestSweepInterval;
	}

	public String getAuthenticatorCertsFolder() {
		return authenticatorCertsFolder;
	}

	public void setAuthenticatorCertsFolder(String authenticatorCertsFolder) {
		this.authenticatorCertsFolder = authenticatorCertsFolder;
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

	public List<String> getAllowedTopOrigins() {
		return allowedTopOrigins;
	}

	public void setAllowedTopOrigins(List<String> allowedTopOrigins) {
		this.allowedTopOrigins = allowedTopOrigins;
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



	public int getAuthenticationHistoryExpiration() {
		return authenticationHistoryExpiration;
	}

	public void setAuthenticationHistoryExpiration(int authenticationHistoryExpiration) {
		this.authenticationHistoryExpiration = authenticationHistoryExpiration;
	}

	

	public boolean isUserAutoEnrollment() {
		return userAutoEnrollment;
	}

	public void setUserAutoEnrollment(boolean userAutoEnrollment) {
		this.userAutoEnrollment = userAutoEnrollment;
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

	public int getMdsDownloadStartupRetries() {
		return mdsDownloadStartupRetries;
	}

	public void setMdsDownloadStartupRetries(int mdsDownloadStartupRetries) {
		this.mdsDownloadStartupRetries = mdsDownloadStartupRetries;
	}

	public int getMdsDownloadStartupRetryInterval() {
		return mdsDownloadStartupRetryInterval;
	}

	public void setMdsDownloadStartupRetryInterval(int mdsDownloadStartupRetryInterval) {
		this.mdsDownloadStartupRetryInterval = mdsDownloadStartupRetryInterval;
	}

	public List<MetadataServer> getMetadataServers() {
		return metadataServers;
	}

	public void setMetadataServers(List<MetadataServer> metadataServers) {
		this.metadataServers = metadataServers;
	}

	public String getAttestationMode() {
		return attestationMode;
	}

	public void setAttestationMode(String attestationMode) {
		this.attestationMode = attestationMode;
	}

	public boolean isLockAuditEnabled() {
		return lockAuditEnabled;
	}

	public void setLockAuditEnabled(boolean lockAuditEnabled) {
		this.lockAuditEnabled = lockAuditEnabled;
	}

	public String getLockAuditEndpoint() {
		return lockAuditEndpoint;
	}

	public void setLockAuditEndpoint(String lockAuditEndpoint) {
		this.lockAuditEndpoint = lockAuditEndpoint;
	}

	public String getLockAuditClientId() {
		return lockAuditClientId;
	}

	public void setLockAuditClientId(String lockAuditClientId) {
		this.lockAuditClientId = lockAuditClientId;
	}

	public String getLockAuditClientPassword() {
		return lockAuditClientPassword;
	}

	public void setLockAuditClientPassword(String lockAuditClientPassword) {
		this.lockAuditClientPassword = lockAuditClientPassword;
	}

	public int getLockAuditFlushInterval() {
		return lockAuditFlushInterval;
	}

	public void setLockAuditFlushInterval(int lockAuditFlushInterval) {
		this.lockAuditFlushInterval = lockAuditFlushInterval;
	}

	public Fido2Configuration() {
		// Default constructor required for JSON (Jackson) deserialization of the FIDO2 configuration.
	}

	@Override
	public String toString() {
		return "Fido2Configuration [authenticatorCertsFolder=" + authenticatorCertsFolder + ", mdsCertsFolder="
				+ mdsCertsFolder + ", mdsTocsFolder=" + mdsTocsFolder + ", userAutoEnrollment="
				+ userAutoEnrollment + ", unfinishedRequestExpiration=" + unfinishedRequestExpiration
				+ ", authenticationHistoryExpiration=" + authenticationHistoryExpiration + ", serverMetadataFolder="
				+ serverMetadataFolder + ", enabledFidoAlgorithms=" + enabledFidoAlgorithms + ", requestedParties="
				+ requestedParties + ", metadataServers=" + metadataServers + ", allowedTopOrigins=" + allowedTopOrigins + ", disableMetadataService="
				+ disableMetadataService + ", mdsDownloadStartupRetries=" + mdsDownloadStartupRetries
				+ ", mdsDownloadStartupRetryInterval=" + mdsDownloadStartupRetryInterval + ", hints=" + hints
				+ ", enterpriseAttestation=" + enterpriseAttestation + ", attestationMode=" + attestationMode
				+ ", lockAuditEnabled=" + lockAuditEnabled + ", lockAuditEndpoint=" + lockAuditEndpoint
				+ ", lockAuditFlushInterval=" + lockAuditFlushInterval + "]"; // lockAuditClientPassword deliberately excluded, see #14676
	}

}
