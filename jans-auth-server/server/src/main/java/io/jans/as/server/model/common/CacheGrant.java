/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import org.apache.commons.lang.StringUtils;

import jakarta.enterprise.inject.Instance;
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
    private String deviceCode;

    private String acrValues;
    private String sessionDn;
    private int expiresIn = 1;

    // CIBA
    private String authReqId;
    private boolean tokensDelivered;


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

    public CacheGrant(CIBAGrant grant, AppConfiguration appConfiguration) {
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

        authReqId = grant.getAuthReqId();
        tokensDelivered = grant.isTokensDelivered();
    }

    public CacheGrant(DeviceCodeGrant grant, AppConfiguration appConfiguration) {
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
        deviceCode = grant.getDeviceCode();
    }

    private void initExpiresIn(AuthorizationGrant grant, AppConfiguration appConfiguration) {
        if (grant.getAuthorizationCode() != null) {
            expiresIn = grant.getAuthorizationCode().getExpiresIn();
        } else {
            expiresIn = appConfiguration.getAccessTokenLifetime();
            // Jans Auth #830 Client-specific access token expiration
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

    public CIBAGrant asCibaGrant(Instance<AbstractAuthorizationGrant> grantInstance) {
        CIBAGrant grant = grantInstance.select(CIBAGrant.class).get();
        grant.init(user, AuthorizationGrantType.CIBA, client, authenticationTime);
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setCodeChallenge(codeChallenge);
        grant.setCodeChallengeMethod(codeChallengeMethod);
        grant.setAcrValues(acrValues);
        grant.setNonce(nonce);
        grant.setClaims(claims);
        grant.setAuthReqId(authReqId);
        grant.setTokensDelivered(tokensDelivered);

        return grant;
    }

    public DeviceCodeGrant asDeviceCodeGrant(Instance<AbstractAuthorizationGrant> grantInstance) {
        DeviceCodeGrant grant = grantInstance.select(DeviceCodeGrant.class).get();
        grant.init(user, AuthorizationGrantType.DEVICE_CODE, client, authenticationTime);
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setCodeChallenge(codeChallenge);
        grant.setCodeChallengeMethod(codeChallengeMethod);
        grant.setAcrValues(acrValues);
        grant.setNonce(nonce);
        grant.setClaims(claims);
        grant.setDeviceCode(deviceCode);

        return grant;
    }

    public String cacheKey() {
        return cacheKey(authorizationCodeString, grantId);
    }

    public static String cacheKey(String code, String grantId) {
        if (StringUtils.isBlank(code)) {
            return grantId;
        }
        return code;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public boolean isTokensDelivered() {
        return tokensDelivered;
    }

    public void setTokensDelivered(boolean tokensDelivered) {
        this.tokensDelivered = tokensDelivered;
    }

    public String getDeviceCode() {
        return deviceCode;
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
