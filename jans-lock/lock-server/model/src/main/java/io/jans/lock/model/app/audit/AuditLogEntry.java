package io.jans.lock.model.app.audit;

import java.util.Date;

/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

import io.jans.net.InetAddressUtility;

/**
 * @version November 03, 2025
 */
public class AuditLogEntry {

    private final String ip;
    private final AuditActionType action;
    private final Date timestamp;
    private final String macAddress;
    private boolean isSuccess;

    private String clientId;

    private String username;
    private String scope;

    private Object resource;

    public AuditLogEntry(String ip, AuditActionType action) {
        this.ip = ip;
        this.action = action;
        this.timestamp = new Date();
        this.macAddress = InetAddressUtility.getMACAddressOrNull();
        this.isSuccess = false;
		this.resource = null;
    }
/*
    public void updateAuditLogEntry(AuthorizationGrant authorizationGrant, boolean success) {
        this.setClientId(authorizationGrant.getClientId());
        this.setUsername(authorizationGrant.getUserId());
        this.setScope(StringUtils.join(authorizationGrant.getScopes(), " "));
        this.setSuccess(success);
    }
*/
    public String getIp() {
        return ip;
    }

    public AuditActionType getAction() {
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

    public Object getResource() {
		return resource;
	}

	public void setResource(Object resource) {
		this.resource = resource;
	}

}