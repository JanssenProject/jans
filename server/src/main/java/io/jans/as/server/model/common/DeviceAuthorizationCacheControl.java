/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.registration.Client;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Class used to keep all data about an OAuth2 Device Flow request.
 */
public class DeviceAuthorizationCacheControl implements Serializable {

    private String userCode;
    private String deviceCode;
    private Client client;
    private List<String> scopes;
    private URI verificationUri;
    private int expiresIn = 1;
    private int interval = 5;
    private long lastAccessControl;
    private DeviceAuthorizationStatus status;

    public DeviceAuthorizationCacheControl() {
    }

    public DeviceAuthorizationCacheControl(String userCode, String deviceCode, Client client, List<String> scopes,
                                           URI verificationUri, int expiresIn, int interval, long lastAccessControl,
                                           DeviceAuthorizationStatus status) {
        this.userCode = userCode;
        this.deviceCode = deviceCode;
        this.client = client;
        this.scopes = scopes;
        this.verificationUri = verificationUri;
        this.expiresIn = expiresIn;
        this.interval = interval;
        this.lastAccessControl = lastAccessControl;
        this.status = status;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public URI getVerificationUri() {
        return verificationUri;
    }

    public void setVerificationUri(URI verificationUri) {
        this.verificationUri = verificationUri;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public Long getLastAccessControl() {
        return lastAccessControl;
    }

    public void setLastAccessControl(long lastAccessControl) {
        this.lastAccessControl = lastAccessControl;
    }

    public DeviceAuthorizationStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceAuthorizationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DeviceAuthorizationCacheControl{" +
                "userCode='" + userCode + '\'' +
                ", deviceCode='" + deviceCode + '\'' +
                ", client=" + client +
                ", scopes=" + scopes +
                ", verificationUri='" + verificationUri + '\'' +
                ", expiresIn=" + expiresIn +
                ", interval=" + interval +
                ", lastAccessControl=" + lastAccessControl +
                ", status=" + status +
                '}';
    }
}