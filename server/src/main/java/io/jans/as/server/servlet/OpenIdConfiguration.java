/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.common.service.AttributeService;
import io.jans.as.model.common.*;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.persistence.model.Scope;
import io.jans.as.persistence.model.ScopeAttributes;
import io.jans.as.server.ciba.CIBAConfigurationService;
import io.jans.as.server.service.LocalResponseCache;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.ExternalDynamicScopeService;
import io.jans.as.server.util.ServerUtil;
import io.jans.model.GluuAttribute;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.*;
import static io.jans.as.model.util.StringUtils.implode;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version September 30, 2021
 */
@WebServlet(urlPatterns = "/.well-known/openid-configuration", loadOnStartup = 10)
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

    @Inject
    private CIBAConfigurationService cibaConfigurationService;

    @Inject
    private LocalResponseCache localResponseCache;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) throws IOException {
        if (!(externalAuthenticationService.isLoaded() && externalDynamicScopeService.isLoaded())) {
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error("Jans Auth still starting up!");
            return;
        }

        httpResponse.setContentType("application/json");
        try (PrintWriter out = httpResponse.getWriter()) {
            final JSONObject cachedResponse = localResponseCache.getDiscoveryResponse();
            if (cachedResponse != null) {
                log.trace("Cached discovery response returned.");
                out.println(ServerUtil.toPrettyJson(cachedResponse).replace("\\/", "/"));
                return;
            }

            JSONObject jsonObj = new JSONObject();

            jsonObj.put(ISSUER, appConfiguration.getIssuer());
            jsonObj.put(AUTHORIZATION_ENDPOINT, appConfiguration.getAuthorizationEndpoint());
            jsonObj.put(TOKEN_ENDPOINT, appConfiguration.getTokenEndpoint());
            jsonObj.put(JWKS_URI, appConfiguration.getJwksUri());
            jsonObj.put(CHECK_SESSION_IFRAME, appConfiguration.getCheckSessionIFrame());

            if (appConfiguration.isEnabledComponent(ComponentType.REVOKE_TOKEN))
                jsonObj.put(REVOCATION_ENDPOINT, appConfiguration.getTokenRevocationEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.REVOKE_SESSION))
                jsonObj.put(SESSION_REVOCATION_ENDPOINT, endpointUrl("/revoke_session"));
            if (appConfiguration.isEnabledComponent(ComponentType.USERINFO))
                jsonObj.put(USER_INFO_ENDPOINT, appConfiguration.getUserInfoEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.CLIENTINFO))
                jsonObj.put(CLIENT_INFO_ENDPOINT, appConfiguration.getClientInfoEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.END_SESSION))
                jsonObj.put(END_SESSION_ENDPOINT, appConfiguration.getEndSessionEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.REGISTRATION))
                jsonObj.put(REGISTRATION_ENDPOINT, appConfiguration.getRegistrationEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.ID_GENERATION))
                jsonObj.put(ID_GENERATION_ENDPOINT, appConfiguration.getIdGenerationEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.INTROSPECTION))
                jsonObj.put(INTROSPECTION_ENDPOINT, appConfiguration.getIntrospectionEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.DEVICE_AUTHZ))
                jsonObj.put(DEVICE_AUTHZ_ENDPOINT, appConfiguration.getDeviceAuthzEndpoint());
            if (appConfiguration.isEnabledComponent(ComponentType.PAR)) {
                jsonObj.put(PAR_ENDPOINT, appConfiguration.getParEndpoint());
                jsonObj.put(REQUIRE_PAR, appConfiguration.getRequirePar());
            }

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

            JSONArray authorizationSigningAlgValuesSupported = new JSONArray();
            for (String authorizationSigningAlg : appConfiguration.getAuthorizationSigningAlgValuesSupported()) {
                authorizationSigningAlgValuesSupported.put(authorizationSigningAlg);
            }
            if (!authorizationSigningAlgValuesSupported.isEmpty()) {
                jsonObj.put(AUTHORIZATION_SIGNING_ALG_VALUES_SUPPORTED, authorizationSigningAlgValuesSupported);
            }

            JSONArray authorizationEncryptionAlgValuesSupported = new JSONArray();
            for (String authorizationEncryptionAlg : appConfiguration.getAuthorizationEncryptionAlgValuesSupported()) {
                authorizationEncryptionAlgValuesSupported.put(authorizationEncryptionAlg);
            }
            if (!authorizationEncryptionAlgValuesSupported.isEmpty()) {
                jsonObj.put(AUTHORIZATION_ENCRYPTION_ALG_VALUES_SUPPORTED, authorizationEncryptionAlgValuesSupported);
            }

            JSONArray authorizationEncryptionEncValuesSupported = new JSONArray();
            for (String authorizationEncyptionEnc : appConfiguration.getAuthorizationEncryptionEncValuesSupported()) {
                authorizationEncryptionEncValuesSupported.put(authorizationEncyptionEnc);
            }
            if (!authorizationEncryptionEncValuesSupported.isEmpty()) {
                jsonObj.put(AUTHORIZATION_ENCRYPTION_ENC_VALUES_SUPPORTED, authorizationEncryptionEncValuesSupported);
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

            JSONArray dpopSigningAlgValuesSupported = new JSONArray();
            for (String dpopSigningAlg : appConfiguration.getDpopSigningAlgValuesSupported()) {
                dpopSigningAlgValuesSupported.put(dpopSigningAlg);
            }
            if (dpopSigningAlgValuesSupported.length() > 0) {
                jsonObj.put(DPOP_SIGNING_ALG_VALUES_SUPPORTED, dpopSigningAlgValuesSupported);
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
            jsonObj.put(FRONT_CHANNEL_LOGOUT_SESSION_SUPPORTED,
                    appConfiguration.getFrontChannelLogoutSessionSupported());

            addMtlsAliases(jsonObj);

            // CIBA Configuration
            cibaConfigurationService.processConfiguration(jsonObj);

            filterOutKeys(jsonObj);
            localResponseCache.putDiscoveryResponse(jsonObj);

            out.println(ServerUtil.toPrettyJson(jsonObj).replace("\\/", "/"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addMtlsAliases(JSONObject jsonObj) {
        JSONObject aliases = new JSONObject();

        if (StringUtils.isNotBlank(appConfiguration.getMtlsAuthorizationEndpoint()))
            aliases.put(AUTHORIZATION_ENDPOINT, appConfiguration.getMtlsAuthorizationEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsTokenEndpoint()))
            aliases.put(TOKEN_ENDPOINT, appConfiguration.getMtlsTokenEndpoint());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsJwksUri()))
            aliases.put(JWKS_URI, appConfiguration.getMtlsJwksUri());
        if (StringUtils.isNotBlank(appConfiguration.getMtlsCheckSessionIFrame()))
            aliases.put(CHECK_SESSION_IFRAME, appConfiguration.getMtlsCheckSessionIFrame());
        if (appConfiguration.isEnabledComponent(ComponentType.REVOKE_TOKEN) && StringUtils.isNotBlank(appConfiguration.getMtlsTokenRevocationEndpoint()))
            aliases.put(REVOCATION_ENDPOINT, appConfiguration.getMtlsTokenRevocationEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.REVOKE_SESSION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(SESSION_REVOCATION_ENDPOINT, StringUtils.replace(appConfiguration.getMtlsEndSessionEndpoint(), "/end_session", "/revoke_session"));
        if (appConfiguration.isEnabledComponent(ComponentType.USERINFO) && StringUtils.isNotBlank(appConfiguration.getMtlsUserInfoEndpoint()))
            aliases.put(USER_INFO_ENDPOINT, appConfiguration.getMtlsUserInfoEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.CLIENTINFO) && StringUtils.isNotBlank(appConfiguration.getMtlsClientInfoEndpoint()))
            aliases.put(CLIENT_INFO_ENDPOINT, appConfiguration.getMtlsClientInfoEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.END_SESSION) && StringUtils.isNotBlank(appConfiguration.getMtlsEndSessionEndpoint()))
            aliases.put(END_SESSION_ENDPOINT, appConfiguration.getMtlsEndSessionEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.REGISTRATION) && StringUtils.isNotBlank(appConfiguration.getMtlsRegistrationEndpoint()))
            aliases.put(REGISTRATION_ENDPOINT, appConfiguration.getMtlsRegistrationEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.ID_GENERATION) && StringUtils.isNotBlank(appConfiguration.getMtlsIdGenerationEndpoint()))
            aliases.put(ID_GENERATION_ENDPOINT, appConfiguration.getMtlsIdGenerationEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.INTROSPECTION) && StringUtils.isNotBlank(appConfiguration.getMtlsIntrospectionEndpoint()))
            aliases.put(INTROSPECTION_ENDPOINT, appConfiguration.getMtlsIntrospectionEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.DEVICE_AUTHZ) && StringUtils.isNotBlank(appConfiguration.getMtlsDeviceAuthzEndpoint()))
            aliases.put(DEVICE_AUTHZ_ENDPOINT, appConfiguration.getMtlsDeviceAuthzEndpoint());
        if (appConfiguration.isEnabledComponent(ComponentType.PAR) && StringUtils.isNotBlank(appConfiguration.getMtlsParEndpoint())) {
            aliases.put(PAR_ENDPOINT, appConfiguration.getMtlsParEndpoint());
        }

        log.trace("MTLS aliases: " + aliases.toString());
        if (!aliases.isEmpty())
            jsonObj.put(MTLS_ENDPOINT_ALIASES, aliases);
    }

    private void filterOutKeys(JSONObject jsonObj) {
        final List<String> allowedKeys = appConfiguration.getDiscoveryAllowedKeys();
        if (allowedKeys == null || allowedKeys.isEmpty()) {
            return; // nothing to filter
        }

        for (String key : new HashSet<>(jsonObj.keySet())) {
            if (allowedKeys.contains(key)) {
                continue;
            }

            jsonObj.remove(key);
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
                            if (StringUtils.isNotBlank(claimName) && !Boolean.TRUE.equals(attribute.getJansHideOnDiscovery())) {
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