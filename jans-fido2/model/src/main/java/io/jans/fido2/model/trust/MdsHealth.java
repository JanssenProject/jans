/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Health of the FIDO Metadata Service data used for attestation validation. A stale or failed MDS load
 * is a common cause of a previously valid authenticator suddenly being rejected, and is otherwise only
 * visible in the server log.
 * <p>
 * Date and time fields are pre-formatted strings: the FIDO2 {@code DataMapperService} uses a plain
 * {@code ObjectMapper} with no JSR-310 module registered, so {@code java.time} values must not be
 * handed to it directly.
 *
 * @author Janssen Project
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MdsHealth {

    private MdsHealthStatus status;

    private boolean metadataServiceDisabled;

    private int tocEntryCount;

    private String nextUpdate;

    private boolean blobExpired;

    private String lastSuccessfulRefresh;

    private String lastRefreshError;

    private List<MetadataServerStatus> metadataServers = new ArrayList<>();

    private String timestamp;

    public MdsHealthStatus getStatus() {
        return status;
    }

    public void setStatus(MdsHealthStatus status) {
        this.status = status;
    }

    public boolean isMetadataServiceDisabled() {
        return metadataServiceDisabled;
    }

    public void setMetadataServiceDisabled(boolean metadataServiceDisabled) {
        this.metadataServiceDisabled = metadataServiceDisabled;
    }

    /** Authenticator metadata entries currently loaded in memory. */
    public int getTocEntryCount() {
        return tocEntryCount;
    }

    public void setTocEntryCount(int tocEntryCount) {
        this.tocEntryCount = tocEntryCount;
    }

    /** The {@code nextUpdate} declared by the loaded TOC blob (ISO local date), or null if none is loaded. */
    public String getNextUpdate() {
        return nextUpdate;
    }

    public void setNextUpdate(String nextUpdate) {
        this.nextUpdate = nextUpdate;
    }

    /** True when no blob is loaded, or its {@code nextUpdate} is today or earlier — a re-download is due. */
    public boolean isBlobExpired() {
        return blobExpired;
    }

    public void setBlobExpired(boolean blobExpired) {
        this.blobExpired = blobExpired;
    }

    /** When metadata was last downloaded and parsed successfully (ISO local date-time, UTC). */
    public String getLastSuccessfulRefresh() {
        return lastSuccessfulRefresh;
    }

    public void setLastSuccessfulRefresh(String lastSuccessfulRefresh) {
        this.lastSuccessfulRefresh = lastSuccessfulRefresh;
    }

    /** Message from the most recent failed refresh; absent when the last refresh succeeded. */
    public String getLastRefreshError() {
        return lastRefreshError;
    }

    public void setLastRefreshError(String lastRefreshError) {
        this.lastRefreshError = lastRefreshError;
    }

    public List<MetadataServerStatus> getMetadataServers() {
        return metadataServers;
    }

    public void setMetadataServers(List<MetadataServerStatus> metadataServers) {
        this.metadataServers = metadataServers;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MdsHealth [status=" + status + ", metadataServiceDisabled=" + metadataServiceDisabled
                + ", tocEntryCount=" + tocEntryCount + ", nextUpdate=" + nextUpdate + ", blobExpired=" + blobExpired
                + ", lastSuccessfulRefresh=" + lastSuccessfulRefresh + ", lastRefreshError=" + lastRefreshError
                + ", metadataServers=" + metadataServers + ", timestamp=" + timestamp + "]";
    }
}
