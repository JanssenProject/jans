package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class Endpoint implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@NotBlank
	@Size(min = 1)
	private String baseEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String authorizationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String tokenEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String tokenRevocationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String userInfoEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String clientInfoEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String endSessionEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String registrationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String openIdDiscoveryEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String openIdConfigurationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String idGenerationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String introspectionEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String umaConfigurationEndpoint;
	
	@NotBlank
	@Size(min = 1)
	private String oxElevenGenerateKeyEndpoint;
	
	private String backchannelAuthenticationEndpoint;
	private String backchannelDeviceRegistrationEndpoint;
	
	public String getBaseEndpoint() {
		return baseEndpoint;
	}
	
	public void setBaseEndpoint(String baseEndpoint) {
		this.baseEndpoint = baseEndpoint;
	}
	
	public String getAuthorizationEndpoint() {
		return authorizationEndpoint;
	}
	
	public void setAuthorizationEndpoint(String authorizationEndpoint) {
		this.authorizationEndpoint = authorizationEndpoint;
	}
	
	public String getTokenEndpoint() {
		return tokenEndpoint;
	}
	
	public void setTokenEndpoint(String tokenEndpoint) {
		this.tokenEndpoint = tokenEndpoint;
	}
	
	public String getTokenRevocationEndpoint() {
		return tokenRevocationEndpoint;
	}
	
	public void setTokenRevocationEndpoint(String tokenRevocationEndpoint) {
		this.tokenRevocationEndpoint = tokenRevocationEndpoint;
	}
	
	public String getUserInfoEndpoint() {
		return userInfoEndpoint;
	}
	
	public void setUserInfoEndpoint(String userInfoEndpoint) {
		this.userInfoEndpoint = userInfoEndpoint;
	}
	
	public String getClientInfoEndpoint() {
		return clientInfoEndpoint;
	}
	
	public void setClientInfoEndpoint(String clientInfoEndpoint) {
		this.clientInfoEndpoint = clientInfoEndpoint;
	}
	
	public String getEndSessionEndpoint() {
		return endSessionEndpoint;
	}
	
	public void setEndSessionEndpoint(String endSessionEndpoint) {
		this.endSessionEndpoint = endSessionEndpoint;
	}
	
	public String getRegistrationEndpoint() {
		return registrationEndpoint;
	}
	
	public void setRegistrationEndpoint(String registrationEndpoint) {
		this.registrationEndpoint = registrationEndpoint;
	}
	
	public String getOpenIdDiscoveryEndpoint() {
		return openIdDiscoveryEndpoint;
	}
	
	public void setOpenIdDiscoveryEndpoint(String openIdDiscoveryEndpoint) {
		this.openIdDiscoveryEndpoint = openIdDiscoveryEndpoint;
	}
	
	public String getOpenIdConfigurationEndpoint() {
		return openIdConfigurationEndpoint;
	}
	
	public void setOpenIdConfigurationEndpoint(String openIdConfigurationEndpoint) {
		this.openIdConfigurationEndpoint = openIdConfigurationEndpoint;
	}
	
	public String getIdGenerationEndpoint() {
		return idGenerationEndpoint;
	}
	
	public void setIdGenerationEndpoint(String idGenerationEndpoint) {
		this.idGenerationEndpoint = idGenerationEndpoint;
	}
	
	public String getIntrospectionEndpoint() {
		return introspectionEndpoint;
	}
	
	public void setIntrospectionEndpoint(String introspectionEndpoint) {
		this.introspectionEndpoint = introspectionEndpoint;
	}
	
	public String getUmaConfigurationEndpoint() {
		return umaConfigurationEndpoint;
	}
	
	public void setUmaConfigurationEndpoint(String umaConfigurationEndpoint) {
		this.umaConfigurationEndpoint = umaConfigurationEndpoint;
	}
	
	public String getOxElevenGenerateKeyEndpoint() {
		return oxElevenGenerateKeyEndpoint;
	}
	
	public void setOxElevenGenerateKeyEndpoint(String oxElevenGenerateKeyEndpoint) {
		this.oxElevenGenerateKeyEndpoint = oxElevenGenerateKeyEndpoint;
	}
	
	public String getBackchannelAuthenticationEndpoint() {
		return backchannelAuthenticationEndpoint;
	}
	
	public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
		this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
	}
	
	public String getBackchannelDeviceRegistrationEndpoint() {
		return backchannelDeviceRegistrationEndpoint;
	}
	
	public void setBackchannelDeviceRegistrationEndpoint(String backchannelDeviceRegistrationEndpoint) {
		this.backchannelDeviceRegistrationEndpoint = backchannelDeviceRegistrationEndpoint;
	}
}
