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
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.config.Configuration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;
import org.xdi.oxauth.service.external.ExternalAuthenticationService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static org.xdi.oxauth.model.configuration.ConfigurationResponseClaim.*;

/**
 * @author Javier Rojas Blum
 * @version 0.9 March 27, 2015
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
            
            ConfigurationFactory configurationFactory = ConfigurationFactory.instance();
            Configuration configuration = configurationFactory.getConfiguration();

            jsonObj.put(ISSUER, configuration.getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, configuration.getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, configuration.getTokenEndpoint());
            jsonObj.put(USER_INFO_ENDPOINT, configuration.getUserInfoEndpoint());
            jsonObj.put(CLIENT_INFO_ENDPOINT, configuration.getClientInfoEndpoint());
            jsonObj.put(CHECK_SESSION_IFRAME, configuration.getCheckSessionIFrame());
            jsonObj.put(END_SESSION_ENDPOINT, configuration.getEndSessionEndpoint());
            jsonObj.put(END_SESSION_PAGE, configuration.getEndSessionPage());
            jsonObj.put(JWKS_URI, configuration.getJwksUri());
            jsonObj.put(REGISTRATION_ENDPOINT, configuration.getRegistrationEndpoint());
            jsonObj.put(VALIDATE_TOKEN_ENDPOINT, configuration.getValidateTokenEndpoint());
            jsonObj.put(FEDERATION_METADATA_ENDPOINT, configuration.getFederationMetadataEndpoint());
            jsonObj.put(FEDERATION_ENDPOINT, configuration.getFederationEndpoint());
            jsonObj.put(ID_GENERATION_ENDPOINT, configuration.getIdGenerationEndpoint());
            jsonObj.put(INTROSPECTION_ENDPOINT, configuration.getIntrospectionEndpoint());

            ScopeService scopeService = ScopeService.instance();
            JSONArray scopesSupported = new JSONArray();
            for (Scope scope : scopeService.getAllScopesList()) {
                scopesSupported.put(scope.getDisplayName());
            }
            if (scopesSupported.length() > 0) {
                jsonObj.put(SCOPES_SUPPORTED, scopesSupported);
            }

            JSONArray responseTypesSupported = new JSONArray();
            for (String responseType : configuration.getResponseTypesSupported()) {
                responseTypesSupported.put(responseType);
            }
            if (responseTypesSupported.length() > 0) {
                jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
            }

            JSONArray grantTypesSupported = new JSONArray();
            for (String grantType : configuration.getGrantTypesSupported()) {
                grantTypesSupported.put(grantType);
            }
            if (grantTypesSupported.length() > 0) {
                jsonObj.put(GRANT_TYPES_SUPPORTED, grantTypesSupported);
            }

            JSONArray acrValuesSupported = new JSONArray();
            ExternalAuthenticationService externalAuthenticationService = ExternalAuthenticationService.instance();
            for (String acr : externalAuthenticationService.getAcrValuesList()) {
                acrValuesSupported.put(acr);
            }
            jsonObj.put(ACR_VALUES_SUPPORTED, acrValuesSupported);

            JSONArray subjectTypesSupported = new JSONArray();
            for (String subjectType : configuration.getSubjectTypesSupported()) {
                subjectTypesSupported.put(subjectType);
            }
            if (subjectTypesSupported.length() > 0) {
                jsonObj.put(SUBJECT_TYPES_SUPPORTED, subjectTypesSupported);
            }

            JSONArray userInfoSigningAlgValuesSupported = new JSONArray();
            for (String userInfoSigningAlg : configuration.getUserInfoSigningAlgValuesSupported()) {
                userInfoSigningAlgValuesSupported.put(userInfoSigningAlg);
            }
            if (userInfoSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_SIGNING_ALG_VALUES_SUPPORTED, userInfoSigningAlgValuesSupported);
            }

            JSONArray userInfoEncryptionAlgValuesSupported = new JSONArray();
            for (String userInfoEncryptionAlg : configuration.getUserInfoEncryptionAlgValuesSupported()) {
                userInfoEncryptionAlgValuesSupported.put(userInfoEncryptionAlg);
            }
            if (userInfoEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_ENCRYPTION_ALG_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
            }

            JSONArray userInfoEncryptionEncValuesSupported = new JSONArray();
            for (String userInfoEncryptionEnc : configuration.getUserInfoEncryptionEncValuesSupported()) {
                userInfoEncryptionEncValuesSupported.put(userInfoEncryptionEnc);
            }
            if (userInfoEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(USER_INFO_ENCRYPTION_ENC_VALUES_SUPPORTED, userInfoEncryptionAlgValuesSupported);
            }

            JSONArray idTokenSigningAlgValuesSupported = new JSONArray();
            for (String idTokenSigningAlg : configuration.getIdTokenSigningAlgValuesSupported()) {
                idTokenSigningAlgValuesSupported.put(idTokenSigningAlg);
            }
            if (idTokenSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, idTokenSigningAlgValuesSupported);
            }

            JSONArray idTokenEncryptionAlgValuesSupported = new JSONArray();
            for (String idTokenEncryptionAlg : configuration.getIdTokenEncryptionAlgValuesSupported()) {
                idTokenEncryptionAlgValuesSupported.put(idTokenEncryptionAlg);
            }
            if (idTokenEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED, idTokenEncryptionAlgValuesSupported);
            }

            JSONArray idTokenEncryptionEncValuesSupported = new JSONArray();
            for (String idTokenEncryptionEnc : configuration.getIdTokenEncryptionEncValuesSupported()) {
                idTokenEncryptionEncValuesSupported.put(idTokenEncryptionEnc);
            }
            if (idTokenEncryptionEncValuesSupported.length() > 0) {
                jsonObj.put(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED, idTokenEncryptionEncValuesSupported);
            }

            JSONArray requestObjectSigningAlgValuesSupported = new JSONArray();
            for (String requestObjectSigningAlg : configuration.getRequestObjectSigningAlgValuesSupported()) {
                requestObjectSigningAlgValuesSupported.put(requestObjectSigningAlg);
            }
            if (requestObjectSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED, requestObjectSigningAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionAlgValuesSupported = new JSONArray();
            for (String requestObjectEncryptionAlg : configuration.getRequestObjectEncryptionAlgValuesSupported()) {
                requestObjectEncryptionAlgValuesSupported.put(requestObjectEncryptionAlg);
            }
            if (requestObjectEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED, requestObjectEncryptionAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionEncValuesSupported = new JSONArray();
            for (String requestObjectEncryptionEnc : configuration.getRequestObjectEncryptionEncValuesSupported()) {
                requestObjectEncryptionEncValuesSupported.put(requestObjectEncryptionEnc);
            }
            if (requestObjectEncryptionEncValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED, requestObjectEncryptionEncValuesSupported);
            }

            JSONArray tokenEndpointAuthMethodsSupported = new JSONArray();
            for (String tokenEndpointAuthMethod : configuration.getTokenEndpointAuthMethodsSupported()) {
                tokenEndpointAuthMethodsSupported.put(tokenEndpointAuthMethod);
            }
            if (tokenEndpointAuthMethodsSupported.length() > 0) {
                jsonObj.put(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED, tokenEndpointAuthMethodsSupported);
            }

            JSONArray tokenEndpointAuthSigningAlgValuesSupported = new JSONArray();
            for (String tokenEndpointAuthSigningAlg : configuration.getTokenEndpointAuthSigningAlgValuesSupported()) {
                tokenEndpointAuthSigningAlgValuesSupported.put(tokenEndpointAuthSigningAlg);
            }
            if (tokenEndpointAuthSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED, tokenEndpointAuthSigningAlgValuesSupported);
            }

            JSONArray displayValuesSupported = new JSONArray();
            for (String display : configuration.getDisplayValuesSupported()) {
                displayValuesSupported.put(display);
            }
            if (displayValuesSupported.length() > 0) {
                jsonObj.put(DISPLAY_VALUES_SUPPORTED, displayValuesSupported);
            }

            JSONArray claimTypesSupported = new JSONArray();
            for (String claimType : configuration.getClaimTypesSupported()) {
                claimTypesSupported.put(claimType);
            }
            if (claimTypesSupported.length() > 0) {
                jsonObj.put(CLAIM_TYPES_SUPPORTED, claimTypesSupported);
            }

            JSONArray claimsSupported = new JSONArray();
            List<GluuAttribute> gluuAttributes = AttributeService.instance().getAllAttributes();
            for (GluuAttribute gluuAttribute : gluuAttributes) {
                String claimName = gluuAttribute.getOxAuthClaimName();
                if (StringUtils.isNotBlank(claimName)) {
                    claimsSupported.put(claimName);
                }
            }
            if (claimsSupported.length() > 0) {
                jsonObj.put(CLAIMS_SUPPORTED, claimsSupported);
            }

            jsonObj.put(SERVICE_DOCUMENTATION, configuration.getServiceDocumentation());

            JSONArray claimsLocalesSupported = new JSONArray();
            for (String claimLocale : configuration.getClaimsLocalesSupported()) {
                claimsLocalesSupported.put(claimLocale);
            }
            if (claimsLocalesSupported.length() > 0) {
                jsonObj.put(CLAIMS_LOCALES_SUPPORTED, claimsLocalesSupported);
            }

            JSONArray uiLocalesSupported = new JSONArray();
            for (String uiLocale : configuration.getUiLocalesSupported()) {
                uiLocalesSupported.put(uiLocale);
            }
            if (uiLocalesSupported.length() > 0) {
                jsonObj.put(UI_LOCALES_SUPPORTED, uiLocalesSupported);
            }

            jsonObj.put(SCOPE_TO_CLAIMS_MAPPING, createScopeToClaimsMapping());

            jsonObj.put(CLAIMS_PARAMETER_SUPPORTED, configuration.getClaimsParameterSupported());
            jsonObj.put(REQUEST_PARAMETER_SUPPORTED, configuration.getRequestParameterSupported());
            jsonObj.put(REQUEST_URI_PARAMETER_SUPPORTED, configuration.getRequestUriParameterSupported());
            jsonObj.put(REQUIRE_REQUEST_URI_REGISTRATION, configuration.getRequireRequestUriRegistration());
            jsonObj.put(OP_POLICY_URI, configuration.getOpPolicyUri());
            jsonObj.put(OP_TOS_URI, configuration.getOpTosUri());
            jsonObj.put(HTTP_LOGOUT_SUPPORTED, "true");
            jsonObj.put(LOGOUT_SESSION_SUPPORTED, "true");

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