/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.servlet;

import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ACR_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.AMR_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.AUTHORIZATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CHECK_SESSION_IFRAME;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_KEY;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_LOCALES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_PARAMETER_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIMS_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLAIM_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.CLIENT_INFO_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.DISPLAY_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.END_SESSION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.FEDERATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.FEDERATION_METADATA_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.GRANT_TYPES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_GENERATION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.INTROSPECTION_ENDPOINT;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.ISSUER;
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.JWKS_URI;
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
import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.SCOPE_KEY;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;

/**
 * @author Javier Rojas Blum Date: 12.7.2011
 */
public class OpenIdConfiguration extends HttpServlet {

    private final static Log LOG = Logging.getLog(OpenIdConfiguration.class);

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        try {
            JSONObject jsonObj = new JSONObject();

            jsonObj.put(ISSUER, ConfigurationFactory.getConfiguration().getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, ConfigurationFactory.getConfiguration().getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, ConfigurationFactory.getConfiguration().getTokenEndpoint());
            jsonObj.put(USER_INFO_ENDPOINT, ConfigurationFactory.getConfiguration().getUserInfoEndpoint());
            jsonObj.put(CLIENT_INFO_ENDPOINT, ConfigurationFactory.getConfiguration().getClientInfoEndpoint());
            jsonObj.put(CHECK_SESSION_IFRAME, ConfigurationFactory.getConfiguration().getCheckSessionIFrame());
            jsonObj.put(END_SESSION_ENDPOINT, ConfigurationFactory.getConfiguration().getEndSessionEndpoint());
            jsonObj.put(JWKS_URI, ConfigurationFactory.getConfiguration().getJwksUri());
            jsonObj.put(REGISTRATION_ENDPOINT, ConfigurationFactory.getConfiguration().getRegistrationEndpoint());
            jsonObj.put(VALIDATE_TOKEN_ENDPOINT, ConfigurationFactory.getConfiguration().getValidateTokenEndpoint());
            jsonObj.put(FEDERATION_METADATA_ENDPOINT, ConfigurationFactory.getConfiguration().getFederationMetadataEndpoint());
            jsonObj.put(FEDERATION_ENDPOINT, ConfigurationFactory.getConfiguration().getFederationEndpoint());
            jsonObj.put(ID_GENERATION_ENDPOINT, ConfigurationFactory.getConfiguration().getIdGenerationEndpoint());
            jsonObj.put(INTROSPECTION_ENDPOINT, ConfigurationFactory.getConfiguration().getIntrospectionEndpoint());

            ScopeService scopeService = ScopeService.instance();
            JSONArray scopesSupported = new JSONArray();
            for (Scope scope : scopeService.getAllScopesList()) {
                scopesSupported.put(scope.getDisplayName());
            }
            if (scopesSupported.length() > 0) {
                jsonObj.put(SCOPES_SUPPORTED, scopesSupported);
            }

            JSONArray responseTypesSupported = new JSONArray();
            for (String responseType : ConfigurationFactory.getConfiguration().getResponseTypesSupported()) {
                responseTypesSupported.put(responseType);
            }
            if (responseTypesSupported.length() > 0) {
                jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
            }

            JSONArray grantTypesSupported = new JSONArray();
            for (String grantType : ConfigurationFactory.getConfiguration().getGrantTypesSupported()) {
                grantTypesSupported.put(grantType);
            }
            if (grantTypesSupported.length() > 0) {
                jsonObj.put(GRANT_TYPES_SUPPORTED, grantTypesSupported);
            }

            JSONArray acrValuesSupported = new JSONArray();
            ExternalAuthenticationService externalAuthenticationService = ExternalAuthenticationService.instance();
            for (String acr : externalAuthenticationService.getAcrValuesList() /*ConfigurationFactory.getConfiguration().getAcrValuesSupported() */) {
                acrValuesSupported.put(acr);
            }
            if (acrValuesSupported.length() > 0) {
                jsonObj.put(ACR_VALUES_SUPPORTED, acrValuesSupported);
            }

            JSONArray amrValuesSupported = new JSONArray();
            for (String amr : ConfigurationFactory.getConfiguration().getAmrValuesSupported()) {
                amrValuesSupported.put(amr);
            }
            if (amrValuesSupported.length() > 0) {
                jsonObj.put(AMR_VALUES_SUPPORTED, amrValuesSupported);
            }

            JSONArray subjectTypesSupported = new JSONArray();
            for (String subjectType : ConfigurationFactory.getConfiguration().getSubjectTypesSupported()) {
                subjectTypesSupported.put(subjectType);
            }
            if (subjectTypesSupported.length() > 0) {
                jsonObj.put(SUBJECT_TYPES_SUPPORTED, subjectTypesSupported);
            }

            JSONArray userInfoSigningAlgValuesSupported = new JSONArray();
            for (String userInfoSigningAlg : ConfigurationFactory.getConfiguration().getUserInfoSigningAlgValuesSupported()) {
                userInfoSigningAlgValuesSupported.put(userInfoSigningAlg);
            }
            if (userInfoSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, userInfoSigningAlgValuesSupported);
            }

