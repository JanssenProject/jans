/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.audit;

import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.net.InetAddressUtility;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;


public class OAuth2AuditLog {

    private final String ip;
    private final Action action;
    private final Date timestamp;
    private final String macAddress;
    private boolean isSuccess;


    private String clientId;

    private String username;
    private String scope;

    public OAuth2AuditLog(String ip, Action action) {
        this.ip = ip;
        this.action = action;
        this.timestamp = new Date();
        this.macAddress = InetAddressUtility.getMACAddressOrNull();
        this.isSuccess = false;
    }

    public void updateOAuth2AuditLog(AuthorizationGrant authorizationGrant, boolean success) {
        this.setClientId(authorizationGrant.getClientId());
        this.setUsername(authorizationGrant.getUserId());
        this.setScope(StringUtils.join(authorizationGrant.getScopes(), " "));
        this.setSuccess(success);
    }

    public String getIp() {
        return ip;
    }

    public Action getAction() {
        return action;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}