/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.registration.Client;

import javax.enterprise.inject.Instance;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * @author yuriyz
 * @version November 28, 2018
 */
public class CacheGrant implements Serializable {

    private String authorizationCodeString;
    private Date authorizationCodeCreationDate;
    private Date authorizationCodeExpirationDate;

    private User user;
    private Client client;
    private Date authenticationTime;
    private Set<String> scopes;
    private String grantId;
    private String tokenBindingHash;
    private String nonce;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String claims;

    private String acrValues;
    private String sessionDn;
    private int expiresIn = 1;

    public CacheGrant() {
    }

    public CacheGrant(AuthorizationGrant grant, AppConfiguration appConfiguration) {
        if (grant.getAuthorizationCode() != null) {
            authorizationCodeString = grant.getAuthorizationCode().getCode();
            authorizationCodeCreationDate = grant.getAuthorizationCode().getCreationDate();
            authorizationCodeExpirationDate = grant.getAuthorizationCode().getExpirationDate();
        }
        initExpiresIn(grant, appConfiguration);

        user = grant.getUser();
        client = grant.getClient();
        authenticationTime = grant.getAuthenticationTime();
        scopes = grant.getScopes();
        tokenBindingHash = grant.getTokenBindingHash();
        grantId = grant.getGrantId();
        nonce = grant.getNonce();
        acrValues = grant.getAcrValues();
        codeChallenge = grant.getCodeChallenge();
        codeChallengeMethod = grant.getCodeChallengeMethod();
        claims = grant.getClaims();
        sessionDn = grant.getSessionDn();
    }

    private void initExpiresIn(AuthorizationGrant grant, AppConfiguration appConfiguration) {
        if (grant.getAuthorizationCode() != null) {
            expiresIn = grant.getAuthorizationCode().getExpiresIn();
        } else {
            expiresIn = appConfiguration.getAccessTokenLifetime();
            // oxAuth #830 Client-specific access token expiration
            if (client != null && client.getAccessTokenLifetime() != null && client.getAccessTokenLifetime() > 0) {
                expiresIn = client.getAccessTokenLifetime();
            }
        }
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public Date getAuthorizationCodeCreationDate() {
        return authorizationCodeCreationDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getGrantId() {
        return grantId;
    }

    public void setGrantId(String grantId) {
        this.grantId = grantId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public String getAuthorizationCodeString() {
        return authorizationCodeString;
    }

    public void setAuthorizationCodeString(String authorizationCodeString) {
        this.authorizationCodeString = authorizationCodeString;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getClaims() {
        return claims;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    public AuthorizationCodeGrant asCodeGrant(Instance<AbstractAuthorizationGrant> grantInstance) {
        AuthorizationCodeGrant grant = grantInstance.select(AuthorizationCodeGrant.class).get();
        grant.init(user, client, authenticationTime);

        grant.setAuthorizationCode(new AuthorizationCode(authorizationCodeString, authorizationCodeCreationDate, authorizationCodeExpirationDate));
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setCodeChallenge(codeChallenge);
        grant.setCodeChallengeMethod(codeChallengeMethod);
        grant.setAcrValues(acrValues);
        grant.setNonce(nonce);
        grant.setClaims(claims);

        return grant;
    }

    public String cacheKey() {
        return cacheKey(client.getClientId(), authorizationCodeString, grantId);
    }

    public static String cacheKey(String clientId, String code, String grantId) {
        if (StringUtils.isBlank(code)) {
            return grantId;
        }
        return clientId + "_" + code;
    }

    @Override
    public String toString() {
        return "MemcachedGrant{" +
                "authorizationCode=" + authorizationCodeString +
                ", user=" + user +
                ", client=" + client +
                ", authenticationTime=" + authenticationTime +
                '}';
    }
}
