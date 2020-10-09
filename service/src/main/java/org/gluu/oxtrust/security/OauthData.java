/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.security;

import java.io.Serializable;

import javax.inject.Named;
/**
 * @author Dejan Maric
 * @author Yuriy Movchan
 * @version 0.1, 12/10/2012
 */
@Named
public class OauthData implements Serializable {

	private static final long serialVersionUID = 3768651940107346004L;

	private String host;
	private String userUid;
	private String accessToken;
	private int accessTokenExpirationInSeconds;
	private String idToken;
	private String scopes;
	private String sessionState;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUserUid() {
		return userUid;
	}

	public void setUserUid(String userUid) {
		this.userUid = userUid;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public int getAccessTokenExpirationInSeconds() {
		return accessTokenExpirationInSeconds;
	}

	public void setAccessTokenExpirationInSeconds(int accessTokenExpirationInSeconds) {
		this.accessTokenExpirationInSeconds = accessTokenExpirationInSeconds;
	}

	public String getIdToken() {
		return idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}

	public String getScopes() {
		return scopes;
	}

	public void setScopes(String scopes) {
		this.scopes = scopes;
	}

	public String getSessionState() {
		return sessionState;
	}

	public void setSessionState(String sessionState) {
		this.sessionState = sessionState;
	}

}
