package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

public class TokenConfiguration implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Boolean persistRefreshTokenInLdap = true;
	
	@Min(value = 1)
	@Max(value = 2147483647)
	private int authorizationCodeLifetime;
	
	@Min(value = 1)
	@Max(value = 2147483647)
	private int refreshTokenLifetime;
	
	@Min(value = 1)
	@Max(value = 2147483647)
	private int accessTokenLifetime;

	public Boolean getPersistRefreshTokenInLdap() {
		return persistRefreshTokenInLdap;
	}

	public void setPersistRefreshTokenInLdap(Boolean persistRefreshTokenInLdap) {
		this.persistRefreshTokenInLdap = persistRefreshTokenInLdap;
	}

	public int getAuthorizationCodeLifetime() {
		return authorizationCodeLifetime;
	}

	public void setAuthorizationCodeLifetime(int authorizationCodeLifetime) {
		this.authorizationCodeLifetime = authorizationCodeLifetime;
	}

	public int getRefreshTokenLifetime() {
		return refreshTokenLifetime;
	}

	public void setRefreshTokenLifetime(int refreshTokenLifetime) {
		this.refreshTokenLifetime = refreshTokenLifetime;
	}

	public int getAccessTokenLifetime() {
		return accessTokenLifetime;
	}

	public void setAccessTokenLifetime(int accessTokenLifetime) {
		this.accessTokenLifetime = accessTokenLifetime;
	}
	
}
