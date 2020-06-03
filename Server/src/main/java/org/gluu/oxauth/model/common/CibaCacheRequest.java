/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.registration.Client;

import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version May 5, 2020
 */
public class CibaCacheRequest implements Serializable {

    private CIBAAuthenticationRequestId cibaAuthenticationRequestId;
    private String authorizationRequestId;
    private User user;
    private Client client;
    private List<String> scopes;

    private int expiresIn = 1;
    private String clientNotificationToken;
    private String bindingMessage;
    private Long lastAccessControl;
    private CIBARequestStatus requestStatus;
    private boolean tokensDelivered;
    private String acrValues;

    public CibaCacheRequest() {
    }

    public CibaCacheRequest(User user, Client client, int expiresIn, List<String> scopeList,
                            String clientNotificationToken, String bindingMessage, Long lastAccessControl,
                            String acrValues) {
        CIBAAuthenticationRequestId authenticationRequestId = new CIBAAuthenticationRequestId(expiresIn);
        this.user = user;
        this.client = client;
        this.scopes = scopeList;
        this.cibaAuthenticationRequestId = authenticationRequestId;
        this.requestStatus = CIBARequestStatus.AUTHORIZATION_PENDING;
        this.authorizationRequestId = authenticationRequestId.getCode();
        this.expiresIn = expiresIn;
        this.clientNotificationToken = clientNotificationToken;
        this.bindingMessage = bindingMessage;
        this.lastAccessControl = lastAccessControl;
        this.tokensDelivered = false;
        this.acrValues = acrValues;
    }

    public String cacheKey() {
        return authorizationRequestId;
    }

    public static String cacheKey(String authorizationRequestId, String grantId) {
        if (StringUtils.isBlank(authorizationRequestId)) {
            return grantId;
        }
        return authorizationRequestId;
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

    public String getAuthorizationRequestId() {
        return authorizationRequestId;
    }

    public void setAuthorizationRequestId(String authorizationRequestId) {
        this.authorizationRequestId = authorizationRequestId;
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

    public CIBARequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(CIBARequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

    public CIBAAuthenticationRequestId getCibaAuthenticationRequestId() {
        return cibaAuthenticationRequestId;
    }

    public void setCibaAuthenticationRequestId(CIBAAuthenticationRequestId cibaAuthenticationRequestId) {
        this.cibaAuthenticationRequestId = cibaAuthenticationRequestId;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    @Override
    public String toString() {
        return "CibaCacheRequest{" +
                "cibaAuthenticationRequestId=" + cibaAuthenticationRequestId +
                ", authorizationRequestId='" + authorizationRequestId + '\'' +
                ", user=" + user +
                ", client=" + client +
                ", scopes=" + scopes +
                ", expiresIn=" + expiresIn +
                ", clientNotificationToken='" + clientNotificationToken + '\'' +
                ", bindingMessage='" + bindingMessage + '\'' +
                ", lastAccessControl=" + lastAccessControl +
                ", userAuthorization=" + requestStatus +
                ", tokensDelivered=" + tokensDelivered +
                ", acrValues='" + acrValues + '\'' +
                '}';
    }
}