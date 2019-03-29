/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.uma.wrapper;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class Token implements Serializable {

    private String authorizationCode;
    private String scope;
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private Integer expiresIn;

    public Token() {
    }

    public Token(String authorizationCode, String refreshToken, String accessToken, String scope, Integer expiresIn) {
        this.authorizationCode = authorizationCode;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.scope = scope;
        this.expiresIn = expiresIn;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public Token setAuthorizationCode(String p_authorizationCode) {
        authorizationCode = p_authorizationCode;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Token setRefreshToken(String p_refreshToken) {
        refreshToken = p_refreshToken;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Token setAccessToken(String p_accessToken) {
        accessToken = p_accessToken;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Token setScope(String p_scope) {
        scope = p_scope;
        return this;
    }

    public String getIdToken() {
        return idToken;
    }

    public Token setIdToken(String p_idToken) {
        idToken = p_idToken;
        return this;
    }

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

}
