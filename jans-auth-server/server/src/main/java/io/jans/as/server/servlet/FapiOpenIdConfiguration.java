/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.as.server.servlet;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseMode;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.util.CertUtils;
import io.jans.as.persistence.model.Scope;
import io.jans.as.persistence.model.ScopeAttributes;
import io.jans.as.server.ciba.CIBAConfigurationService;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;
import static io.jans.as.model.util.StringUtils.implode;
import static io.jans.as.model.util.Util.putArray;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 2016/04/26
 * @version August 14, 2019
 */
@WebServlet(urlPatterns = "/fapi-rs/.well-known/openid-configuration", loadOnStartup = 9)
public class FapiOpenIdConfiguration extends HttpServlet {

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

    @Inject
    private CIBAConfigurationService cibaConfigurationService;

    @Inject
    private TokenService tokenService;

    @Inject
    private ClientService clientService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Override
    public void init() throws ServletException {
        log.info("Inside init method of FapiOpenIdConfiguration***********************************************************************");
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     */
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
        //addedforfapi
        String authFromReq = null;
        String xfapiinteractionid = null;
        String tempaccess_token = null;

        httpResponse.setContentType("application/json");


        try (PrintWriter out = httpResponse.getWriter()) {

            xfapiinteractionid = servletRequest.getHeader("x-fapi-interaction-id");
            tempaccess_token = servletRequest.getParameter("access_token");
            if ((tempaccess_token != null) && (xfapiinteractionid != null)) {
                if (tempaccess_token.startsWith("Bearer")) {
                    log.info("FAPI: Authorization Bearer Token from qeury ********************************************* {}", tempaccess_token);
                    log.info("FAPI: Bearler Token is not allowed.**********************************************************************.");
                    httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "Bearer token in query is disallowed");
                } else
                    httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "token in query is disallowed");
                log.info("FAPI: Authorization token is non-Bearer is not allowed in query*********************************************");
            }

            String clientCertAsPem = servletRequest.getHeader("X-ClientCert");
            if (clientCertAsPem != null) {
                log.info("FAPI: clientCertAsPem found*****************************************");
                log.info("FAPI: clientCertAsPem found*****************************************" + clientCertAsPem);
            } else
                log.info("FAPI: No clientCertAsPem *****************************************");

            authFromReq = servletRequest.getHeader("Authorization");

            String clientDn = null;
            Client cl = null;
            clientDn = tokenService.getClientDn(authFromReq);
            String bearerToken = tokenService.getBearerToken(authFromReq);
            X509Certificate cert = CertUtils.x509CertificateFromPem(clientCertAsPem);

            AuthorizationGrant authorizationGrant = tokenService.getBearerAuthorizationGrant(authFromReq);
            if (authorizationGrant == null) {
                log.error("FAPI: Authorization grant is null.*********************************************");
                httpResponse.sendError(httpResponse.SC_UNAUTHORIZED, "Authorization grant is null.");
            }

            if (cert == null) {
                log.debug("Failed to parse client certificate, client_dn: {}.", clientDn);
                return;
            }
            PublicKey publicKey = cert.getPublicKey();
            byte[] encodedKey = publicKey.getEncoded();

            if (clientDn != null) {
                log.info("FAPI: ClientDn from Authoirization(tokenService) *********************************************" + clientDn);
                cl = clientService.getClientByDn(clientDn);
                String tempjwks = cl.getJwks();
                if (tempjwks == null)
                    log.debug("********************FAPIRS JWKS not defined for the client");
                else {
                    JSONObject jsonWebKeys = new JSONObject(tempjwks);
                    int matchctr = 0;
                    final JSONWebKeySet keySet = JSONWebKeySet.fromJSONObject(jsonWebKeys);
                    try {
                        for (JSONWebKey key : keySet.getKeys()) {
                            if (ArrayUtils.isEquals(encodedKey, cryptoProvider.getPublicKey(key.getKid(), jsonWebKeys, null).getEncoded())) {
                                matchctr += 1;
                                log.debug("********************************Client {} authenticated via `self_signed_tls_client_auth`, matched kid: {}.",
                                        cl.getClientId(), key.getKid());
                            }
                        }

                        if (matchctr == 0) {
                            log.error("Client certificate does not match clientId. clientId: " + cl.getClientId() + "*********************************************");
                            httpResponse.setStatus(401, "The resource owner or authorization server denied the request");
                            return;
                        }
                    } catch (Exception e) {
                        log.info("Exception while keymatching****************************************************************");
                    }
                }
            } else
                log.info("FAPI: ClientDn from Authoirization(tokenService) is NULL*********************************************");

            JSONObject jsonObj = new JSONObject();            // original

            if (xfapiinteractionid != null) {
                httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
                log.info("x-fapi-interaction-id*************************=" + xfapiinteractionid);
            } else {
                xfapiinteractionid = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a";
                httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
                log.info("x-fapi-interaction-id***********************=" + xfapiinteractionid);
            }

