package org.xdi.oxauth.model.common;

import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.registration.Client;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * @author yuriyz on 02/14/2017.
 */
public class MemcachedGrant implements Serializable {

    private AuthorizationCode authorizationCode;
    private User user;
    private Client client;
    private Date authenticationTime;
    private Set<String> scopes;
    private String grantId;
    private String nonce;
    private String codeChallenge;
    private String codeChallengeMethod;

    private String acrValues;
    private String sessionDn;

    public MemcachedGrant() {
    }

    public MemcachedGrant(AuthorizationGrant codeGrant) {
        authorizationCode = codeGrant.getAuthorizationCode();
        user = codeGrant.getUser();
        client = codeGrant.getClient();
        authenticationTime = codeGrant.getAuthenticationTime();
        scopes = codeGrant.getScopes();
        grantId = codeGrant.getGrantId();
        nonce = codeGrant.getNonce();
        acrValues = codeGrant.getAcrValues();
        codeChallenge = codeGrant.getCodeChallenge();
        codeChallengeMethod = codeGrant.getCodeChallengeMethod();
        sessionDn = codeGrant.getSessionDn();
    }

    public AuthorizationCode getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(AuthorizationCode authorizationCode) {
        this.authorizationCode = authorizationCode;
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

    public AuthorizationCodeGrant asCodeGrant(AppConfiguration appConfiguration) {
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(user, client, authenticationTime, appConfiguration);
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setCodeChallenge(codeChallenge);
        grant.setCodeChallengeMethod(codeChallengeMethod);
        grant.setAcrValues(acrValues);
        grant.setNonce(nonce);
        return grant;
    }

    public String cacheKey() {
        return cacheKey(client.getClientId(), getAuthorizationCode().getCode());
    }

    public static String cacheKey(String clientId, String code) {
        return clientId + "_" + code;
    }

    @Override
    public String toString() {
        return "MemcachedGrant{" +
                "authorizationCode=" + authorizationCode +
                ", user=" + user +
                ", client=" + client +
                ", authenticationTime=" + authenticationTime +
                '}';
    }
}
