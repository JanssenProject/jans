/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.ldap;

import io.jans.as.model.common.GrantType;
import io.jans.orm.annotation.*;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */

@DataEntry
@ObjectClass(value = "jansToken")
public class TokenEntity implements Serializable {

    @DN
    private String dn;
    @AttributeName(name = "grtId", consistency = true)
    private String grantId;
    @AttributeName(name = "usrId")
    private String userId;
    @AttributeName(name = "jansUsrDN")
    private String userDn;
    @AttributeName(name = "clnId")
    private String clientId;
    @AttributeName(name = "iat")
    private Date creationDate;
    @AttributeName(name = "exp")
    private Date expirationDate;
    @AttributeName(name = "del")
    private boolean deletable = true;
    @AttributeName(name = "authnTime")
    private Date authenticationTime;
    @AttributeName(name = "scp")
    private String scope;
    @AttributeName(name = "tknCde", consistency = true)
    private String tokenCode;
    @AttributeName(name = "tknTyp")
    private String tokenType;
    @AttributeName(name = "grtTyp")
    private String grantType;
    @AttributeName(name = "jwtReq")
    private String jwtRequest;
    @AttributeName(name = "authzCode", consistency = true)
    private String authorizationCode;
    @AttributeName(name = "nnc")
    private String nonce;
    @AttributeName(name = "chlng")
    private String codeChallenge;
    @AttributeName(name = "chlngMth")
    private String codeChallengeMethod;
    @AttributeName(name = "clms")
    private String claims;
    @AttributeName(name = "tknBndCnf")
    private String tokenBindingHash;

    @AttributeName(name = "acr")
    private String authMode;

    @AttributeName(name = "ssnId", consistency = true)
    private String sessionDn;
    @Expiration
    private Integer ttl;

    @AttributeName(name = "attr")
    @JsonObject
    private TokenAttributes attributes;

    @AttributeName(name = "dpop")
    private String dpop;

    public TokenAttributes getAttributes() {
        if (attributes == null) {
            attributes = new TokenAttributes();
        }
        return attributes;
    }

    public final void setAttributes(TokenAttributes attributes) {
        this.attributes = attributes;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getTokenBindingHash() {
        return tokenBindingHash;
    }

    public void setTokenBindingHash(String tokenBindingHash) {
        this.tokenBindingHash = tokenBindingHash;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getGrantId() {
        return grantId;
    }

    public void setGrantId(String grantId) {
        this.grantId = grantId;
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date authenticationTime) {
        this.authenticationTime = authenticationTime;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTokenCode() {
        return tokenCode;
    }

    public void setTokenCode(String tokenCode) {
        this.tokenCode = tokenCode;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public TokenType getTokenTypeEnum() {
        return TokenType.fromValue(tokenType);
    }

    public void setTokenTypeEnum(TokenType tokenType) {
        if (tokenType != null) {
            this.tokenType = tokenType.getValue();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String userDn) {
        this.userDn = userDn;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJwtRequest() {
        return jwtRequest;
    }

    public void setJwtRequest(String jwtRequest) {
        this.jwtRequest = jwtRequest;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
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

    public String getSessionDn() {
        return sessionDn;
    }

    public void setSessionDn(String sessionDn) {
        this.sessionDn = sessionDn;
    }

    public boolean isImplicitFlow() {
        return StringUtils.isBlank(grantType) || grantType.equals(GrantType.IMPLICIT.getValue());
    }

    public String getDpop() {
        return dpop;
    }

    public void setDpop(String dpop) {
        this.dpop = dpop;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenEntity tokenEntity = (TokenEntity) o;

        if (tokenCode != null ? !tokenCode.equals(tokenEntity.tokenCode) : tokenEntity.tokenCode != null) return false;
        return tokenType != null ? tokenType.equals(tokenEntity.tokenType) : tokenEntity.tokenType == null;
    }

    @Override
    public int hashCode() {
        int result = tokenCode != null ? tokenCode.hashCode() : 0;
        result = 31 * result + (tokenType != null ? tokenType.hashCode() : 0);
        return result;
    }
}