            JSONArray userInfoEncryptionAlgValuesSupported = new JSONArray();
            for (String userInfoEncryptionAlg : ConfigurationFactory.getConfiguration().getUserInfoEncryptionAlgValuesSupported()) {
                userInfoEncryptionAlgValuesSupported.put(userInfoEncryptionAlg);
            }
            if (userInfoEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
            }

            JSONArray userInfoEncryptionEncValuesSupported = new JSONArray();
            for (String userInfoEncryptionEnc : ConfigurationFactory.getConfiguration().getUserInfoEncryptionEncValuesSupported()) {
                userInfoEncryptionEncValuesSupported.put(userInfoEncryptionEnc);
            }
            if (userInfoEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
            }

            JSONArray idTokenSigningAlgValuesSupported = new JSONArray();
            for (String idTokenSigningAlg : ConfigurationFactory.getConfiguration().getIdTokenSigningAlgValuesSupported()) {
                idTokenSigningAlgValuesSupported.put(idTokenSigningAlg);
            }
            if (idTokenSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, idTokenSigningAlgValuesSupported);
            }

            JSONArray idTokenEncryptionAlgValuesSupported = new JSONArray();
            for (String idTokenEncryptionAlg : ConfigurationFactory.getConfiguration().getIdTokenEncryptionAlgValuesSupported()) {
                idTokenEncryptionAlgValuesSupported.put(idTokenEncryptionAlg);
            }
            if (idTokenEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED, idTokenEncryptionAlgValuesSupported);
            }

            JSONArray idTokenEncryptionEncValuesSupported = new JSONArray();
            for (String idTokenEncryptionEnc : ConfigurationFactory.getConfiguration().getIdTokenEncryptionEncValuesSupported()) {
                idTokenEncryptionEncValuesSupported.put(idTokenEncryptionEnc);
            }
            if (idTokenEncryptionEncValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED, idTokenEncryptionEncValuesSupported);
            }

            JSONArray requestObjectSigningAlgValuesSupported = new JSONArray();
            for (String requestObjectSigningAlg : ConfigurationFactory.getConfiguration().getRequestObjectSigningAlgValuesSupported()) {
                requestObjectSigningAlgValuesSupported.put(requestObjectSigningAlg);
            }
            if (requestObjectSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED, requestObjectSigningAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionAlgValuesSupported = new JSONArray();
            for (String requestObjectEncryptionAlg : ConfigurationFactory.getConfiguration().getRequestObjectEncryptionAlgValuesSupported()) {
                requestObjectEncryptionAlgValuesSupported.put(requestObjectEncryptionAlg);
            }
            if (requestObjectEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED, requestObjectEncryptionAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionEncValuesSupported = new JSONArray();
            for (String requestObjectEncryptionEnc : ConfigurationFactory.getConfiguration().getRequestObjectEncryptionEncValuesSupported()) {
                requestObjectEncryptionEncValuesSupported.put(requestObjectEncryptionEnc);
            }
            if (requestObjectEncryptionEncValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED, requestObjectEncryptionEncValuesSupported);
            }

            JSONArray tokenEndpointAuthMethodsSupported = new JSONArray();
            for (String tokenEndpointAuthMethod : ConfigurationFactory.getConfiguration().getTokenEndpointAuthMethodsSupported()) {
                tokenEndpointAuthMethodsSupported.put(tokenEndpointAuthMethod);
            }
            if (tokenEndpointAuthMethodsSupported.length() > 0) {
                jsonObj.put(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED, tokenEndpointAuthMethodsSupported);
            }

            JSONArray tokenEndpointAuthSigningAlgValuesSupported = new JSONArray();
            for (String tokenEndpointAuthSigningAlg : ConfigurationFactory.getConfiguration().getTokenEndpointAuthSigningAlgValuesSupported()) {
                tokenEndpointAuthSigningAlgValuesSupported.put(tokenEndpointAuthSigningAlg);
            }
            if (tokenEndpointAuthSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED, tokenEndpointAuthSigningAlgValuesSupported);
            }

            JSONArray displayValuesSupported = new JSONArray();
            for (String display : ConfigurationFactory.getConfiguration().getDisplayValuesSupported()) {
                displayValuesSupported.put(display);
            }
            if (displayValuesSupported.length() > 0) {
                jsonObj.put(DISPLAY_VALUES_SUPPORTED, displayValuesSupported);
            }

            JSONArray claimTypesSupported = new JSONArray();
            for (String claimType : ConfigurationFactory.getConfiguration().getClaimTypesSupported()) {
                claimTypesSupported.put(claimType);
            }
            if (claimTypesSupported.length() > 0) {
                jsonObj.put(CLAIM_TYPES_SUPPORTED, claimTypesSupported);
            }

            JSONArray claimsSupported = new JSONArray();
            for (String claim : ConfigurationFactory.getConfiguration().getClaimsSupported()) {
                claimsSupported.put(claim);
            }
            if (claimsSupported.length() > 0) {
                jsonObj.put(CLAIMS_SUPPORTED, claimsSupported);
            }

            jsonObj.put(SERVICE_DOCUMENTATION, ConfigurationFactory.getConfiguration().getServiceDocumentation());

            JSONArray claimsLocalesSupported = new JSONArray();
            for (String claimLocale : ConfigurationFactory.getConfiguration().getClaimsLocalesSupported()) {
                claimsLocalesSupported.put(claimLocale);
            }
            if (claimsLocalesSupported.length() > 0) {
                jsonObj.put(CLAIMS_LOCALES_SUPPORTED, claimsLocalesSupported);
            }

            JSONArray uiLocalesSupported = new JSONArray();
            for (String uiLocale : ConfigurationFactory.getConfiguration().getUiLocalesSupported()) {
                uiLocalesSupported.put(uiLocale);
            }
            if (uiLocalesSupported.length() > 0) {
                jsonObj.put(UI_LOCALES_SUPPORTED, uiLocalesSupported);
            }

            jsonObj.put(SCOPE_TO_CLAIMS_MAPPING, createScopeToClaimsMapping());

            jsonObj.put(CLAIMS_PARAMETER_SUPPORTED, ConfigurationFactory.getConfiguration().getClaimsParameterSupported());
            jsonObj.put(REQUEST_PARAMETER_SUPPORTED, ConfigurationFactory.getConfiguration().getRequestParameterSupported());
            jsonObj.put(REQUEST_URI_PARAMETER_SUPPORTED, ConfigurationFactory.getConfiguration().getRequestUriParameterSupported());
            jsonObj.put(REQUIRE_REQUEST_URI_REGISTRATION, ConfigurationFactory.getConfiguration().getRequireRequestUriRegistration());
            jsonObj.put(OP_POLICY_URI, ConfigurationFactory.getConfiguration().getOpPolicyUri());
            jsonObj.put(OP_TOS_URI, ConfigurationFactory.getConfiguration().getOpTosUri());

            out.println(jsonObj.toString(4).replace("\\/", "/"));
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    private static JSONArray createScopeToClaimsMapping() {
        final JSONArray result = new JSONArray();
        try {
            final AttributeService attributeService = AttributeService.instance();
            final ScopeService scopeService = ScopeService.instance();
            for (Scope scope : scopeService.getAllScopesList()) {
                final JSONArray claimsList = new JSONArray();
                final JSONObject mapping = new JSONObject();
                mapping.put(SCOPE_KEY, scope.getDisplayName());
                mapping.put(CLAIMS_KEY, claimsList);

                result.put(mapping);

                final List<String> claimIdList = scope.getOxAuthClaims();
                if (claimIdList != null && !claimIdList.isEmpty()) {
                    for (String claimDn : claimIdList) {
                        final GluuAttribute attribute = attributeService.getScopeByDn(claimDn);
                        final String claimName = attribute.getName();
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
     * Handles the HTTP
     * <code>GET</code> method.
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
     * Handles the HTTP
     * <code>POST</code> method.
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