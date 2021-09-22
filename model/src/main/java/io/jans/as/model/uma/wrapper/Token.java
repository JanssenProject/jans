/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma.wrapper;

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

    public Token setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Token setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Token setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Token setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getIdToken() {
        return idToken;
    }

    public Token setIdToken(String idToken) {
        this.idToken = idToken;
        return this;
    }

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

}
