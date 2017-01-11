/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.servlet;

import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ACR_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.AUTHORIZATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.AUTH_LEVEL_MAPPING;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CHECK_SESSION_IFRAME;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_LOCALES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_PARAMETER_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIM_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLIENT_INFO_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.DISPLAY_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.END_SESSION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.GRANT_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_GENERATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.INTROSPECTION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ISSUER;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.JWKS_URI;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.OP_POLICY_URI;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.OP_TOS_URI;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REGISTRATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUEST_PARAMETER_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUEST_URI_PARAMETER_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.REQUIRE_REQUEST_URI_REGISTRATION;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.RESPONSE_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.SCOPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.SCOPE_TO_CLAIMS_MAPPING;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.SERVICE_DOCUMENTATION;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.SUBJECT_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.UI_LOCALES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.USER_INFO_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.USER_INFO_SIGNING_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.VALIDATE_TOKEN_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.xdi.ldap.model.GluuStatus;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.common.ScopeType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.uma.UmaScopeType;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 2016/04/26
 * @version 0.9 March 27, 2015
 */
public class OpenIdConfiguration extends HttpServlet {

	private final static Log LOG = Logging.getLog(OpenIdConfiguration.class);

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

		new ContextualHttpServletRequest(httpRequest) {
			@Override
			public void process() throws IOException {
				httpResponse.setContentType("application/json");
				PrintWriter out = httpResponse.getWriter();
				try {
					JSONObject jsonObj = new JSONObject();

					AppConfiguration appConfiguration = ServerUtil.instance("appConfiguration");

					jsonObj.put(ISSUER, appConfiguration.getIssuer());
					jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
					jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
					jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
					jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());
					jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());
					jsonObj.put(END_SESSION_ENDPOINT, appConfiguration.getEndSessionEndpoint());
					jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
					jsonObj.put(REGISTRATION_ENDPOINT, appConfiguration.getRegistrationEndpoint());
					jsonObj.put(VALIDATE_TOKEN_ENDPOINT, appConfiguration.getValidateTokenEndpoint());
					jsonObj.put(ID_GENERATION_ENDPOINT, appConfiguration.getIdGenerationEndpoint());
					jsonObj.put(INTROSPECTION_ENDPOINT, appConfiguration.getIntrospectionEndpoint());

					ScopeService scopeService = ScopeService.instance();
					JSONArray scopesSupported = new JSONArray();
					for (Scope scope : scopeService.getAllScopesList()) {
						boolean isUmaAuthorization = UmaScopeType.AUTHORIZATION.getValue()
								.equals(scope.getDisplayName());
						boolean isUmaProtection = UmaScopeType.PROTECTION.getValue().equals(scope.getDisplayName());
						if (!isUmaAuthorization && !isUmaProtection)
							scopesSupported.put(scope.getDisplayName());
					}
					if (scopesSupported.length() > 0) {
						jsonObj.put(SCOPES_SUPPORTED, scopesSupported);
					}

					JSONArray responseTypesSupported = new JSONArray();
					for (String responseType : appConfiguration.getResponseTypesSupported()) {
						responseTypesSupported.put(responseType);
					}
					if (responseTypesSupported.length() > 0) {
						jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
					}

					JSONArray grantTypesSupported = new JSONArray();
					for (String grantType : appConfiguration.getGrantTypesSupported()) {
						grantTypesSupported.put(grantType);
					}
					if (grantTypesSupported.length() > 0) {
						jsonObj.put(GRANT_TYPES_SUPPORTED, grantTypesSupported);
					}

					ExternalAuthenticationService externalAuthenticationService = ExternalAuthenticationService
							.instance();
					JSONArray acrValuesSupported = new JSONArray();
					for (String acr : externalAuthenticationService.getAcrValuesList()) {
						acrValuesSupported.put(acr);
					}
					jsonObj.put(ACR_VALUES_SUPPORTED, acrValuesSupported);
					jsonObj.put(AUTH_LEVEL_MAPPING, createAuthLevelMapping());

					JSONArray subjectTypesSupported = new JSONArray();
					for (String subjectType : appConfiguration.getSubjectTypesSupported()) {
						subjectTypesSupported.put(subjectType);
					}
					if (subjectTypesSupported.length() > 0) {
						jsonObj.put(SUBJECT_TYPES_SUPPORTED, subjectTypesSupported);
					}

					JSONArray userInfoSigningAlgValuesSupported = new JSONArray();
					for (String userInfoSigningAlg : appConfiguration.getUserInfoSigningAlgValuesSupported()) {
						userInfoSigningAlgValuesSupported.put(userInfoSigningAlg);
					}
					if (userInfoSigningAlgValuesSupported.length() > 0) {
						jsonObj.put(USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, userInfoSigningAlgValuesSupported);
					}

					JSONArray userInfoEncryptionAlgValuesSupported = new JSONArray();
					for (String userInfoEncryptionAlg : appConfiguration.getUserInfoEncryptionAlgValuesSupported()) {
						userInfoEncryptionAlgValuesSupported.put(userInfoEncryptionAlg);
					}
					if (userInfoEncryptionAlgValuesSupported.length() > 0) {
						jsonObj.put(USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
					}

					JSONArray userInfoEncryptionEncValuesSupported = new JSONArray();
					for (String userInfoEncryptionEnc : appConfiguration.getUserInfoEncryptionEncValuesSupported()) {
						userInfoEncryptionEncValuesSupported.put(userInfoEncryptionEnc);
					}
					if (userInfoEncryptionAlgValuesSupported.length() > 0) {
						jsonObj.put(USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
					}

					JSONArray idTokenSigningAlgValuesSupported = new JSONArray();
					for (String idTokenSigningAlg : appConfiguration.getIdTokenSigningAlgValuesSupported()) {
						idTokenSigningAlgValuesSupported.put(idTokenSigningAlg);
					}
					if (idTokenSigningAlgValuesSupported.length() > 0) {
						jsonObj.put(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, idTokenSigningAlgValuesSupported);
					}

					JSONArray idTokenEncryptionAlgValuesSupported = new JSONArray();
					for (String idTokenEncryptionAlg : appConfiguration.getIdTokenEncryptionAlgValuesSupported()) {
						idTokenEncryptionAlgValuesSupported.put(idTokenEncryptionAlg);
					}
					if (idTokenEncryptionAlgValuesSupported.length() > 0) {
						jsonObj.put(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED, idTokenEncryptionAlgValuesSupported);
					}

					JSONArray idTokenEncryptionEncValuesSupported = new JSONArray();
					for (String idTokenEncryptionEnc : appConfiguration.getIdTokenEncryptionEncValuesSupported()) {
						idTokenEncryptionEncValuesSupported.put(idTokenEncryptionEnc);
					}
					if (idTokenEncryptionEncValuesSupported.length() > 0) {
						jsonObj.put(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED, idTokenEncryptionEncValuesSupported);
					}

					JSONArray requestObjectSigningAlgValuesSupported = new JSONArray();
					for (String requestObjectSigningAlg : appConfiguration.getRequestObjectSigningAlgValuesSupported()) {
						requestObjectSigningAlgValuesSupported.put(requestObjectSigningAlg);
					}
					if (requestObjectSigningAlgValuesSupported.length() > 0) {
						jsonObj.put(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED,
								requestObjectSigningAlgValuesSupported);
					}

					JSONArray requestObjectEncryptionAlgValuesSupported = new JSONArray();
					for (String requestObjectEncryptionAlg : appConfiguration
							.getRequestObjectEncryptionAlgValuesSupported()) {
						requestObjectEncryptionAlgValuesSupported.put(requestObjectEncryptionAlg);
					}
					if (requestObjectEncryptionAlgValuesSupported.length() > 0) {
						jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED,
								requestObjectEncryptionAlgValuesSupported);
					}

					JSONArray requestObjectEncryptionEncValuesSupported = new JSONArray();
					for (String requestObjectEncryptionEnc : appConfiguration
							.getRequestObjectEncryptionEncValuesSupported()) {
						requestObjectEncryptionEncValuesSupported.put(requestObjectEncryptionEnc);
					}
					if (requestObjectEncryptionEncValuesSupported.length() > 0) {
						jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED,
								requestObjectEncryptionEncValuesSupported);
					}

					JSONArray tokenEndpointAuthMethodsSupported = new JSONArray();
					for (String tokenEndpointAuthMethod : appConfiguration.getTokenEndpointAuthMethodsSupported()) {
						tokenEndpointAuthMethodsSupported.put(tokenEndpointAuthMethod);
					}
					if (tokenEndpointAuthMethodsSupported.length() > 0) {
						jsonObj.put(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED, tokenEndpointAuthMethodsSupported);
					}

					JSONArray tokenEndpointAuthSigningAlgValuesSupported = new JSONArray();
					for (String tokenEndpointAuthSigningAlg : appConfiguration
							.getTokenEndpointAuthSigningAlgValuesSupported()) {
						tokenEndpointAuthSigningAlgValuesSupported.put(tokenEndpointAuthSigningAlg);
					}
					if (tokenEndpointAuthSigningAlgValuesSupported.length() > 0) {
						jsonObj.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED,
								tokenEndpointAuthSigningAlgValuesSupported);
					}

