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

    public AuditLogEntry(String ip, AuditActionType action) {
        this.ip = ip;
        this.action = action;
        this.timestamp = new Date();
        this.macAddress = InetAddressUtility.getMACAddressOrNull();
        this.isSuccess = false;
    }

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

}