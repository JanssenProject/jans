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
public class TokenLdap implements Serializable {

    @DN
    private String dn;
    @AttributeName(name = "grtId", consistency = true)
    private String grantId;
    @AttributeName(name = "usrId")
    private String userId;
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

    public TokenLdap() {
    }

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

    public void setAuthorizationCode(String p_authorizationCode) {
        authorizationCode = p_authorizationCode;
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

    public void setGrantId(String p_grantId) {
        grantId = p_grantId;
    }

    public Date getAuthenticationTime() {
        return authenticationTime;
    }

    public void setAuthenticationTime(Date p_authenticationTime) {
        authenticationTime = p_authenticationTime;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date p_creationDate) {
        creationDate = p_creationDate;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String p_dn) {
        dn = p_dn;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date p_expirationDate) {
        expirationDate = p_expirationDate;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String p_grantType) {
        grantType = p_grantType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String p_scope) {
        scope = p_scope;
    }

    public String getTokenCode() {
        return tokenCode;
    }

    public void setTokenCode(String p_tokenCode) {
        tokenCode = p_tokenCode;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String p_tokenType) {
        tokenType = p_tokenType;
    }

    public TokenType getTokenTypeEnum() {
        return TokenType.fromValue(tokenType);
    }

    public void setTokenTypeEnum(TokenType p_tokenType) {
        if (p_tokenType != null) {
            tokenType = p_tokenType.getValue();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String p_userId) {
        userId = p_userId;
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

    public void setJwtRequest(String p_jwtRequest) {
        jwtRequest = p_jwtRequest;
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

        TokenLdap tokenLdap = (TokenLdap) o;

        if (tokenCode != null ? !tokenCode.equals(tokenLdap.tokenCode) : tokenLdap.tokenCode != null) return false;
        return tokenType != null ? tokenType.equals(tokenLdap.tokenType) : tokenLdap.tokenType == null;
    }

    @Override
    public int hashCode() {
        int result = tokenCode != null ? tokenCode.hashCode() : 0;
        result = 31 * result + (tokenType != null ? tokenType.hashCode() : 0);
        return result;
    }
}
