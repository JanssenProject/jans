package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class OpenIdConfiguration implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@NotBlank
	@Size(min = 1)
	private String oxOpenIdConnectVersion;
	
	@NotBlank
	@Size(min = 1)
	private String issuer;
	
	@NotBlank
	@Size(min = 1)
	private String jwksUri;	
	
    @NotEmpty
    @Size(min = 1)
	private List<String> tokenEndpointAuthMethodsSupported;
    
    @NotEmpty
    @Size(min = 1)
	private List<String> tokenEndpointAuthSigningAlgValuesSupported;
    
    @NotBlank
	@Size(min = 1)
	private String serviceDocumentation;
    
    @NotEmpty
    @Size(min = 1)
	private List<String> uiLocalesSupported;
    
    @NotBlank
 	@Size(min = 1)
	private String opPolicyUri;
    
    @NotBlank
 	@Size(min = 1)
	private String opTosUri;
    
    @NotBlank
 	@Size(min = 1)
	private String checkSessionIFrame;	
    
	private String deviceAuthzEndpoint;
    
	private Boolean introspectionAccessTokenMustHaveUmaProtectionScope = false;	
	
    @NotEmpty
    @Size(min = 1)
	private List<String> displayValuesSupported;
    
    @NotEmpty
    @Size(min = 1)
	private List<String> claimTypesSupported;
    
    @NotEmpty
    @Size(min = 1)
	private List<String> claimsLocalesSupported;
    
    @NotEmpty
    @Size(min = 1)
	private List<String> idTokenTokenBindingCnfValuesSupported;
    
	private Boolean claimsParameterSupported;
	private Boolean requestParameterSupported;
	private Boolean requestUriParameterSupported;
	private Boolean requireRequestUriRegistration;
	private Boolean forceIdTokenHintPrecense = false;
	private Boolean forceOfflineAccessScopeToEnableRefreshToken = true;
	private Boolean allowPostLogoutRedirectWithoutValidation = false;
	private Boolean removeRefreshTokensForClientOnLogout  = true;
	
	@Min(value = 1)
	@Max(value = 2147483647)
	private int spontaneousScopeLifetime;
	
	private Boolean endSessionWithAccessToken;
	private List<String> clientWhiteList;
	private List<String> clientBlackList;
	private Boolean legacyIdTokenClaims;
	private Boolean customHeadersWithAuthorizationResponse;
	private Boolean frontChannelLogoutSessionSupported;
	private Boolean useCacheForAllImplicitFlowObjects = false;
	private Boolean invalidateSessionCookiesAfterAuthorizationFlow = false;
	private Boolean openidScopeBackwardCompatibility = false;
	private Boolean skipAuthorizationForOpenIdScopeAndPairwiseId = false;
	private Boolean keepAuthenticatorAttributesOnAcrChange = false;
	private int deviceAuthzRequestExpiresIn;
	private int deviceAuthzTokenPollInterval;
	private String deviceAuthzResponseTypeToProcessAuthz;
	private String cookieDomain;
	
	
	public String getOxOpenIdConnectVersion() {
		return oxOpenIdConnectVersion;
	}
	
	public void setOxOpenIdConnectVersion(String oxOpenIdConnectVersion) {
		this.oxOpenIdConnectVersion = oxOpenIdConnectVersion;
	}
	
	public String getIssuer() {
		return issuer;
	}
	
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
	
	public String getJwksUri() {
		return jwksUri;
	}
	
	public void setJwksUri(String jwksUri) {
		this.jwksUri = jwksUri;
	}
	
	public List<String> getTokenEndpointAuthMethodsSupported() {
		return tokenEndpointAuthMethodsSupported;
	}
	
	public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
		this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
	}
	
	public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
		return tokenEndpointAuthSigningAlgValuesSupported;
	}
	
	public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
		this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
	}
	
	public String getServiceDocumentation() {
		return serviceDocumentation;
	}
	
	public void setServiceDocumentation(String serviceDocumentation) {
		this.serviceDocumentation = serviceDocumentation;
	}
	
	public List<String> getUiLocalesSupported() {
		return uiLocalesSupported;
	}
	
	public void setUiLocalesSupported(List<String> uiLocalesSupported) {
		this.uiLocalesSupported = uiLocalesSupported;
	}
	
	public String getOpPolicyUri() {
		return opPolicyUri;
	}
	
	public void setOpPolicyUri(String opPolicyUri) {
		this.opPolicyUri = opPolicyUri;
	}
	
	public String getOpTosUri() {
		return opTosUri;
	}
	
	public void setOpTosUri(String opTosUri) {
		this.opTosUri = opTosUri;
	}
	
	public String getCheckSessionIFrame() {
		return checkSessionIFrame;
	}
	
	public void setCheckSessionIFrame(String checkSessionIFrame) {
		this.checkSessionIFrame = checkSessionIFrame;
	}
	
	public String getDeviceAuthzEndpoint() {
		return deviceAuthzEndpoint;
	}
	
	public void setDeviceAuthzEndpoint(String deviceAuthzEndpoint) {
		this.deviceAuthzEndpoint = deviceAuthzEndpoint;
	}
	
	public Boolean getIntrospectionAccessTokenMustHaveUmaProtectionScope() {
		return introspectionAccessTokenMustHaveUmaProtectionScope;
	}
	
	public void setIntrospectionAccessTokenMustHaveUmaProtectionScope(
			Boolean introspectionAccessTokenMustHaveUmaProtectionScope) {
		this.introspectionAccessTokenMustHaveUmaProtectionScope = introspectionAccessTokenMustHaveUmaProtectionScope;
	}
	
	public List<String> getDisplayValuesSupported() {
		return displayValuesSupported;
	}
	
	public void setDisplayValuesSupported(List<String> displayValuesSupported) {
		this.displayValuesSupported = displayValuesSupported;
	}
	
	public List<String> getClaimTypesSupported() {
		return claimTypesSupported;
	}
	
	public void setClaimTypesSupported(List<String> claimTypesSupported) {
		this.claimTypesSupported = claimTypesSupported;
	}
	
	public List<String> getClaimsLocalesSupported() {
		return claimsLocalesSupported;
	}
	
	public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
		this.claimsLocalesSupported = claimsLocalesSupported;
	}
	
	public List<String> getIdTokenTokenBindingCnfValuesSupported() {
		return idTokenTokenBindingCnfValuesSupported;
	}
	
	public void setIdTokenTokenBindingCnfValuesSupported(List<String> idTokenTokenBindingCnfValuesSupported) {
		this.idTokenTokenBindingCnfValuesSupported = idTokenTokenBindingCnfValuesSupported;
	}
	
	public Boolean getClaimsParameterSupported() {
		return claimsParameterSupported;
	}
	
	public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
		this.claimsParameterSupported = claimsParameterSupported;
	}
	
	public Boolean getRequestParameterSupported() {
		return requestParameterSupported;
	}
	
	public void setRequestParameterSupported(Boolean requestParameterSupported) {
		this.requestParameterSupported = requestParameterSupported;
	}
	
	public Boolean getRequestUriParameterSupported() {
		return requestUriParameterSupported;
	}
	
	public void setRequestUriParameterSupported(Boolean requestUriParameterSupported) {
		this.requestUriParameterSupported = requestUriParameterSupported;
	}
	
	public Boolean getRequireRequestUriRegistration() {
		return requireRequestUriRegistration;
	}
	
	public void setRequireRequestUriRegistration(Boolean requireRequestUriRegistration) {
		this.requireRequestUriRegistration = requireRequestUriRegistration;
	}
	
	public Boolean getForceIdTokenHintPrecense() {
		return forceIdTokenHintPrecense;
	}
	
	public void setForceIdTokenHintPrecense(Boolean forceIdTokenHintPrecense) {
		this.forceIdTokenHintPrecense = forceIdTokenHintPrecense;
	}
	
	public Boolean getForceOfflineAccessScopeToEnableRefreshToken() {
		return forceOfflineAccessScopeToEnableRefreshToken;
	}
	
	public void setForceOfflineAccessScopeToEnableRefreshToken(Boolean forceOfflineAccessScopeToEnableRefreshToken) {
		this.forceOfflineAccessScopeToEnableRefreshToken = forceOfflineAccessScopeToEnableRefreshToken;
	}
	
	public Boolean getAllowPostLogoutRedirectWithoutValidation() {
		return allowPostLogoutRedirectWithoutValidation;
	}
	
	public void setAllowPostLogoutRedirectWithoutValidation(Boolean allowPostLogoutRedirectWithoutValidation) {
		this.allowPostLogoutRedirectWithoutValidation = allowPostLogoutRedirectWithoutValidation;
	}
	
	public Boolean getRemoveRefreshTokensForClientOnLogout() {
		return removeRefreshTokensForClientOnLogout;
	}
	
	public void setRemoveRefreshTokensForClientOnLogout(Boolean removeRefreshTokensForClientOnLogout) {
		this.removeRefreshTokensForClientOnLogout = removeRefreshTokensForClientOnLogout;
	}
	
	public int getSpontaneousScopeLifetime() {
		return spontaneousScopeLifetime;
	}
	
	public void setSpontaneousScopeLifetime(int spontaneousScopeLifetime) {
		this.spontaneousScopeLifetime = spontaneousScopeLifetime;
	}
	
	public Boolean getEndSessionWithAccessToken() {
		return endSessionWithAccessToken;
	}
	
	public void setEndSessionWithAccessToken(Boolean endSessionWithAccessToken) {
		this.endSessionWithAccessToken = endSessionWithAccessToken;
	}
	
	public List<String> getClientWhiteList() {
		return clientWhiteList;
	}
	
	public void setClientWhiteList(List<String> clientWhiteList) {
		this.clientWhiteList = clientWhiteList;
	}
	
	public List<String> getClientBlackList() {
		return clientBlackList;
	}
	
	public void setClientBlackList(List<String> clientBlackList) {
		this.clientBlackList = clientBlackList;
	}
	
	public Boolean getLegacyIdTokenClaims() {
		return legacyIdTokenClaims;
	}
	
	public void setLegacyIdTokenClaims(Boolean legacyIdTokenClaims) {
		this.legacyIdTokenClaims = legacyIdTokenClaims;
	}
	
	public Boolean getCustomHeadersWithAuthorizationResponse() {
		return customHeadersWithAuthorizationResponse;
	}
	
	public void setCustomHeadersWithAuthorizationResponse(Boolean customHeadersWithAuthorizationResponse) {
		this.customHeadersWithAuthorizationResponse = customHeadersWithAuthorizationResponse;
	}
	
	public Boolean getFrontChannelLogoutSessionSupported() {
		return frontChannelLogoutSessionSupported;
	}
	
	public void setFrontChannelLogoutSessionSupported(Boolean frontChannelLogoutSessionSupported) {
		this.frontChannelLogoutSessionSupported = frontChannelLogoutSessionSupported;
	}
	
	public Boolean getUseCacheForAllImplicitFlowObjects() {
		return useCacheForAllImplicitFlowObjects;
	}
	
	public void setUseCacheForAllImplicitFlowObjects(Boolean useCacheForAllImplicitFlowObjects) {
		this.useCacheForAllImplicitFlowObjects = useCacheForAllImplicitFlowObjects;
	}
	
	public Boolean getInvalidateSessionCookiesAfterAuthorizationFlow() {
		return invalidateSessionCookiesAfterAuthorizationFlow;
	}
	
	public void setInvalidateSessionCookiesAfterAuthorizationFlow(Boolean invalidateSessionCookiesAfterAuthorizationFlow) {
		this.invalidateSessionCookiesAfterAuthorizationFlow = invalidateSessionCookiesAfterAuthorizationFlow;
	}
	
	public Boolean getOpenidScopeBackwardCompatibility() {
		return openidScopeBackwardCompatibility;
	}
	
	public void setOpenidScopeBackwardCompatibility(Boolean openidScopeBackwardCompatibility) {
		this.openidScopeBackwardCompatibility = openidScopeBackwardCompatibility;
	}
	
	public Boolean getSkipAuthorizationForOpenIdScopeAndPairwiseId() {
		return skipAuthorizationForOpenIdScopeAndPairwiseId;
	}
	
	public void setSkipAuthorizationForOpenIdScopeAndPairwiseId(Boolean skipAuthorizationForOpenIdScopeAndPairwiseId) {
		this.skipAuthorizationForOpenIdScopeAndPairwiseId = skipAuthorizationForOpenIdScopeAndPairwiseId;
	}
	
	public Boolean getKeepAuthenticatorAttributesOnAcrChange() {
		return keepAuthenticatorAttributesOnAcrChange;
	}
	
	public void setKeepAuthenticatorAttributesOnAcrChange(Boolean keepAuthenticatorAttributesOnAcrChange) {
		this.keepAuthenticatorAttributesOnAcrChange = keepAuthenticatorAttributesOnAcrChange;
	}
	
	public int getDeviceAuthzRequestExpiresIn() {
		return deviceAuthzRequestExpiresIn;
	}
	
	public void setDeviceAuthzRequestExpiresIn(int deviceAuthzRequestExpiresIn) {
		this.deviceAuthzRequestExpiresIn = deviceAuthzRequestExpiresIn;
	}
	
	public int getDeviceAuthzTokenPollInterval() {
		return deviceAuthzTokenPollInterval;
	}
	
	public void setDeviceAuthzTokenPollInterval(int deviceAuthzTokenPollInterval) {
		this.deviceAuthzTokenPollInterval = deviceAuthzTokenPollInterval;
	}
	
	public String getDeviceAuthzResponseTypeToProcessAuthz() {
		return deviceAuthzResponseTypeToProcessAuthz;
	}
	
	public void setDeviceAuthzResponseTypeToProcessAuthz(String deviceAuthzResponseTypeToProcessAuthz) {
		this.deviceAuthzResponseTypeToProcessAuthz = deviceAuthzResponseTypeToProcessAuthz;
	}
	
	public String getCookieDomain() {
		return cookieDomain;
	}
	
	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}	
	
}
