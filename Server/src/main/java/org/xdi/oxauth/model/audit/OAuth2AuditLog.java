package org.xdi.oxauth.model.audit;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.common.AuthorizationGrant;

import java.util.Date;


public class OAuth2AuditLog {

    private String ip;
    private Date timestamp;
    private boolean isSuccess;

    private String clientId;
    private Action action;
    private String username;
    private String scope;

    public OAuth2AuditLog(String ip, Action action) {
        this.ip = ip;
        this.action = action;
        this.isSuccess = false;
        this.timestamp = new Date();
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

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}