            jsonObj.put(ISSUER, appConfiguration.getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
            jsonObj.put(REVOCATION_ENDPOINT, appConfiguration.getTokenRevocationEndpoint());
            jsonObj.put(SESSION_REVOCATION_ENDPOINT, endpointUrl("/revoke_session"));
            jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
            jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());
            jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());
            jsonObj.put(END_SESSION_ENDPOINT, appConfiguration.getEndSessionEndpoint());
            jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
            jsonObj.put(REGISTRATION_ENDPOINT, appConfiguration.getRegistrationEndpoint());
            jsonObj.put(ID_GENERATION_ENDPOINT, appConfiguration.getIdGenerationEndpoint());
            jsonObj.put(INTROSPECTION_ENDPOINT, appConfiguration.getIntrospectionEndpoint());
            jsonObj.put(PAR_ENDPOINT, appConfiguration.getParEndpoint());
            jsonObj.put(REQUIRE_PAR, appConfiguration.getRequirePar());

            JSONArray responseTypesSupported = new JSONArray();
            for (Set<ResponseType> responseTypes : appConfiguration.getResponseTypesSupported()) {
                responseTypesSupported.put(implode(responseTypes, " "));
            }
            if (responseTypesSupported.length() > 0) {
                jsonObj.put(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
            }

            JSONArray responseModesSupported = new JSONArray();
            if (appConfiguration.getResponseModesSupported() != null) {
                for (ResponseMode responseMode : appConfiguration.getResponseModesSupported()) {
                    responseModesSupported.put(responseMode);
                }
            }
            if (responseModesSupported.length() > 0) {
                jsonObj.put(RESPONSE_MODES_SUPPORTED, responseModesSupported);
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

            putArray(jsonObj, appConfiguration.getAccessTokenSigningAlgValuesSupported(), ACCESS_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);

            JSONArray requestObjectSigningAlgValuesSupported = new JSONArray();
            for (String requestObjectSigningAlg : appConfiguration.getRequestObjectSigningAlgValuesSupported()) {
                requestObjectSigningAlgValuesSupported.put(requestObjectSigningAlg);
            }
            if (requestObjectSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED, requestObjectSigningAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionAlgValuesSupported = new JSONArray();
            for (String requestObjectEncryptionAlg : appConfiguration.getRequestObjectEncryptionAlgValuesSupported()) {
                requestObjectEncryptionAlgValuesSupported.put(requestObjectEncryptionAlg);
            }
            if (requestObjectEncryptionAlgValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED, requestObjectEncryptionAlgValuesSupported);
            }

            JSONArray requestObjectEncryptionEncValuesSupported = new JSONArray();
            for (String requestObjectEncryptionEnc : appConfiguration.getRequestObjectEncryptionEncValuesSupported()) {
                requestObjectEncryptionEncValuesSupported.put(requestObjectEncryptionEnc);
            }
            if (requestObjectEncryptionEncValuesSupported.length() > 0) {
                jsonObj.put(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED, requestObjectEncryptionEncValuesSupported);
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
            jsonObj.put(TLS_CLIENT_CERTIFICATE_BOUND_ACCESS_TOKENS, Boolean.TRUE);
            jsonObj.put(BACKCHANNEL_LOGOUT_SUPPORTED, Boolean.TRUE);
            jsonObj.put(BACKCHANNEL_LOGOUT_SESSION_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONTCHANNEL_LOGOUT_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONTCHANNEL_LOGOUT_SESSION_SUPPORTED, Boolean.TRUE);
            jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED, appConfiguration.getFrontChannelLogoutSessionSupported());

            cibaConfigurationService.processConfiguration(jsonObj);

            out.println(ServerUtil.toPrettyJson(jsonObj).replace("\\/", "/"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String endpointUrl(String path) {
        return StringUtils.replace(appConfiguration.getEndSessionEndpoint(), "/end_session", path);
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
    private JSONArray createScopeToClaimsMapping(JSONArray scopesSupported, JSONArray claimsSupported) {
        final JSONArray scopeToClaimMapping = new JSONArray();
        Set<String> scopes = new HashSet<String>();
        Set<String> claims = new HashSet<String>();

        try {
            for (Scope scope : scopeService.getAllScopesList()) {
                if ((scope.getScopeType() == ScopeType.SPONTANEOUS && scope.isDeletable())
                        || !(canShowInConfigEndpoint(scope.getAttributes()))) {
                    continue;
                }

                final JSONArray claimsList = new JSONArray();
                final JSONObject mapping = new JSONObject();
                mapping.put(scope.getId(), claimsList);
                scopes.add(scope.getId());

                scopeToClaimMapping.put(mapping);

                if (ScopeType.DYNAMIC.equals(scope.getScopeType())) {
                    List<String> claimNames = externalDynamicScopeService
                            .executeExternalGetSupportedClaimsMethods(Arrays.asList(scope));
                    for (String claimName : claimNames) {
                        if (StringUtils.isNotBlank(claimName)) {
                            claimsList.put(claimName);
                            claims.add(claimName);
                        }
                    }
                } else {
                    final List<String> claimIdList = scope.getClaims();
                    if (claimIdList != null && !claimIdList.isEmpty()) {
                        for (String claimDn : claimIdList) {
                            final GluuAttribute attribute = attributeService.getAttributeByDn(claimDn);
                            final String claimName = attribute.getClaimName();
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

    private boolean canShowInConfigEndpoint(ScopeAttributes scopeAttributes) {
        return scopeAttributes.isShowInConfigurationEndpoint();
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
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
