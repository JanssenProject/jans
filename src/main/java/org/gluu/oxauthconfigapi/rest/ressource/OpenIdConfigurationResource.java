package org.gluu.oxauthconfigapi.rest.ressource;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauthconfigapi.filters.ProtectedApi;
import org.gluu.oxauthconfigapi.rest.model.OpenIdConfiguration;
import org.gluu.oxauthconfigapi.util.ApiConstants;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.slf4j.Logger;

import com.couchbase.client.core.message.ResponseStatus;

/**
 * @author Puja Sharma
 *
 */

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIG + ApiConstants.PROPERTIES + ApiConstants.OPENID)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OpenIdConfigurationResource extends BaseResource {

	@Inject
	Logger log;

	@Inject
	JsonConfigurationService jsonConfigurationService;

	@GET
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getOpenIdConfigurationResource() throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		OpenIdConfiguration openIdConfiguration = new OpenIdConfiguration();
		openIdConfiguration.setOxOpenIdConnectVersion(appConfiguration.getOxOpenIdConnectVersion());
		openIdConfiguration.setIssuer(appConfiguration.getIssuer());
		openIdConfiguration.setJwksUri(appConfiguration.getJwksUri());
		openIdConfiguration
				.setTokenEndpointAuthMethodsSupported(appConfiguration.getTokenEndpointAuthMethodsSupported());
		openIdConfiguration.setTokenEndpointAuthSigningAlgValuesSupported(
				appConfiguration.getTokenEndpointAuthSigningAlgValuesSupported());
		openIdConfiguration.setServiceDocumentation(appConfiguration.getServiceDocumentation());
		openIdConfiguration.setUiLocalesSupported(appConfiguration.getUiLocalesSupported());
		openIdConfiguration.setOpPolicyUri(appConfiguration.getOpPolicyUri());
		openIdConfiguration.setOpTosUri(appConfiguration.getOpTosUri());
		openIdConfiguration.setCheckSessionIFrame(appConfiguration.getCheckSessionIFrame());
		openIdConfiguration.setDeviceAuthzEndpoint(appConfiguration.getDeviceAuthzEndpoint());
		openIdConfiguration.setIntrospectionAccessTokenMustHaveUmaProtectionScope(
				appConfiguration.getIntrospectionAccessTokenMustHaveUmaProtectionScope());
		openIdConfiguration.setDisplayValuesSupported(appConfiguration.getDisplayValuesSupported());
		openIdConfiguration.setClaimTypesSupported(appConfiguration.getClaimTypesSupported());
		openIdConfiguration.setClaimsLocalesSupported(appConfiguration.getClaimsLocalesSupported());
		openIdConfiguration
				.setIdTokenTokenBindingCnfValuesSupported(appConfiguration.getIdTokenTokenBindingCnfValuesSupported());
		openIdConfiguration.setClaimsParameterSupported(appConfiguration.getClaimsParameterSupported());
		openIdConfiguration.setRequestParameterSupported(appConfiguration.getRequestParameterSupported());
		openIdConfiguration.setRequestUriParameterSupported(appConfiguration.getRequestUriParameterSupported());
		openIdConfiguration.setRequireRequestUriRegistration(appConfiguration.getRequireRequestUriRegistration());
		openIdConfiguration.setForceIdTokenHintPrecense(appConfiguration.getForceIdTokenHintPrecense());
		openIdConfiguration.setForceOfflineAccessScopeToEnableRefreshToken(
				appConfiguration.getForceOfflineAccessScopeToEnableRefreshToken());
		openIdConfiguration.setAllowPostLogoutRedirectWithoutValidation(
				appConfiguration.getAllowPostLogoutRedirectWithoutValidation());
		openIdConfiguration
				.setRemoveRefreshTokensForClientOnLogout(appConfiguration.getRemoveRefreshTokensForClientOnLogout());
		openIdConfiguration.setSpontaneousScopeLifetime(appConfiguration.getSpontaneousScopeLifetime());
		openIdConfiguration.setEndSessionWithAccessToken(appConfiguration.getEndSessionWithAccessToken());
		openIdConfiguration.setClientWhiteList(appConfiguration.getClientWhiteList());
		openIdConfiguration.setClientBlackList(appConfiguration.getClientBlackList());
		openIdConfiguration.setLegacyIdTokenClaims(appConfiguration.getLegacyIdTokenClaims());
		openIdConfiguration.setCustomHeadersWithAuthorizationResponse(
				appConfiguration.getCustomHeadersWithAuthorizationResponse());
		openIdConfiguration
				.setFrontChannelLogoutSessionSupported(appConfiguration.getFrontChannelLogoutSessionSupported());
		//openIdConfiguration
			//	.setUseCacheForAllImplicitFlowObjects(appConfiguration.getUseCacheForAllImplicitFlowObjects());
		openIdConfiguration.setInvalidateSessionCookiesAfterAuthorizationFlow(
				appConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow());
		openIdConfiguration.setOpenidScopeBackwardCompatibility(appConfiguration.getOpenidScopeBackwardCompatibility());
		openIdConfiguration.setSkipAuthorizationForOpenIdScopeAndPairwiseId(
				appConfiguration.getSkipAuthorizationForOpenIdScopeAndPairwiseId());
		openIdConfiguration.setKeepAuthenticatorAttributesOnAcrChange(
				appConfiguration.getKeepAuthenticatorAttributesOnAcrChange());
		openIdConfiguration.setDeviceAuthzRequestExpiresIn(appConfiguration.getDeviceAuthzRequestExpiresIn());
		openIdConfiguration.setDeviceAuthzTokenPollInterval(appConfiguration.getDeviceAuthzTokenPollInterval());
		openIdConfiguration
				.setDeviceAuthzResponseTypeToProcessAuthz(appConfiguration.getDeviceAuthzResponseTypeToProcessAuthz());
		openIdConfiguration.setCookieDomain(appConfiguration.getCookieDomain());
		openIdConfiguration.setOpenidSubAttribute(appConfiguration.getOpenidSubAttribute());
		return Response.ok(openIdConfiguration).build();
	}

	@PUT
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateOpenIdConfigurationResource(@Valid OpenIdConfiguration openIdConfiguration)
			throws IOException {
		AppConfiguration appConfiguration = this.jsonConfigurationService.getOxauthAppConfiguration();
		appConfiguration.setOxOpenIdConnectVersion(openIdConfiguration.getOxOpenIdConnectVersion());
		appConfiguration.setIssuer(openIdConfiguration.getIssuer());
		appConfiguration.setJwksUri(openIdConfiguration.getJwksUri());
		appConfiguration
				.setTokenEndpointAuthMethodsSupported(openIdConfiguration.getTokenEndpointAuthMethodsSupported());
		appConfiguration.setTokenEndpointAuthSigningAlgValuesSupported(
				openIdConfiguration.getTokenEndpointAuthSigningAlgValuesSupported());
		appConfiguration.setServiceDocumentation(openIdConfiguration.getServiceDocumentation());
		appConfiguration.setUiLocalesSupported(openIdConfiguration.getUiLocalesSupported());
		appConfiguration.setOpPolicyUri(openIdConfiguration.getOpPolicyUri());
		appConfiguration.setOpTosUri(openIdConfiguration.getOpTosUri());
		appConfiguration.setCheckSessionIFrame(openIdConfiguration.getCheckSessionIFrame());
		appConfiguration.setDeviceAuthzEndpoint(openIdConfiguration.getDeviceAuthzEndpoint());
		appConfiguration.setIntrospectionAccessTokenMustHaveUmaProtectionScope(
				openIdConfiguration.getIntrospectionAccessTokenMustHaveUmaProtectionScope());
		appConfiguration.setDisplayValuesSupported(openIdConfiguration.getDisplayValuesSupported());
		appConfiguration.setClaimTypesSupported(openIdConfiguration.getClaimTypesSupported());
		appConfiguration.setClaimsLocalesSupported(openIdConfiguration.getClaimsLocalesSupported());
		appConfiguration.setIdTokenTokenBindingCnfValuesSupported(
				openIdConfiguration.getIdTokenTokenBindingCnfValuesSupported());
		appConfiguration.setClaimsParameterSupported(openIdConfiguration.getClaimsParameterSupported());
		appConfiguration.setRequestParameterSupported(openIdConfiguration.getRequestParameterSupported());
		appConfiguration.setRequestUriParameterSupported(openIdConfiguration.getRequestUriParameterSupported());
		appConfiguration.setRequireRequestUriRegistration(openIdConfiguration.getRequireRequestUriRegistration());
		appConfiguration.setForceIdTokenHintPrecense(openIdConfiguration.getForceIdTokenHintPrecense());
		appConfiguration.setForceOfflineAccessScopeToEnableRefreshToken(
				openIdConfiguration.getForceOfflineAccessScopeToEnableRefreshToken());
		appConfiguration.setAllowPostLogoutRedirectWithoutValidation(
				openIdConfiguration.getAllowPostLogoutRedirectWithoutValidation());
		appConfiguration
				.setRemoveRefreshTokensForClientOnLogout(openIdConfiguration.getRemoveRefreshTokensForClientOnLogout());
		appConfiguration.setSpontaneousScopeLifetime(openIdConfiguration.getSpontaneousScopeLifetime());
		appConfiguration.setEndSessionWithAccessToken(openIdConfiguration.getEndSessionWithAccessToken());
		appConfiguration.setClientWhiteList(openIdConfiguration.getClientWhiteList());
		appConfiguration.setClientBlackList(openIdConfiguration.getClientBlackList());
		appConfiguration.setLegacyIdTokenClaims(openIdConfiguration.getLegacyIdTokenClaims());
		appConfiguration.setCustomHeadersWithAuthorizationResponse(
				openIdConfiguration.getCustomHeadersWithAuthorizationResponse());
		appConfiguration
				.setFrontChannelLogoutSessionSupported(openIdConfiguration.getFrontChannelLogoutSessionSupported());
	//	appConfiguration
		//		.setUseCacheForAllImplicitFlowObjects(openIdConfiguration.getUseCacheForAllImplicitFlowObjects());
		appConfiguration.setInvalidateSessionCookiesAfterAuthorizationFlow(
				openIdConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow());
		appConfiguration.setOpenidScopeBackwardCompatibility(openIdConfiguration.getOpenidScopeBackwardCompatibility());
		appConfiguration.setSkipAuthorizationForOpenIdScopeAndPairwiseId(
				openIdConfiguration.getSkipAuthorizationForOpenIdScopeAndPairwiseId());
		appConfiguration.setKeepAuthenticatorAttributesOnAcrChange(
				openIdConfiguration.getKeepAuthenticatorAttributesOnAcrChange());
		appConfiguration.setDeviceAuthzRequestExpiresIn(openIdConfiguration.getDeviceAuthzRequestExpiresIn());
		appConfiguration.setDeviceAuthzTokenPollInterval(openIdConfiguration.getDeviceAuthzTokenPollInterval());
		appConfiguration.setDeviceAuthzResponseTypeToProcessAuthz(
				openIdConfiguration.getDeviceAuthzResponseTypeToProcessAuthz());
		appConfiguration.setCookieDomain(openIdConfiguration.getCookieDomain());
		appConfiguration.setOpenidSubAttribute(openIdConfiguration.getOpenidSubAttribute());
		this.jsonConfigurationService.saveOxAuthAppConfiguration(appConfiguration);
		return Response.ok(ResponseStatus.SUCCESS).build();
	}

}