					JSONArray displayValuesSupported = new JSONArray();
					for (String display : appConfiguration.getDisplayValuesSupported()) {
						displayValuesSupported.put(display);
					}
					if (displayValuesSupported.length() > 0) {
						jsonObj.put(DISPLAY_VALUES_SUPPORTED, displayValuesSupported);
					}

					JSONArray claimTypesSupported = new JSONArray();
					for (String claimType : appConfiguration.getClaimTypesSupported()) {
						claimTypesSupported.put(claimType);
					}
					if (claimTypesSupported.length() > 0) {
						jsonObj.put(CLAIM_TYPES_SUPPORTED, claimTypesSupported);
					}

					JSONArray claimsSupported = new JSONArray();
					List<GluuAttribute> gluuAttributes = AttributeService.instance().getAllAttributes();

					// Preload all scopes to avoid sending request to LDAP per
					// claim
					List<org.xdi.oxauth.model.common.Scope> scopes = scopeService.getAllScopesList();

					for (GluuAttribute gluuAttribute : gluuAttributes) {
						if (GluuStatus.ACTIVE.equals(gluuAttribute.getStatus())) {
							String claimName = gluuAttribute.getOxAuthClaimName();
							if (StringUtils.isNotBlank(claimName)) {
								List<org.xdi.oxauth.model.common.Scope> scopesByClaim = scopeService
										.getScopesByClaim(scopes, gluuAttribute.getDn());
								for (org.xdi.oxauth.model.common.Scope scope : scopesByClaim) {
									if (ScopeType.OPENID.equals(scope.getScopeType())) {
										claimsSupported.put(claimName);
										break;
									}
								}
							}
						}
					}

