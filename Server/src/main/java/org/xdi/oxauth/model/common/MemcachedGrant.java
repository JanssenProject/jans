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

    private String authorizationCodeString;  // duplicate due to AbstractToken code change on deserialization
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
        authorizationCodeString = codeGrant.getAuthorizationCode().getCode(); // duplicate due to AbstractToken code change on deserialization
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

    public AuthorizationCodeGrant asCodeGrant(AppConfiguration appConfiguration) {
        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(user, client, authenticationTime, appConfiguration);
        grant.setAuthorizationCode(authorizationCode);
        grant.setScopes(scopes);
        grant.setGrantId(grantId);
        grant.setSessionDn(sessionDn);
        grant.setCodeChallenge(codeChallenge);
        grant.setCodeChallengeMethod(codeChallengeMethod);
        grant.setAcrValues(acrValues);
        grant.setNonce(nonce);

        if (authorizationCode != null) {
            authorizationCode.setCode(authorizationCodeString);
        }
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
