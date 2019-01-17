/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.servlet;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.common.ScopeType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;
import org.xdi.oxauth.service.external.ExternalDynamicScopeService;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.*;
import static org.xdi.oxauth.model.util.StringUtils.implode;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 2016/04/26
 * @version January 16, 2019
 */
@WebServlet(urlPatterns = "/.well-known/openid-configuration")
public class OpenIdConfiguration extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678903L;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttributeService attributeService;

    @Inject
    private ScopeService scopeService;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ExternalDynamicScopeService externalDynamicScopeService;


    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest  servlet request
     * @param servletResponse servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        httpResponse.setContentType("application/json");
        PrintWriter out = httpResponse.getWriter();
        try {
            JSONObject jsonObj = new JSONObject();

            jsonObj.put(ISSUER, appConfiguration.getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
            jsonObj.put(TOKEN_REVOCATION_ENDPOINT, appConfiguration.getTokenRevocationEndpoint());
            jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
            jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());
            jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());
            jsonObj.put(END_SESSION_ENDPOINT, appConfiguration.getEndSessionEndpoint());
            jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
            jsonObj.put(REGISTRATION_ENDPOINT, appConfiguration.getRegistrationEndpoint());
            jsonObj.put(ID_GENERATION_ENDPOINT, appConfiguration.getIdGenerationEndpoint());
            jsonObj.put(INTROSPECTION_ENDPOINT, appConfiguration.getIntrospectionEndpoint());

            JSONArray responseTypesSupported = new JSONArray();
            for (Set<ResponseType> responseTypes : appConfiguration.getResponseTypesSupported()) {
                responseTypesSupported.put(implode(responseTypes, " "));
            }
            if (responseTypesSupported.length() > 0) {
                jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
            }

            JSONArray grantTypesSupported = new JSONArray();
            for (GrantType grantType : appConfiguration.getGrantTypesSupported()) {
                grantTypesSupported.put(grantType);
            }
            if (grantTypesSupported.length() > 0) {
                jsonObj.put(GRANT_TYPES_SUPPORTED, grantTypesSupported);
            }

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

            jsonObj.put(SERVICE_DOCUMENTATION, appConfiguration.getServiceDocumentation());

            JSONArray idTokenTokenBindingCnfValuesSupported = new JSONArray();
            for (String value : appConfiguration.getIdTokenTokenBindingCnfValuesSupported()) {
                idTokenTokenBindingCnfValuesSupported.put(value);
            }
            jsonObj.put(ID_TOKEN_TOKEN_BINDING_CNF_VALUES_SUPPORTED, idTokenTokenBindingCnfValuesSupported);

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

            JSONArray scopesSupported = new JSONArray();
            JSONArray claimsSupported = new JSONArray();
            JSONArray scopeToClaimsMapping = createScopeToClaimsMapping(scopesSupported, claimsSupported);
            if (scopesSupported.length() > 0) {
                jsonObj.put(SCOPES_SUPPORTED, scopesSupported);
            }
            if (claimsSupported.length() > 0) {
                jsonObj.put(CLAIMS_SUPPORTED, claimsSupported);
            }
            jsonObj.put(SCOPE_TO_CLAIMS_MAPPING, scopeToClaimsMapping);

            jsonObj.put(CLAIMS_PARAMETER_SUPPORTED, appConfiguration.getClaimsParameterSupported());
            jsonObj.put(REQUEST_PARAMETER_SUPPORTED, appConfiguration.getRequestParameterSupported());
            jsonObj.put(REQUEST_URI_PARAMETER_SUPPORTED, appConfiguration.getRequestUriParameterSupported());
            jsonObj.put(REQUIRE_REQUEST_URI_REGISTRATION, appConfiguration.getRequireRequestUriRegistration());
            jsonObj.put(OP_POLICY_URI, appConfiguration.getOpPolicyUri());
            jsonObj.put(OP_TOS_URI, appConfiguration.getOpTosUri());
            jsonObj.put(FRONTCHANNEL_LOGOUT_SUPPORTED, new Boolean(true));
            jsonObj.put(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED, new Boolean(true));
            jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED, appConfiguration.getFrontChannelLogoutSessionSupported());

            out.println(jsonObj.toString(4).replace("\\/", "/"));
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    /**
     * @deprecated theses params:
     * <ul>
     * <li>id_generation_endpoint</li>
     * <li>introspection_endpoint</li>
     * <li>auth_level_mapping</li>
     * <li>scope_to_claims_mapping</li>
     * </ul>
     * will be moved from /.well-known/openid-configuration
     * to /.well-known/gluu-configuration
     */
    @Deprecated
    private JSONArray createScopeToClaimsMapping(JSONArray scopesSupported, JSONArray claimsSupported) {
        final JSONArray scopeToClaimMapping = new JSONArray();
        Set<String> scopes = new HashSet<String>();
        Set<String> claims = new HashSet<String>();

        try {
            for (Scope scope : scopeService.getAllScopesList()) {
                final JSONArray claimsList = new JSONArray();
                final JSONObject mapping = new JSONObject();
                mapping.put(scope.getDisplayName(), claimsList);
                scopes.add(scope.getDisplayName());

                scopeToClaimMapping.put(mapping);

                if (ScopeType.DYNAMIC.equals(scope.getScopeType())) {
                    List<String> claimNames = externalDynamicScopeService.executeExternalGetSupportedClaimsMethods(Arrays.asList(scope));
                    for (String claimName : claimNames) {
                        if (StringUtils.isNotBlank(claimName)) {
                            claimsList.put(claimName);
                            claims.add(claimName);
                        }
                    }
                } else {
                    final List<String> claimIdList = scope.getOxAuthClaims();
                    if (claimIdList != null && !claimIdList.isEmpty()) {
                        for (String claimDn : claimIdList) {
                            final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
                            final String claimName = attribute.getOxAuthClaimName();
                            if (StringUtils.isNotBlank(claimName)) {
                                claimsList.put(claimName);
                                claims.add(claimName);
                            }
                        }
                    }
                }
            }

            for (String scope : scopes) {
                scopesSupported.put(scope);
            }
            for (String claim : claims) {
                claimsSupported.put(claim);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return scopeToClaimMapping;
    }

    /**
     * @deprecated theses params:
     * <ul>
     * <li>id_generation_endpoint</li>
     * <li>introspection_endpoint</li>
     * <li>auth_level_mapping</li>
     * <li>scope_to_claims_mapping</li>
     * </ul>
     * will be moved from /.well-known/openid-configuration to
     * /.well-known/gluu-configuration
     */
    @Deprecated
    private JSONObject createAuthLevelMapping() {
        final JSONObject mappings = new JSONObject();
        try {
            Map<Integer, Set<String>> map = externalAuthenticationService.levelToAcrMapping();
            for (Integer level : map.keySet())
                mappings.put(level.toString(), map.get(level));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return mappings;
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
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