					if (claimsSupported.length() > 0) {
						jsonObj.put(CLAIMS_SUPPORTED, claimsSupported);
					}

					jsonObj.put(SERVICE_DOCUMENTATION, appConfiguration.getServiceDocumentation());

					JSONArray claimsLocalesSupported = new JSONArray();
					for (String claimLocale : appConfiguration.getClaimsLocalesSupported()) {
						claimsLocalesSupported.put(claimLocale);
					}
					if (claimsLocalesSupported.length() > 0) {
						jsonObj.put(CLAIMS_LOCALES_SUPPORTED, claimsLocalesSupported);
					}

					JSONArray uiLocalesSupported = new JSONArray();
					for (String uiLocale : appConfiguration.getUiLocalesSupported()) {
						uiLocalesSupported.put(uiLocale);
					}
					if (uiLocalesSupported.length() > 0) {
						jsonObj.put(UI_LOCALES_SUPPORTED, uiLocalesSupported);
					}

					jsonObj.put(SCOPE_TO_CLAIMS_MAPPING, createScopeToClaimsMapping());

					jsonObj.put(CLAIMS_PARAMETER_SUPPORTED, appConfiguration.getClaimsParameterSupported());
					jsonObj.put(REQUEST_PARAMETER_SUPPORTED, appConfiguration.getRequestParameterSupported());
					jsonObj.put(REQUEST_URI_PARAMETER_SUPPORTED, appConfiguration.getRequestUriParameterSupported());
					jsonObj.put(REQUIRE_REQUEST_URI_REGISTRATION, appConfiguration.getRequireRequestUriRegistration());
					jsonObj.put(OP_POLICY_URI, appConfiguration.getOpPolicyUri());
					jsonObj.put(OP_TOS_URI, appConfiguration.getOpTosUri());
					jsonObj.put(FRONTCHANNEL_LOGOUT_SUPPORTED, "true");
					jsonObj.put(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED, "true");
					jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED, appConfiguration.getFrontChannelLogoutSessionSupported());

					out.println(jsonObj.toString(4).replace("\\/", "/"));
				} catch (JSONException e) {
					LOG.error(e.getMessage(), e);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				} finally {
					out.close();
				}
			}
		}.run();
	}

	/**
	 * @deprecated theses params:
	 *             <ul>
	 *             <li>id_generation_endpoint</li>
	 *             <li>introspection_endpoint</li>
	 *             <li>auth_level_mapping</li>
	 *             <li>scope_to_claims_mapping</li>
	 *             </ul>
	 *             will be moved from /.well-known/openid-configuration
	 *             to /.well-known/gluu-configuration
	 */
	@Deprecated
	private static JSONArray createScopeToClaimsMapping() {
		final JSONArray result = new JSONArray();
		try {
			final AttributeService attributeService = AttributeService.instance();
			final ScopeService scopeService = ScopeService.instance();
			for (Scope scope : scopeService.getAllScopesList()) {
				final JSONArray claimsList = new JSONArray();
				final JSONObject mapping = new JSONObject();
				mapping.put(scope.getDisplayName(), claimsList);

				result.put(mapping);

				final List<String> claimIdList = scope.getOxAuthClaims();
				if (claimIdList != null && !claimIdList.isEmpty()) {
					for (String claimDn : claimIdList) {
						final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
						final String claimName = attribute.getOxAuthClaimName();
						if (StringUtils.isNotBlank(claimName)) {
							claimsList.put(claimName);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * @deprecated theses params:
	 *             <ul>
	 *             <li>id_generation_endpoint</li>
	 *             <li>introspection_endpoint</li>
	 *             <li>auth_level_mapping</li>
	 *             <li>scope_to_claims_mapping</li>
	 *             </ul>
	 *             will be moved from /.well-known/openid-configuration to
	 *             /.well-known/gluu-configuration
	 */
	@Deprecated
	private JSONObject createAuthLevelMapping() {
		final JSONObject mappings = new JSONObject();
		try {
			Map<Integer, Set<String>> map = ExternalAuthenticationService.instance().levelToAcrMapping();
			for (Integer level : map.keySet())
				mappings.put(level.toString(), map.get(level));
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return mappings;
	}

	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 * @throws ServletException
	 *             if a servlet-specific error occurs
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "OpenID Provider Configuration Information";
	}

}