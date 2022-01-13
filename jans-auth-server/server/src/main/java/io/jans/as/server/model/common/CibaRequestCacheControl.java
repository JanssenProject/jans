/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.util.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Class used to keep all data about a CIBA request that should be processed and saved in Cache.
 *
 * @author Milton BO
 * @version June 2, 2020
 */
public class CibaRequestCacheControl implements Serializable {

    private String authReqId;
    private User user;
    private Client client;
    private List<String> scopes;

    private int expiresIn = 1;
    private String clientNotificationToken;
    private String bindingMessage;
    private Long lastAccessControl;
    private CibaRequestStatus status;
    private boolean tokensDelivered;
    private String acrValues;

    public CibaRequestCacheControl() {
    }

    public CibaRequestCacheControl(User user, Client client, int expiresIn, List<String> scopeList,
                                   String clientNotificationToken, String bindingMessage, Long lastAccessControl,
                                   String acrValues) {
        this.authReqId = StringUtils.generateRandomCode((byte) 24); // Entropy above of 160 bits based on specs [RFC section 7.3]
        this.user = user;
        this.client = client;
        this.scopes = scopeList;
        this.status = CibaRequestStatus.PENDING;
        this.expiresIn = expiresIn;
        this.clientNotificationToken = clientNotificationToken;
        this.bindingMessage = bindingMessage;
        this.lastAccessControl = lastAccessControl;
        this.tokensDelivered = false;
        this.acrValues = acrValues;
    }

    public String cacheKey() {
        return authReqId;
    }

    public String getScopesAsString() {
        final StringBuilder scopes = new StringBuilder();
        for (String s : getScopes()) {
            scopes.append(s).append(" ");
        }
        return scopes.toString().trim();
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public Long getLastAccessControl() {
        return lastAccessControl;
    }

    public void setLastAccessControl(Long lastAccessControl) {
        this.lastAccessControl = lastAccessControl;
    }

    public CibaRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CibaRequestStatus status) {
        this.status = status;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    @Override
    public String toString() {
        return "CibaRequestCacheControl{" +
                ", authReqId='" + authReqId + '\'' +
                ", user=" + user +
                ", client=" + client +
                ", scopes=" + scopes +
                ", expiresIn=" + expiresIn +
                ", clientNotificationToken='" + clientNotificationToken + '\'' +
                ", bindingMessage='" + bindingMessage + '\'' +
                ", lastAccessControl=" + lastAccessControl +
                ", userAuthorization=" + status +
                ", tokensDelivered=" + tokensDelivered +
                ", acrValues='" + acrValues + '\'' +
                '}';
    }
}