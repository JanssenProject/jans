/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma.wrapper;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/03/2013
 */

public class Token implements Serializable {

    private String m_authorizationCode;
    private String m_scope;
    private String m_accessToken;
    private String m_refreshToken;
    private String m_idToken;
    private Integer expiresIn;

    public Token() {
    }

    public Token(String p_authorizationCode, String p_refreshToken, String p_accessToken, String p_scope, Integer expiresIn) {
        m_authorizationCode = p_authorizationCode;
        m_refreshToken = p_refreshToken;
        m_accessToken = p_accessToken;
        m_scope = p_scope;
        this.expiresIn = expiresIn;
    }

    public String getAuthorizationCode() {
        return m_authorizationCode;
    }

    public Token setAuthorizationCode(String p_authorizationCode) {
        m_authorizationCode = p_authorizationCode;
        return this;
    }

    public String getRefreshToken() {
        return m_refreshToken;
    }

    public Token setRefreshToken(String p_refreshToken) {
        m_refreshToken = p_refreshToken;
        return this;
    }

    public String getAccessToken() {
        return m_accessToken;
    }

    public Token setAccessToken(String p_accessToken) {
        m_accessToken = p_accessToken;
        return this;
    }

    public String getScope() {
        return m_scope;
    }

    public Token setScope(String p_scope) {
        m_scope = p_scope;
        return this;
    }

    public String getIdToken() {
        return m_idToken;
    }

    public Token setIdToken(String p_idToken) {
        m_idToken = p_idToken;
        return this;
    }

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

}
