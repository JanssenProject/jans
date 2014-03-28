package org.gluu.oxauth.client.session;

import java.io.Serializable;

/**
 * Stores OAuth specific data
 * 
 * @author Yuriy Movchan
 * @version 0.1, 03/20/2013
 */
public class OAuthData implements Serializable {

	private static final long serialVersionUID = 3768651940107346004L;

	private String host;
	private String userUid;
	private String accessToken;
	private int accessTokenExpirationInSeconds;
	private String idToken;
	private String scopes;

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

}
